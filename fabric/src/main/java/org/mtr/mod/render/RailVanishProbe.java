package org.mtr.mod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.render.CapturedRenderMatrices;
import org.mtr.mod.client.MinecraftClientData;

public final class RailVanishProbe {
   private static final Logger LOGGER = LogManager.getLogger("MTR-RailVanishProbe");
   private static final float YAW_STEP = 15.0F;
   private static final float PITCH_STEP = 15.0F;
   private static final float YAW_MIN = -180.0F;
   private static final float YAW_MAX = 165.0F;
   private static final float PITCH_MIN = -45.0F;
   private static final float PITCH_MAX = 45.0F;
   private static final int HOLD_FRAMES = 4;
   private static final int MAX_RAILS = 6;
   private static final int VISUAL_THRESHOLD = 80;
   private static boolean running;
   private static boolean finished;
   private static boolean waitingForRails;
   private static float yaw;
   private static float pitch;
   private static int framesAtAngle;
   private static int waitFrames;
   private static Path csvPath;
   private static int sampleCount;
   private static int anomalyCount;
   private static int visualVanishCount;
   private static float savedYaw;
   private static float savedPitch;

   private RailVanishProbe() {
   }

   public static void tickApplyCamera() {
      MinecraftClient minecraftclient = MinecraftClient.getInstance();
      Path path = minecraftclient.getRunDirectoryMapped().toPath();
      Path path1 = path.resolve("mtr-rail-vanish-probe.start");
      Path path2 = path.resolve("mtr-rail-vanish-probe.stop");
      if (Files.exists(path2)) {
         try {
            Files.deleteIfExists(path2);
         } catch (IOException ioexception) {
         }

         if (running) {
            finish(minecraftclient, "stopped");
         }
      } else {
         if (!running && Files.exists(path1)) {
            finished = false;
            start(minecraftclient, path, path1);
         }

         if (running) {
            if (minecraftclient.getCurrentScreenMapped() != null) {
               minecraftclient.openScreen(null);
            }

            ClientPlayerEntity clientplayerentity = minecraftclient.getPlayerMapped();
            if (clientplayerentity != null) {
               clientplayerentity.setYRot(yaw);
               clientplayerentity.setXRot(pitch);
               clientplayerentity.setYRotOMapped(yaw);
               clientplayerentity.setXRotOMapped(pitch);
            }
         }
      }
   }

   public static void onWorldRendered() {
      if (running) {
         MinecraftClient minecraftclient = MinecraftClient.getInstance();
         ClientPlayerEntity clientplayerentity = minecraftclient.getPlayerMapped();
         if (clientplayerentity != null) {
            if (waitingForRails) {
               int i = MinecraftClientData.getInstance().railWrapperList.size();
               waitFrames++;
               if (i <= 0) {
                  if (waitFrames % 40 == 0) {
                     LOGGER.info("[MTR-RailVanishProbe] waiting for rails… ({} frames)", waitFrames);
                  }

                  if (waitFrames > 1200) {
                     finish(minecraftclient, "timeout-no-rails");
                  }
               } else {
                  waitingForRails = false;
                  framesAtAngle = 0;
                  // Confirm rails exist / warm nearby look, then always run the full yaw×pitch grid.
                  // aimAtNearestRail alone can land near yaw=±180 and finish after 1–2 pitch steps.
                  aimAtNearestRail(minecraftclient, clientplayerentity);
                  yaw = YAW_MIN;
                  pitch = PITCH_MIN;
                  LOGGER.info("[MTR-RailVanishProbe] rails ready count={} — starting full sweep yaw=[{}..{}] pitch=[{}..{}]", i, YAW_MIN, YAW_MAX, PITCH_MIN, PITCH_MAX);
                  clientplayerentity.sendMessage(new Text((Component)TextHelper.literal("[MTR] Probe: " + i + " rails, full sweep").data), false);
               }
            } else {
               framesAtAngle++;
               if (framesAtAngle >= 4) {
                  sample(minecraftclient, clientplayerentity);
                  sampleCount++;
                  framesAtAngle = 0;
                  pitch += 15.0F;
                  if (pitch > 45.01F) {
                     pitch = -45.0F;
                     yaw += 15.0F;
                     if (yaw > 165.01F) {
                        finish(minecraftclient, "complete");
                     }
                  }
               }
            }
         }
      }
   }

   private static void start(MinecraftClient client, Path gameDir, Path startFile) {
      ClientPlayerEntity clientplayerentity = client.getPlayerMapped();
      if (clientplayerentity != null) {
         if (client.getCurrentScreenMapped() != null) {
            client.openScreen(null);
         }

         savedYaw = clientplayerentity.getYRot();
         savedPitch = clientplayerentity.getXRot();
         yaw = -180.0F;
         pitch = -45.0F;
         framesAtAngle = 0;
         waitFrames = 0;
         sampleCount = 0;
         anomalyCount = 0;
         visualVanishCount = 0;
         running = true;
         finished = false;
         waitingForRails = true;

         try {
            Files.createDirectories(gameDir.resolve("logs"));
            csvPath = gameDir.resolve("logs/mtr-rail-vanish-probe.csv");
            Files.writeString(
               csvPath,
               "yaw,pitch,railIdx,rx,ry,rz,lookDot,ndcCapX,ndcCapY,ndcCapZ,inCap,sleeperPx,visualVanish,viewDiff,transLen,capValid\n",
               StandardCharsets.UTF_8
            );
            Files.deleteIfExists(startFile);
            Files.deleteIfExists(gameDir.resolve("mtr-rail-vanish-probe.done"));
            Files.writeString(gameDir.resolve("mtr-rail-vanish-probe.running"), "1", StandardCharsets.UTF_8);
         } catch (IOException ioexception) {
            LOGGER.error("[MTR-RailVanishProbe] failed to init", ioexception);
            running = false;
            return;
         }

         LOGGER.info("[MTR-RailVanishProbe] START");
         clientplayerentity.sendMessage(new Text((Component)TextHelper.literal("[MTR] Rail vanish probe START").data), false);
      }
   }

   private static void finish(MinecraftClient client, String reason) {
      running = false;
      finished = true;
      ClientPlayerEntity clientplayerentity = client.getPlayerMapped();
      if (clientplayerentity != null) {
         clientplayerentity.setYRot(savedYaw);
         clientplayerentity.setXRot(savedPitch);
         clientplayerentity.sendMessage(
            new Text(
               (Component)TextHelper.literal(
                     "[MTR] Probe " + reason + " samples=" + sampleCount + " matrixAnom=" + anomalyCount + " visualVanish=" + visualVanishCount
                  )
                  .data
            ),
            false
         );
      }

      Path path = client.getRunDirectoryMapped().toPath();

      try {
         Files.deleteIfExists(path.resolve("mtr-rail-vanish-probe.running"));
         Files.writeString(
            path.resolve("mtr-rail-vanish-probe.done"),
            "reason=" + reason + " samples=" + sampleCount + " anomalies=" + anomalyCount + " visualVanish=" + visualVanishCount + "\n",
            StandardCharsets.UTF_8
         );
      } catch (IOException ioexception) {
      }

      LOGGER.info("[MTR-RailVanishProbe] DONE reason={} samples={} matrixAnom={} visualVanish={}", reason, sampleCount, anomalyCount, visualVanishCount);
   }

   private static void aimAtNearestRail(MinecraftClient client, ClientPlayerEntity player) {
      Vector3d vector3d = client.getGameRendererMapped().getCamera().getPos();
      List<float[]> list = nearestRails(vector3d, 1);
      if (!list.isEmpty()) {
         float[] afloat = list.get(0);
         float f = afloat[0] - (float)vector3d.getXMapped();
         float f1 = afloat[1] - (float)vector3d.getYMapped();
         float f2 = afloat[2] - (float)vector3d.getZMapped();
         float f3 = (float)Math.sqrt(f * f + f2 * f2);
         yaw = (float)Math.toDegrees(Math.atan2(-f, f2));
         pitch = (float)Math.toDegrees(-Math.atan2(f1, Math.max(f3, 0.001F)));
         pitch = Math.max(-45.0F, Math.min(45.0F, pitch));
         yaw = Math.round(yaw / 15.0F) * 15.0F;
         pitch = Math.round(pitch / 15.0F) * 15.0F;
         player.setYRot(yaw);
         player.setXRot(pitch);
      }
   }

   private static void sample(MinecraftClient client, ClientPlayerEntity player) {
      Vector3d vector3d = client.getGameRendererMapped().getCamera().getPos();
      float f = (float)Math.toRadians(yaw);
      float f1 = (float)Math.toRadians(pitch);
      Vector3f vector3f = new Vector3f(-((float)Math.sin(f)) * (float)Math.cos(f1), -((float)Math.sin(f1)), (float)Math.cos(f) * (float)Math.cos(f1))
         .normalize();
      Matrix4f matrix4f = CapturedRenderMatrices.copyViewOrNull();
      Matrix4f matrix4f1 = CapturedRenderMatrices.copyProjectionOrNull();
      Matrix4f matrix4f2 = org.mtr.mapping.render.CapturedRenderMatrices.copyViewOrNull();
      boolean flag = matrix4f != null && matrix4f1 != null;
      float f2 = flag ? frobeniusDiff(matrix4f, matrix4f2) : -1.0F;
      float f3 = flag ? translationLength(matrix4f) : -1.0F;
      int i = countSleeperPixels();
      List<float[]> list = nearestRails(vector3d, 6);
      if (list.isEmpty()) {
         appendCsv(String.format(Locale.ROOT, "%.1f,%.1f,-1,0,0,0,0,0,0,0,0,%d,0,%.5f,%.5f,%s\n", yaw, pitch, i, f2, f3, flag));
      } else {
         StringBuilder stringbuilder = new StringBuilder();
         int j = 0;
         int k = 0;
         boolean flag1 = false;

         for (int l = 0; l < list.size(); l++) {
            float[] afloat = list.get(l);
            float f4 = afloat[0] - (float)vector3d.getXMapped();
            float f5 = afloat[1] - (float)vector3d.getYMapped();
            float f6 = afloat[2] - (float)vector3d.getZMapped();
            Vector3f vector3f1 = new Vector3f(f4, f5, f6);
            float f7 = vector3f1.length();
            float f8 = f7 < 0.001F ? 0.0F : new Vector3f(vector3f1).normalize().dot(vector3f);
            Matrix4f matrix4f3 = new Matrix4f().translation(f4, f5, f6);
            Vector3f vector3f2 = flag ? project(matrix4f1, matrix4f, matrix4f3) : nan();
            boolean flag2 = inNdc(vector3f2);
            int i1 = 0;
            if (f8 > 0.85F) {
               flag1 = true;
               j++;
               if (!flag2) {
                  k++;
                  anomalyCount++;
               }

               if (flag2 && i < 80) {
                  i1 = 1;
                  visualVanishCount++;
                  LOGGER.warn(
                     "[MTR-RailVanishProbe] VISUAL-VANISH yaw={} pitch={} lookDot={} sleeperPx={} ndc={}",
                     yaw,
                     pitch,
                     String.format(Locale.ROOT, "%.3f", f8),
                     i,
                     fmt(vector3f2)
                  );
               }
            }

            stringbuilder.append(
               String.format(
                  Locale.ROOT,
                  "%.1f,%.1f,%d,%.3f,%.3f,%.3f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%.5f,%.5f,%s\n",
                  yaw,
                  pitch,
                  l,
                  afloat[0],
                  afloat[1],
                  afloat[2],
                  f8,
                  vector3f2.x,
                  vector3f2.y,
                  vector3f2.z,
                  flag2 ? 1 : 0,
                  i,
                  i1,
                  f2,
                  f3,
                  flag
               )
            );
         }

         appendCsv(stringbuilder.toString());
         if (sampleCount % 5 == 0 || k > 0 || flag1 && i < 80) {
            LOGGER.info(
               "[MTR-RailVanishProbe] yaw={} pitch={} hits={} outCap={} sleeperPx={} viewDiff={}", yaw, pitch, j, k, i, String.format(Locale.ROOT, "%.5f", f2)
            );
         }
      }
   }

   private static volatile int lastSleeperPixelCount = -1;
   private static volatile boolean readbackPending;

   private static int countSleeperPixels() {
      if (!readbackPending) {
         readbackPending = true;
         try {
            net.minecraft.client.Screenshot.takeScreenshot(net.minecraft.client.Minecraft.getInstance().gameRenderer.mainRenderTarget(), image -> {
               try (image) {
                  int count = 0;
                  int left = image.getWidth() / 4, top = image.getHeight() / 5;
                  int right = Math.min(image.getWidth(), left + image.getWidth() / 2);
                  int bottom = Math.min(image.getHeight(), top + image.getHeight() * 2 / 5);
                  for (int y = top; y < bottom; y++) for (int x = left; x < right; x++) {
                     int color = image.getPixel(x, y);
                     int red = color >>> 16 & 255, green = color >>> 8 & 255, blue = color & 255;
                     int max = Math.max(red, Math.max(green, blue));
                     int min = Math.min(red, Math.min(green, blue));
                     if (max >= 160 && max - min <= 40) count++;
                  }
                  lastSleeperPixelCount = count;
               } finally { readbackPending = false; }
            });
         } catch (Exception failure) {
            readbackPending = false;
            LOGGER.warn("[MTR-RailVanishProbe] Native screenshot readback failed: {}", failure.toString());
         }
      }
      return lastSleeperPixelCount;
   }

   private static List<float[]> nearestRails(Vector3d cam, int limit) {
      List<float[]> list = new ArrayList<>();
      int i = MinecraftClientData.getInstance().railWrapperList.size();

      try {
         ObjectIterator e = MinecraftClientData.getInstance().railWrapperList.values().iterator();

         while (e.hasNext()) {
            MinecraftClientData.RailWrapper minecraftclientdata$railwrapper = (MinecraftClientData.RailWrapper)e.next();
            if (minecraftclientdata$railwrapper != null) {
               float f = (float)((minecraftclientdata$railwrapper.startVector.x + minecraftclientdata$railwrapper.endVector.x) * 0.5);
               float f1 = (float)((minecraftclientdata$railwrapper.startVector.y + minecraftclientdata$railwrapper.endVector.y) * 0.5);
               float f2 = (float)((minecraftclientdata$railwrapper.startVector.z + minecraftclientdata$railwrapper.endVector.z) * 0.5);
               list.add(new float[]{f, f1, f2});
               if (minecraftclientdata$railwrapper.getRail() != null) {
                  try {
                     minecraftclientdata$railwrapper.getRail()
                        .railMath
                        .render(
                           (x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> list.add(
                              new float[]{(float)((x1 + x3) * 0.5), (float)((y1 + y2) * 0.5), (float)((z1 + z3) * 0.5)}
                           ),
                           8.0,
                           0.0F,
                           0.0F
                        );
                  } catch (Exception exception) {
                  }
               }
            }
         }
      } catch (Exception exception1) {
         LOGGER.error("[MTR-RailVanishProbe] rail sample failed wrappers={}", i, exception1);
      }

      list.sort(Comparator.comparingDouble(a -> {
         double d0 = a[0] - cam.getXMapped();
         double d1 = a[1] - cam.getYMapped();
         double d2 = a[2] - cam.getZMapped();
         return d0 * d0 + d1 * d1 + d2 * d2;
      }));
      return (List<float[]>)(list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list);
   }

   private static float translationLength(Matrix4f view) {
      float f = view.m30();
      float f1 = view.m31();
      float f2 = view.m32();
      return (float)Math.sqrt(f * f + f1 * f1 + f2 * f2);
   }

   private static Vector3f nan() {
      return new Vector3f(Float.NaN, Float.NaN, Float.NaN);
   }

   private static Vector3f project(Matrix4f proj, Matrix4f view, Matrix4f model) {
      Vector4f vector4f = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
      model.transform(vector4f);
      view.transform(vector4f);
      proj.transform(vector4f);
      return Math.abs(vector4f.w) < 1.0E-8F
         ? new Vector3f(Float.NaN, Float.NaN, Float.NaN)
         : new Vector3f(vector4f.x / vector4f.w, vector4f.y / vector4f.w, vector4f.z / vector4f.w);
   }

   private static boolean inNdc(Vector3f ndc) {
      return ndc != null && !Float.isNaN(ndc.x)
         ? ndc.x >= -1.05F && ndc.x <= 1.05F && ndc.y >= -1.05F && ndc.y <= 1.05F && ndc.z >= -1.05F && ndc.z <= 1.05F
         : false;
   }

   private static float frobeniusDiff(Matrix4f a, Matrix4f b) {
      float f = 0.0F;
      float[] afloat = new float[16];
      float[] afloat1 = new float[16];
      a.get(afloat);
      b.get(afloat1);

      for (int i = 0; i < 16; i++) {
         float f1 = afloat[i] - afloat1[i];
         f += f1 * f1;
      }

      return (float)Math.sqrt(f);
   }

   private static String fmt(Vector3f v) {
      return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", v.x, v.y, v.z);
   }

   private static void appendCsv(String line) {
      if (csvPath != null) {
         try {
            Files.writeString(csvPath, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (IOException ioexception) {
            LOGGER.error("[MTR-RailVanishProbe] write failed", ioexception);
         }
      }
   }
}
