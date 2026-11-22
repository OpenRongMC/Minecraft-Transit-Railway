package org.mtr.test;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.mtr.mod.block.BlockPhysicalRail;
import org.mtr.mod.data.RailBlockGeometry;
import org.mtr.mod.entity.TransportEntity;
import org.mtr.mod.screen.ConfigScreen;

public final class TransportGameTest implements FabricClientGameTest {
    private static boolean loadingLegacyFixture;
    private static net.minecraft.client.gui.screens.Screen acceptedWarning;
    @Override public void runTest(ClientGameTestContext test) {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(loadingLegacyFixture && client.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen warning && acceptedWarning!=warning) {
                acceptedWarning=warning;
                System.out.println("[RongMC-QA] Isolated legacy fixture warning: "+warning.getNarrationMessage().getString());
                try {
                    var field=net.minecraft.client.gui.screens.ConfirmScreen.class.getDeclaredField("callback");field.setAccessible(true);
                    ((it.unimi.dsi.fastutil.booleans.BooleanConsumer)field.get(warning)).accept(true);
                }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
            }
        });
        test.runOnClient(client->{client.options.renderDistance().set(3);client.options.guiScale().set(2);});
        try(var world=test.worldBuilder().create()) {
            var server=world.getServer();var connection=world.getConnection();
            server.runOnServer(game->{
                var player=connection.getServerPlayer();var level=player.level();
                for(int x=-4;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,109,z),Blocks.STONE.defaultBlockState());
                var rail=BuiltInRegistries.BLOCK.getValue(Identifier.parse("mtr:physical_rail"));
                for(int x=-1;x<=1;x++) {
                    var pos=new BlockPos(x,110,0);level.setBlockAndUpdate(pos,rail.defaultBlockState());
                    var data=(BlockPhysicalRail.Data)level.getBlockEntity(pos);
                    var shape=new RailBlockGeometry.Geometry(List.of(new RailBlockGeometry.Part("qa",true,.26428,List.of(
                        new RailBlockGeometry.Point(0,0,.2),new RailBlockGeometry.Point(1,0,.2),new RailBlockGeometry.Point(1,0,.8),new RailBlockGeometry.Point(0,0,.8)))));
                    data.update("qa","",shape.encode());
                    check(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"Track has no collision");
                }
                for(var fixture:List.of("mtr:railway_sign_4_even","jsblock:auto_iron_door","yte:pat_p01_ticket_barrier_bare")) {
                    var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(fixture));
                    check(block!=Blocks.AIR,"Missing block "+fixture);
                }
                var aliases = com.google.gson.JsonParser.parseString("{\"playerDetectRange\":\"7\",\"smokeAlarmRange\":\"12\",\"preventBlockFalling\":\"true\"}");
                var migrated = net.minecraft.world.level.gamerules.GameRuleMap.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, aliases).getOrThrow();
                var rule = BuiltInRegistries.GAME_RULE.getValue(Identifier.parse("tjmetro:player_detect_range"));
                check(migrated.get(rule).equals(7), "Historical game-rule value was lost");
                level.setBlockAndUpdate(new BlockPos(2,110,0),BuiltInRegistries.BLOCK.getValue(Identifier.parse("yte:pat_p01_ticket_barrier_bare")).defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(-2,110,0),BuiltInRegistries.BLOCK.getValue(Identifier.parse("mtr:railway_sign_4_even")).defaultBlockState());
                player.teleportTo(level,.5,110,-3.5,Set.of(),0,10,false);
            });
            connection.waitForChunksRender();test.waitTicks(8);test.takeScreenshot("01-three-block-track");
            test.runOnClient(client -> {
                final String[] fixtures = {"fujitec_mic400_button_1", "pat_p01_ticket_barrier_bare", "yte_lift_buttons_link_connector"};
                for (final String fixture : fixtures) {
                    final var itemId = Identifier.fromNamespaceAndPath("yte", fixture);
                    check(client.getResourceManager().getResource(itemId.withPrefix("items/").withSuffix(".json")).isPresent(),
                            "Missing YTE item definition " + itemId);
                    final var itemModel = client.getModelManager().getItemModel(itemId);
                    check(itemModel != client.getModelManager().getItemModel(Identifier.parse("yte:qa_missing_model")),
                            "YTE item definition baked to the missing model " + itemId);
                }
            });
            test.setScreen(()->new ConfigScreen(null));test.waitForScreen(ConfigScreen.class);test.takeScreenshot("02-mtr-config");
            boolean announcementBefore=org.mtr.mod.config.Config.getClient().getChatAnnouncements();
            var button=test.computeOnClient(client->(net.minecraft.client.gui.components.AbstractWidget)client.gui.screen().children().getFirst());
            double scale=test.computeOnClient(client->(double)client.getWindow().getWidth()/client.getWindow().getGuiScaledWidth());
            test.getInput().setCursorPos((button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale);
            test.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            test.waitFor(client->org.mtr.mod.config.Config.getClient().getChatAnnouncements()!=announcementBefore);
            test.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
            test.waitFor(client->org.mtr.mod.config.Config.getClient().getChatAnnouncements()==announcementBefore);
            test.runOnClient(client->client.gui.setScreen(null));
            verifyOrielUi(test);
            verifyFilterButton(test);
            verifyModernUiNavigation(test);
            verifyAddonScreens(test);
            verifyYteFloorEditor(test,world);
            verifyNativeLiftModel(test);
            verifySimulatorSpawnsLift(test, world);
            server.runOnServer(game->{
                var player=connection.getServerPlayer();var entity=new TransportEntity(TransportEntity.TYPE,player.level());
                entity.bind(1234567,0);entity.configure(2.3,8,3,true);entity.setPos(player.position());
                check(player.level().addFreshEntity(entity), "Could not create transport test car");
                final Vec3 position = entity.position();
                final var floor = new net.minecraft.world.phys.AABB(position.x - .2, position.y - .1, position.z - .2,
                        position.x + .2, position.y + .2, position.z + .2);
                check(!player.level().getEntityCollisions(player, floor).isEmpty(), "World collision omitted the transport floor");
                check(entity.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
                        "Native boarding interaction rejected");
                check(!entity.canBeCollidedWith(player), "A mounted player collided with their own vehicle");
                check(!entity.getBoundingBox().contains(entity.getDismountLocationForPassenger(player)),
                        "Dismount placed the player inside the solid vehicle");
            });
            connection.waitForClientboundPackets();test.waitFor(client->client.player.isPassenger());
            check(server.computeOnServer(game->connection.getServerPlayer().getVehicle() instanceof TransportEntity),"Server passenger not attached");
            test.takeScreenshot("03-native-passenger");
            final double movedX = server.computeOnServer(game -> {
                var vehicle = connection.getServerPlayer().getVehicle();
                final double x = vehicle.getX() + 2;
                vehicle.setPos(x, vehicle.getY(), vehicle.getZ());
                return x;
            });
            test.waitFor(client -> client.player.isPassenger() && Math.abs(client.player.getX() - movedX) < 0.4);
            check(server.computeOnServer(game -> Math.abs(connection.getServerPlayer().getX() - movedX) < 0.4),
                    "Native passenger did not move with the car");
            server.runOnServer(game -> {
                final var player = connection.getServerPlayer();
                final var fixture = player.getVehicle();
                player.stopRiding();
                if (fixture != null) fixture.discard();
            });
            connection.waitForClientboundPackets();test.waitFor(client->!client.player.isPassenger());
            server.runOnServer(game -> {
                verifyWalkThroughVehicle(connection.getServerPlayer(), -1);
                verifyWalkThroughVehicle(connection.getServerPlayer(), 0);
            });
        }
        java.nio.file.Path source=java.nio.file.Path.of(System.getProperty("mtr.qaLegacyWorld","/data/RongMC/.restore/creative-server/world"));
        if(!Boolean.getBoolean("mtr.qaSkipLegacyWorld") && java.nio.file.Files.isDirectory(source)) verifyLegacyWorld(test,source);
    }
    private static void verifyWalkThroughVehicle(net.minecraft.server.level.ServerPlayer player, int car) {
        final var level = player.level();
        final Vec3 original = player.position();
        final float yaw = player.getYRot();
        final Vec3 center = new Vec3(0, 110, -4);
        // Both lift and train use the same native passenger and server movement path.
        final Vec3 entrance = car < 0 ? new Vec3(0, 0, -1.4) : new Vec3(-1.4, 0, 0);
        final Vec3 step = car < 0 ? new Vec3(0, 0, .85) : new Vec3(.85, 0, 0);
        final var entity = new TransportEntity(TransportEntity.TYPE, level);
        entity.bind(2345678 + car, car);
        entity.configure(2, 2, 3, false);
        entity.setPos(center);
        check(level.addFreshEntity(entity), "Could not create walk-in fixture");
        final var landing = BlockPos.containing(center.add(car < 0 ? new Vec3(0, 0, -1) : new Vec3(-1, 0, 0)));
        final var door = BuiltInRegistries.BLOCK.getValue(Identifier.parse(car < 0 ? "yte:kone_m_door_1" : "mtr:psd_door"));
        check(door != Blocks.AIR, "Missing platform screen door fixture");
        final var originalDoor = level.getBlockState(landing);
        level.setBlock(landing, door.defaultBlockState()
                .setValue(org.mtr.mod.block.BlockPSDAPGDoorBase.UNLOCKED.data, true), 2 | 16);
        check(level.getBlockState(landing).is(door), "Landing door block did not survive placement");
        check(!level.getBlockState(landing).getCollisionShape(level, landing).isEmpty(), "Closed landing door had no collision");
        try {
            player.setPos(center.add(entrance));
            player.move(net.minecraft.world.entity.MoverType.SELF, step);
            check(player.position().subtract(center).horizontalDistance() >= 1.25,
                    "Closed vehicle did not block walking");
            entity.configure(2, 2, 3, true);
            check(level.getBlockState(landing).getCollisionShape(level, landing).isEmpty(), "Open landing door still blocked the player");
            player.move(net.minecraft.world.entity.MoverType.SELF, step);
            entity.tick();
            check(player.getVehicle() == entity, "Walking through the open doorway did not board");
            final Vec3 prior = entity.getPassengerRidingPosition(player);
            entity.configure(2, 2, 3, false);
            entity.setPos(center.add(0, 2, 0));
            check(entity.getPassengerRidingPosition(player).y > prior.y + 1.9,
                    "Moving cabin did not carry the walking passenger");
            player.setYRot(0);
            player.setLastClientInput(new net.minecraft.world.entity.player.Input(car >= 0, false, car < 0, false, false, false, false));
            entity.tick();
            check(entity.getPassengerRidingPosition(player).subtract(prior).horizontalDistance() > .05,
                    "Passenger could not walk inside the moving cabin");
            player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
            entity.setPos(center);
            entity.configure(2, 2, 3, true);
            player.setYRot(0);
            player.setLastClientInput(new net.minecraft.world.entity.player.Input(false, car < 0, false, car >= 0, false, false, false));
            for (int i = 0; i < 8 && player.getVehicle() == entity; i++) entity.tick();
            check(player.getVehicle() != entity && !entity.getBoundingBox().contains(player.position()),
                    "Walking back through the doorway did not dismount");
        } finally {
            player.setLastClientInput(net.minecraft.world.entity.player.Input.EMPTY);
            player.stopRiding();
            player.setYRot(yaw);
            player.setPos(original);
            level.setBlock(landing, originalDoor, 2);
            entity.discard();
        }
    }

    private static void verifyLegacyWorld(ClientGameTestContext test,java.nio.file.Path source) {
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
        try(var empty=test.worldBuilder().create()){save=empty.getWorldSave();}
        try {
            var destination=save.getSaveDirectory();
            java.nio.file.Files.move(destination,destination.resolveSibling(destination.getFileName()+"-fresh-fixture-backup"));
            java.nio.file.Files.createDirectories(destination);
            var process=new ProcessBuilder("cp","-a","--reflink=auto",source.toString()+"/.",destination.toString()).start();
            check(process.waitFor()==0,"Could not copy legacy fixture world");
        }catch(java.io.IOException|InterruptedException failure){throw new RuntimeException(failure);}
        loadingLegacyFixture=true;
        try(var world=save.open()) {
            loadingLegacyFixture=false;
            test.runOnClient(client->{client.options.renderDistance().set(3);client.options.simulationDistance().set(3);});
            var connection=world.getConnection();var server=world.getServer();
            server.runOnServer(game->{
                var player=connection.getServerPlayer();var level=player.level();
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                player.getAbilities().flying=true;player.onUpdateAbilities();
                player.teleportTo(level,2832.5,50,565.5,Set.of(),-90,14,false);
                System.out.println("[RongMC-QA] Legacy server dimension="+level.dimension()+" position="+player.position());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,12000,0,false,false));
            });
            connection.waitForClientboundPackets();
            test.waitFor(client->client.player!=null && client.player.position().distanceToSqr(new Vec3(2832.5,50,565.5))<4);
            connection.waitForChunksRender();
            test.waitFor(client->client.level!=null && java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(),false).anyMatch(entity->entity instanceof TransportEntity),1200);
            test.waitTicks(100);connection.waitForChunksRender();
            test.runOnClient(client->{
                System.out.println("[RongMC-QA] Legacy client dimension="+client.level.dimension()+" position="+client.player.position());
                for(var entity:client.level.entitiesForRendering())if(entity instanceof TransportEntity transport)System.out.println("[RongMC-QA] Transport "+transport.transportId()+" position="+entity.position());
            });
            test.getInput().lookAt(-90,14);test.takeScreenshot("04-legacy-train-and-tracks");
            server.runOnServer(game->{
                var level=connection.getServerPlayer().level();
                for(int x=2835;x<=2837;x++) {
                    var pos=new BlockPos(x,48,570);var block=level.getBlockState(pos);
                    check(block.getBlock() instanceof BlockPhysicalRail,"Legacy track was not converted at "+pos);
                    check(!block.getCollisionShape(level,pos).isEmpty(),"Migrated track has empty collision at "+pos);
                }
            });
            verifyDriverRegistration(test, world);
            verifyImportedTrainPack(test);
            verifyLegacyTrackAcrossChunks(test, world);
        } finally { loadingLegacyFixture=false; }
    }

    private static void verifyLegacyTrackAcrossChunks(ClientGameTestContext test, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        final var connection = world.getConnection();
        final var server = world.getServer();
        final var snowPosition = new BlockPos(1446, 64, 582);
        server.runOnServer(game -> {
            final var player = connection.getServerPlayer();
            final var level = player.level();
            level.setBlockAndUpdate(snowPosition.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(snowPosition, Blocks.SNOW.defaultBlockState());
            check(level.getBlockState(snowPosition).is(Blocks.SNOW), "Could not place the snow migration fixture");
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            player.teleportTo(level, 1440, 92, 591, Set.of(), -90, 54, false);
        });
        connection.waitForClientboundPackets();
        test.waitFor(client -> client.player != null && client.player.position().distanceToSqr(new Vec3(1440, 92, 591)) < 4);
        connection.waitForChunksRender();
        test.waitTicks(200);
        server.runOnServer(game -> {
            final var level = connection.getServerPlayer().level();
            for (int x = 1424; x < 1472; x++) {
                for (final int z : new int[]{582, 585, 595, 601}) {
                    final var pos = new BlockPos(x, z < 590 ? 64 : 66, z);
                    check(level.getBlockEntity(pos) instanceof BlockPhysicalRail.Data data && !data.geometry().parts().isEmpty(),
                            "Migrated track is missing at " + pos);
                }
            }
        });
        for (final var position : List.of(new BlockPos(1430, 64, 582), snowPosition, new BlockPos(1462, 64, 582))) {
            test.waitFor(client -> client.level.getBlockEntity(position) instanceof BlockPhysicalRail.Data data && !data.geometry().parts().isEmpty());
        }
        connection.waitForChunksRender();
        test.getInput().lookAt(-90, 54);
        test.takeScreenshot("legacy-four-tracks-across-chunks");
    }
    private static void verifyDriverRegistration(ClientGameTestContext test, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        var connection = world.getConnection();
        var server = world.getServer();
        long trainId = server.computeOnServer(game -> {
            var player = connection.getServerPlayer();
            var train = player.level().getEntitiesOfClass(TransportEntity.class, player.getBoundingBox().inflate(24),
                            entity -> entity.carIndex() >= 0).stream()
                    .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElseThrow();
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse("mtr:creative_driver_key"));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
            var position = train.position();
            player.teleportTo(player.level(), position.x, position.y, position.z, Set.of(), 0, 0, false);
            check(train.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction(),
                    "Driver key could not board a scheduled train");
            return train.transportId();
        });
        connection.waitForClientboundPackets();
        test.waitFor(client -> client.player.getVehicle() instanceof TransportEntity train && train.transportId() == trainId);
        test.waitFor(client -> {
            var playerId = client.player.getUUID();
            for (var vehicle : org.mtr.mod.client.MinecraftClientData.getInstance().vehicles) {
                if (vehicle.getId() != trainId) continue;
                boolean[] registered = {false};
                vehicle.vehicleExtraData.iterateRidingEntities(rider -> {
                    if (rider.uuid.equals(playerId) && rider.isDriver()) registered[0] = true;
                });
                if (registered[0]) return true;
            }
            return false;
        }, 400);
        server.runOnServer(game -> connection.getServerPlayer().stopRiding());
        connection.waitForClientboundPackets();
        test.waitFor(client -> !client.player.isPassenger());
    }

    private static void verifyImportedTrainPack(ClientGameTestContext test) {
        var reload = test.computeOnClient(client -> {
            var resources = client.getResourceManager();
            var manifestId = Identifier.parse("mtr:mtr_custom_resources.json");
            var modelId = Identifier.parse("mtr:models/vehicle/sp1900.bbmodel");
            var packDirectory = client.getResourcePackDirectory().resolve("rongmc-import-qa");
            try (var reader = resources.openAsReader(manifestId)) {
                var vehicles = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("vehicles");
                var train = java.util.stream.StreamSupport.stream(vehicles.spliterator(), false)
                        .map(com.google.gson.JsonElement::getAsJsonObject)
                        .filter(vehicle -> vehicle.get("id").getAsString().equals("sp1900_trailer"))
                        .findFirst().orElseThrow().deepCopy();
                train.addProperty("id", "rongmc_import_qa");
                train.addProperty("name", "RongMC Imported Model QA");
                var model = train.getAsJsonArray("models").get(0).getAsJsonObject();
                model.addProperty("modelResource", "rongmc_qa:train.bbmodel");

                var trainList = new com.google.gson.JsonArray();
                trainList.add(train);
                var manifest = new com.google.gson.JsonObject();
                manifest.add("vehicles", trainList);
                var manifestPath = packDirectory.resolve("assets/mtr/mtr_custom_resources.json");
                java.nio.file.Files.createDirectories(manifestPath.getParent());
                java.nio.file.Files.writeString(manifestPath, manifest.toString());

                var customModelPath = packDirectory.resolve("assets/rongmc_qa/train.bbmodel");
                java.nio.file.Files.createDirectories(customModelPath.getParent());
                try (var modelStream = resources.open(modelId)) {
                    java.nio.file.Files.copy(modelStream, customModelPath);
                }

                var packType = net.minecraft.server.packs.metadata.pack.PackMetadataSection.CLIENT_TYPE;
                var metadata = new com.google.gson.JsonObject();
                metadata.add(packType.name(), org.mtr.mod.servlet.ResourcePackCreatorUploadServlet.encodePackMetadata("RongMC Import QA"));
                java.nio.file.Files.writeString(packDirectory.resolve("pack.mcmeta"), metadata.toString());
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }

            var packs = client.getResourcePackRepository();
            packs.reload();
            var pack = packs.getPack("file/rongmc-import-qa");
            check(pack != null && pack.getCompatibility().isCompatible(), "26.3 rejected the exported metadata");
            check(packs.addPack(pack.getId()), "Could not select custom train resource pack");
            return client.reloadResourcePacks();
        });
        test.waitFor(client -> reload.isDone(), 1200);
        reload.join();
        test.runOnClient(client -> {
            boolean[] found = {false};
            org.mtr.mod.client.CustomResourceLoader.getVehicleById(org.mtr.core.data.TransportMode.TRAIN,
                    "rongmc_import_qa", details -> details.left().iterateModels(0, 1, (index, model) -> found[0] = true));
            check(found[0], "Custom train and Blockbench model were not loaded");
        });
    }

    private static void verifyOrielUi(ClientGameTestContext test) {
        test.setScreen(() -> new org.mtr.init.ModMenuConfig().getModConfigScreenFactory().create(null));
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("oriel")) {
            test.waitForScreen(ConfigScreen.class);
            clickButton(test, "rongmc.transportui.style");
            test.waitFor(client -> client.gui.screen().getClass().getSimpleName().equals("TransportUiSettings"));
            clickButton(test, "gui.done");
            test.waitForScreen(ConfigScreen.class);
            test.runOnClient(client -> client.gui.setScreen(null));
            return;
        }
        test.waitForScreen(dev.oriel.mod.client.OrielScreen.class);
        test.waitFor(client -> dev.oriel.mod.client.OrielAutomation.screen()
                .flatMap(page -> page.rect("#announcements")).isPresent());
        test.takeScreenshot("oriel-mtr-settings");
        test.setScreen(TransportGameTest::createYteSettings);
        test.waitForScreen(dev.oriel.mod.client.OrielScreen.class);
        test.waitFor(client -> dev.oriel.mod.client.OrielAutomation.screen()
                .flatMap(page -> page.rect("#watermark")).isPresent());
        test.takeScreenshot("oriel-yte-settings");
        test.setScreen(() -> new org.mtr.init.ModMenuConfig().getModConfigScreenFactory().create(null));
        test.waitFor(client -> dev.oriel.mod.client.OrielAutomation.screen()
                .flatMap(page -> page.rect("#announcements")).isPresent());
        final boolean old = org.mtr.mod.config.Config.getClient().getChatAnnouncements();
        test.runOnClient(client -> dev.oriel.mod.client.OrielAutomation.screen().orElseThrow().click("#announcements"));
        test.waitFor(client -> org.mtr.mod.config.Config.getClient().getChatAnnouncements() != old);
        test.runOnClient(client -> dev.oriel.mod.client.OrielAutomation.screen().orElseThrow().click("#announcements"));
        test.waitFor(client -> org.mtr.mod.config.Config.getClient().getChatAnnouncements() == old);
        test.runOnClient(client -> dev.oriel.mod.client.OrielAutomation.screen().orElseThrow().click("#style"));
        test.waitFor(client -> client.gui.screen().getClass().getSimpleName().equals("TransportUiSettings"));
        clickButton(test, "rongmc.transportui.oriel");
        test.waitFor(client -> hasPreference(client, "classic"));
        clickButton(test, "gui.done");
        test.waitForScreen(ConfigScreen.class);
        test.takeScreenshot("classic-mtr-settings");
        clickButton(test, "rongmc.transportui.style");
        test.waitFor(client -> client.gui.screen().getClass().getSimpleName().equals("TransportUiSettings"));
        clickButton(test, "rongmc.transportui.classic");
        test.waitFor(client -> hasPreference(client, "oriel"));
        clickButton(test, "gui.done");
        test.waitForScreen(dev.oriel.mod.client.OrielScreen.class);
        test.runOnClient(client -> client.gui.setScreen(null));
    }

    private static net.minecraft.client.gui.screens.Screen createYteSettings() {
        try {
            var api = (com.terraformersmc.modmenu.api.ModMenuApi) Class
                    .forName("top.xfunny.entrypoint.ModMenu").getConstructor().newInstance();
            return api.getModConfigScreenFactory().create(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Could not open YTE settings", error);
        }
    }

    private static void verifyFilterButton(ClientGameTestContext test) {
        final var button = test.computeOnClient(client -> new ziyue.filters.Filter(
                net.minecraft.network.chat.Component.literal("Filter QA"),
                () -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE),
                java.util.List.of(net.minecraft.world.item.Items.STONE)) {});
        test.setScreen(() -> new net.minecraft.client.gui.screens.Screen(
                net.minecraft.network.chat.Component.literal("Filter QA")) {
            @Override
            protected void init() {
                button.setPosition(width / 2 - 16, height / 2 - 13);
                addRenderableWidget(button);
            }
        });
        test.waitTicks(3);
        test.takeScreenshot("filters-control");
        var pointer = test.computeOnClient(client -> {
            double scale = (double) client.getWindow().getWidth() / client.getWindow().getGuiScaledWidth();
            return new Vec3((button.getX() + 16) * scale, (button.getY() + 13) * scale, 0);
        });
        test.getInput().setCursorPos(pointer.x, pointer.y);
        test.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        test.waitFor(client -> !button.enabled);
        test.runOnClient(client -> client.gui.setScreen(null));
    }

    private static boolean hasPreference(net.minecraft.client.Minecraft client, String mode) {
        try {
            return java.nio.file.Files.readString(client.gameDirectory.toPath()
                    .resolve("config/rongmc-transport-ui.txt")).strip().equals(mode);
        } catch (java.io.IOException failure) {
            return false;
        }
    }

    private static void verifyModernUiNavigation(ClientGameTestContext test) {
        test.setScreen(() -> new org.mtr.mod.screen.DashboardScreen(org.mtr.core.data.TransportMode.TRAIN));
        test.waitForScreen(org.mtr.mod.screen.DashboardScreen.class);
        test.takeScreenshot("mtr-dashboard");
        clickButton(test, "gui.mtr.model_library");
        test.waitForScreen(org.mtr.mod.screen.TrainModelLibraryScreen.class);
        test.takeScreenshot("mtr-model-library");
        clickButton(test, "gui.mtr.model_library_packs");
        test.waitForScreen(net.minecraft.client.gui.screens.packs.PackSelectionScreen.class);
        test.runOnClient(client -> client.gui.screen().onClose());
        test.waitForScreen(org.mtr.mod.screen.TrainModelLibraryScreen.class);
        test.waitFor(client -> client.gui.overlay() == null);
        clickButton(test, "gui.back");
        test.waitForScreen(org.mtr.mod.screen.DashboardScreen.class);

        final int creatorPort = test.computeOnClient(client -> org.mtr.mod.InitClient.getServerPort());
        check(creatorPort > 0, "The game did not expose the model creator");
        try (var http = java.net.http.HttpClient.newHttpClient()) {
            final var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + creatorPort + "/creator/"))
                    .timeout(java.time.Duration.ofSeconds(10)).GET().build();
            final var response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200 && response.body().contains("<app-root"), "The game did not serve the creator page");
        } catch (java.io.IOException | InterruptedException failure) {
            throw new RuntimeException(failure);
        }

        final var siding = test.computeOnClient(client -> new org.mtr.core.data.Siding(
                new org.mtr.core.data.Position(0, 110, 0), new org.mtr.core.data.Position(0, 110, 84), 84,
                org.mtr.core.data.TransportMode.TRAIN, org.mtr.mod.client.MinecraftClientData.getDashboardInstance()));
        test.setScreen(() -> new org.mtr.mod.screen.VehicleSelectorScreen(siding,
                (org.mtr.mod.screen.DashboardScreen)net.minecraft.client.Minecraft.getInstance().gui.screen()));
        test.waitForScreen(org.mtr.mod.screen.VehicleSelectorScreen.class);
        test.waitTicks(4);
        test.takeScreenshot("mtr-train-formation");
        test.runOnClient(client -> client.gui.setScreen(null));
    }

    private static void clickButton(ClientGameTestContext test, String key) {
        var point = test.computeOnClient(client -> {
            var label = net.minecraft.network.chat.Component.translatable(key).getString();
            var widget = client.gui.screen().children().stream()
                    .filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget)
                    .map(child -> (net.minecraft.client.gui.components.AbstractWidget) child)
                    .filter(button -> button.visible && button.active && button.getMessage().getString().equals(label))
                    .findFirst().orElseThrow();
            double scale = (double) client.getWindow().getWidth() / client.getWindow().getGuiScaledWidth();
            return new Vec3((widget.getX() + widget.getWidth() / 2.0) * scale,
                    (widget.getY() + widget.getHeight() / 2.0) * scale, 0);
        });
        test.getInput().setCursorPos(point.x, point.y);
        test.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void verifyAddonScreens(ClientGameTestContext test) {
        for(String entry:List.of("com.lx862.jcm.entrypoint.ModMenuConfig","top.mcmtr.init.ModMenuConfig","ziyue.tjmetro.fabric.ModMenuConfig","top.xfunny.entrypoint.ModMenu")) {
            test.setScreen(()->{
                try {
                    var menu=(com.terraformersmc.modmenu.api.ModMenuApi)Class.forName(entry).getConstructor().newInstance();
                    var screen=menu.getModConfigScreenFactory().create(null);
                    check(screen!=null,"Missing config screen "+entry);
                    return screen;
                }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
            });
            test.waitTicks(3);test.takeScreenshot("addon-"+entry.split("\\.")[2]);
            test.runOnClient(client->client.gui.setScreen(null));
        }
    }
    private static void verifySimulatorSpawnsLift(ClientGameTestContext test, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        final var liftId = new java.util.concurrent.CompletableFuture<Long>();
        world.getServer().runOnServer(server -> {
            final var level = server.overworld();
            for (int y = 110; y <= 114; y++) {
                final var id = Identifier.parse(y == 110 || y == 114 ? "mtr:lift_track_floor_1" : "mtr:lift_track_1");
                level.setBlockAndUpdate(new BlockPos(0, y, 3), BuiltInRegistries.BLOCK.getValue(id).defaultBlockState());
            }
            try {
                final var main = org.mtr.mod.Init.class.getDeclaredField("main");
                main.setAccessible(true);
                final var simulator = ((org.mtr.mixin.SimulationMainAccessor) main.get(null)).mtr$getSimulators().stream()
                        .filter(value -> value.dimension.equals(org.mtr.mod.Init.getWorldId(new org.mtr.mapping.holder.World(server.overworld())))).findFirst().orElseThrow();
                simulator.run(() -> {
                    final var lift = new org.mtr.core.data.Lift(simulator);
                    lift.setDimensions(3, 2, 2, 0, 0, 0);
                    final var floors = new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<org.mtr.core.data.LiftFloor>();
                    floors.add(new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(0, 110, 3), "1", "QA"));
                    floors.add(new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(0, 110, 3), "1", "duplicate"));
                    floors.add(new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(0, 114, 3), "2", "QA"));
                    // Old saves can contain multiple floor entries at the same block.
                    lift.setFloors(floors);
                    check(lift.getFloorCount() == 2, "Duplicate floor did not collapse");
                    simulator.lifts.add(lift);
                    liftId.complete(lift.getId());
                });
            } catch (ReflectiveOperationException failure) {
                throw new RuntimeException(failure);
            }
        });
        test.waitFor(client -> liftId.isDone() && client.level.getEntitiesOfClass(TransportEntity.class,
                client.player.getBoundingBox().inflate(16)).stream()
                .anyMatch(entity -> entity.carIndex() == -1 && entity.transportId() == liftId.join() && entity.cabinHeight() == 3 && Math.abs(entity.getY() - 110) < 1));
        test.takeScreenshot("native-lift-entity");
    }

    private static void verifyNativeLiftModel(ClientGameTestContext test) {
        test.runOnClient(client -> {
            final var data = org.mtr.mod.client.MinecraftClientData.getInstance();
            final var lift = new org.mtr.core.data.Lift(data);
            lift.setDimensions(3, 2, 2, 0, 0, 0);
            final var floors = new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<org.mtr.core.data.LiftFloor>();
            floors.add(new org.mtr.core.data.LiftFloor(new org.mtr.core.data.Position(0, 110, 2), "1", "QA"));
            lift.setFloors(floors);
            final var entity = new TransportEntity(TransportEntity.TYPE, client.level);
            entity.bind(lift.getId(), -1);
            entity.configure(2, 2, 3, false);
            entity.setPos(0, 110, 2);
            final var origin = entity.getBoundingBox();
            check(origin.getYsize() == 3, "Lift entity did not synchronize its dimensions");
            check(entity.canBeCollidedWith(client.player), "Lift did not opt into vanilla entity collisions");
            check(Math.abs(origin.getXsize() - 2) < .001 && Math.abs(origin.getZsize() - 2) < .001,
                    "Axis-aligned lift box exceeded its dimensions");
            final var renderer = (org.mtr.mod.render.TransportEntityRenderer) client.getEntityRenderDispatcher().getRenderer(entity);
            final var state = renderer.createRenderState();
            renderer.extractRenderState(entity, state, 1);
            try {
                final var prepare = org.mtr.mod.render.RenderLifts.class.getDeclaredMethod("prepareEntity",
                        state.getClass(), org.mtr.core.data.Lift.class, org.mtr.mapping.holder.ClientWorld.class);
                prepare.setAccessible(true);
                final var capture = org.mtr.mod.render.MainRenderer.class.getDeclaredMethod("captureEntityCommands",
                        state.getClass(), Runnable.class);
                capture.setAccessible(true);
                capture.invoke(null, state, (Runnable) () -> {
                    try {
                        prepare.invoke(null, state, lift, new org.mtr.mapping.holder.ClientWorld(client.level));
                    } catch (ReflectiveOperationException failure) {
                        throw new RuntimeException(failure);
                    }
                });
                final var commands = state.getClass().getDeclaredField("commands");
                commands.setAccessible(true);
                check(((List<?>) commands.get(state)).size() > 1, "Lift entity did not receive cabin geometry");

                // Reference an actual OBJ already loaded by the resource-pack model importer.
                final var cache = org.mtr.mod.client.CustomResourceLoader.class.getDeclaredField("LIFTS_CACHE");
                cache.setAccessible(true);
                @SuppressWarnings("unchecked")
                final var styles = (java.util.Map<String, org.mtr.mod.resource.LiftResource>) cache.get(null);
                final String style = "qa_imported_lift";
                styles.put(style, new org.mtr.mod.resource.LiftResource(new org.mtr.core.serializer.JsonReader(
                        org.mtr.core.tool.Utilities.parseJson("{\"id\":\"qa_imported_lift\",\"objectModelId\":\"teapot\"}"))));
                try {
                    entity.configureLift(style, false, 0);
                    renderer.extractRenderState(entity, state, 1);
                    capture.invoke(null, state, (Runnable) () -> {
                        try {
                            prepare.invoke(null, state, null, new org.mtr.mapping.holder.ClientWorld(client.level));
                        } catch (ReflectiveOperationException failure) {
                            throw new RuntimeException(failure);
                        }
                    });
                    var imported = (List<?>) commands.get(state);
                    check(!imported.isEmpty() && imported.stream().noneMatch(command -> command.toString().contains("lift_1.png")),
                            "Imported OBJ did not supply entity mesh commands");
                } finally {
                    styles.remove(style);
                }
            } catch (ReflectiveOperationException failure) {
                throw new RuntimeException(failure);
            }
        });
    }

    private static void verifyYteFloorEditor(ClientGameTestContext test,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        var pos=new BlockPos(3,110,0);
        world.getServer().runOnServer(game->{
            var level=world.getConnection().getServerPlayer().level();
            level.setBlockAndUpdate(pos,BuiltInRegistries.BLOCK.getValue(Identifier.parse("yte:lift_track_empty_floor")).defaultBlockState());
        });
        world.getConnection().waitForClientboundPackets();
        test.waitFor(client -> client.level.getBlockEntity(pos) != null);
        test.setScreen(()->{
            try {
                var client=net.minecraft.client.Minecraft.getInstance();
                var entity=client.level.getBlockEntity(pos);
                var type=Class.forName("top.xfunny.mod.client.screen.LiftEmptyFloorScreen");
                return (net.minecraft.client.gui.screens.Screen)type.getConstructor(org.mtr.mapping.holder.BlockPos.class,entity.getClass()).newInstance(new org.mtr.mapping.holder.BlockPos(pos),entity);
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        });
        test.runOnClient(client->{
            var edits=client.gui.screen().children().stream().filter(child->child instanceof net.minecraft.client.gui.components.EditBox).map(child->(net.minecraft.client.gui.components.EditBox)child).toList();
            check(edits.size()==2,"Missing lift floor editor fields");
            edits.get(0).setValue("QA26");edits.get(1).setValue("26.3 Fabric floor");
        });
        // Hit the real SDL mouse path, including the mapping wrapper used by addon text fields.
        final var field = test.computeOnClient(client -> client.gui.screen().children().stream()
                .filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                .map(net.minecraft.client.gui.components.EditBox.class::cast).findFirst().orElseThrow());
        final double[] click = test.computeOnClient(client -> new double[]{
                (field.getX() + field.getWidth() / 2.0) * client.getWindow().getWidth() / client.getWindow().getGuiScaledWidth(),
                (field.getY() + field.getHeight() / 2.0) * client.getWindow().getHeight() / client.getWindow().getGuiScaledHeight()
        });
        test.getInput().setCursorPos(click[0], click[1]);
        test.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        test.waitFor(client -> field.isFocused());
        test.takeScreenshot("yte-floor-editor");
        clickButton(test, "gui.done");
        world.getConnection().waitForServerboundPackets();
        world.getServer().runOnServer(game->{
            var entity=world.getConnection().getServerPlayer().level().getBlockEntity(pos);
            try {check(entity.getClass().getMethod("getFloorNumber").invoke(entity).equals("QA26"),"Lift floor packet was not applied");}
            catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        });
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
