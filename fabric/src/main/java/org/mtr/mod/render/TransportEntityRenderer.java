package org.mtr.mod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.ClientWorld;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.entity.TransportEntity;

import java.util.HashMap;
import java.util.Map;

/** Captures each car's model commands during extraction and submits them with the entity. */
public final class TransportEntityRenderer extends EntityRenderer<TransportEntity, TransportEntityRenderer.State> {
	private static final Map<Key, State> extractingTrains = new HashMap<>();

	public TransportEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public static void register() {
		EntityRendererRegistry.register(TransportEntity.TYPE, TransportEntityRenderer::new);
		LevelExtractionEvents.END_EXTRACTION.register(context -> {
			if (OptimizedRenderer.renderingShadows()) return;
			try {
				for (EntityRenderState entity : context.levelState().entityRenderStates) {
					if (!(entity instanceof State state) || state.transportId == 0) continue;
					if (state.car >= 0) {
						extractingTrains.put(new Key(state.transportId, state.car), state);
					} else {
						final var lift = MinecraftClientData.getInstance().liftWrapperList.get(state.transportId);
						MainRenderer.captureEntityCommands(state, () -> RenderLifts.prepareEntity(state,
								lift == null ? null : lift.getLift(),
								new ClientWorld(net.minecraft.client.Minecraft.getInstance().level)));
					}
				}
				MainRenderer.extractVehicles(new Vector3d(context.camera().position()));
			} finally {
				extractingTrains.clear();
			}
		});
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(TransportEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.transportId = entity.transportId();
		state.car = entity.carIndex();
		state.commands.clear();
		if (state.car < 0) {
			state.cabinWidth = entity.cabinWidth();
			state.cabinDepth = entity.cabinLength();
			state.cabinHeight = entity.cabinHeight();
			state.liftStyle = entity.liftStyle();
			state.liftDoubleSided = entity.liftDoubleSided();
			state.liftDoor = entity.liftDoor();
		}
		state.pose = new PositionAndRotation(new Vector(state.x, state.y, state.z),
				-Math.toRadians(entity.getViewYRot(partialTick)), Math.toRadians(entity.getViewXRot(partialTick)));
	}

	@Override
	public void submit(State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, matrices, collector, camera);
		if (state.commands.isEmpty()) return;
		GraphicsHolder.createInstanceSafe(matrices, collector,
				graphics -> MainRenderer.submitEntityCommands(state.commands, graphics, new Vector3d(state.x, state.y, state.z)));
	}

	static State train(long id, int car) {
		return extractingTrains.get(new Key(id, car));
	}

	public static final class State extends EntityRenderState {
		private long transportId;
		private int car;
		PositionAndRotation pose;
		float cabinWidth, cabinDepth, cabinHeight, liftDoor;
		String liftStyle;
		boolean liftDoubleSided;
		final java.util.List<MainRenderer.ModelCommand> commands = new java.util.ArrayList<>();
	}

	private record Key(long id, int car) {}
}
