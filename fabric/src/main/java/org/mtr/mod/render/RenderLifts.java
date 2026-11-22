package org.mtr.mod.render;

import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftDirection;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.data.Position;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.Init;
import org.mtr.mod.Items;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.item.ItemLiftRefresher;
import org.mtr.mod.model.ModelSmallCube;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.data.IGui;
import org.mtr.mod.model.ModelLift1;
import org.mtr.mod.resource.LiftResource;

import javax.annotation.Nullable;

public class RenderLifts implements IGui {

	private static final int LIFT_DISPLAY_COLOR = 0xFFFF0000;
	private static final ModelSmallCube FLOOR_MARKER = new ModelSmallCube(new Identifier("textures/block/redstone_block.png"));
	private static final float LIFT_DOOR_VALUE = 0.75F;
	private static final float LIFT_FLOOR_PADDING = 0.25F;

	static void renderTrackDiagnostics() {
		final var client = MinecraftClient.getInstance();
		final var player = client.getPlayerMapped();
		final var level = client.getWorldMapped();
		if (player == null || level == null || !player.isHolding(Items.LIFT_REFRESHER.get())) return;
		final var world = new World(level.data);
		MinecraftClientData.getInstance().liftWrapperList.values().forEach(wrapper -> {
			final Position[] previous = {null};
			wrapper.getLift().iterateFloors(floor -> {
				final Position current = floor.getPosition();
				FLOOR_MARKER.render(new StoredMatrixTransformations(current.getX(), current.getY(), current.getZ()),
						GraphicsHolder.getDefaultLight());
				if (previous[0] != null) {
					final var path = ItemLiftRefresher.findPath(world, previous[0], current);
					for (int index = 1; index < path.size(); index++) {
						final Vector from = path.get(index - 1);
						final Vector to = path.get(index);
						RenderLiftLinks.scheduleSegmentedLine(from.x + .5, from.y, from.z + .5,
								to.x + .5, to.y, to.z + .5, ARGB_WHITE);
					}
				}
				previous[0] = current;
			});
		});
	}

	static void prepareEntity(TransportEntityRenderer.State state, @Nullable Lift lift, ClientWorld world) {
		final PositionAndRotation pose = state.pose;
		final var matrices = RenderVehicles.getStoredMatrixTransformations(true, pose, 0);
		final float door = Math.min(state.liftDoor, LIFT_DOOR_VALUE);
		final boolean front = door > 0 && RenderVehicleHelper.canOpenDoors(
				new Box(-LIFT_DOOR_VALUE, 0, -state.cabinDepth / 2 + LIFT_FLOOR_PADDING, LIFT_DOOR_VALUE, 0, -state.cabinDepth / 2), pose, door, state.cabinHeight);
		final boolean back = door > 0 && state.liftDoubleSided && RenderVehicleHelper.canOpenDoors(
				new Box(-LIFT_DOOR_VALUE, 0, state.cabinDepth / 2 - LIFT_FLOOR_PADDING, LIFT_DOOR_VALUE, 0, state.cabinDepth / 2), pose, door, state.cabinHeight);
		final LiftResource resource = getLiftResource(state.liftStyle);
		final boolean[] imported = {false};
		if (!resource.getObjectModelId().isEmpty()) {
			CustomResourceLoader.getObjectById(resource.getObjectModelId(), object -> {
				final var mesh = object.getDynamicVehicleModel();
				if (mesh != null) {
					mesh.render(matrices, null, 0, new int[]{0}, pose.light, new ObjectArrayList<>(), false);
					imported[0] = true;
				}
			});
		}
		if (!imported[0]) new ModelLift1((int) Math.round(state.cabinHeight * 2),
				(int) Math.round(state.cabinWidth), (int) Math.round(state.cabinDepth), state.liftDoubleSided).render(matrices, null, resource.getTexture(), pose.light,
				front ? door / LIFT_DOOR_VALUE : 0, back ? door / LIFT_DOOR_VALUE : 0,
				false, 0, 1, true, true, false, true, false);
		for (int side = 0; lift != null && side < (state.liftDoubleSided ? 2 : 1); side++) {
			final boolean rotate = side == 0;
			final var display = matrices.copy();
			display.add(graphics -> {
				if (rotate) graphics.rotateYDegrees(180);
				graphics.translate(0.875F, -1.5, state.cabinDepth / 2 - 0.25 - SMALL_OFFSET);
			});
			renderLiftDisplay(display, new World(world.data), lift, 0.1875F, 0.3125F);
		}
	}

	public static void renderLiftDisplay(StoredMatrixTransformations storedMatrixTransformations, World world, Lift lift, float width, float height) {
		final ObjectObjectImmutablePair<LiftDirection, ObjectObjectImmutablePair<String, String>> liftDetails = getLiftDetails(world, lift, Init.positionToBlockPos(lift.getCurrentFloor().getPosition()));
		final LiftDirection liftDirection = liftDetails.left();

		MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
			storedMatrixTransformations.transform(graphicsHolder, offset);
			IDrawing.drawStringWithFont(graphicsHolder, liftDetails.right().left(), IGui.HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM, 0, height, width, -1, 18 / width, LIFT_DISPLAY_COLOR, false, GraphicsHolder.getDefaultLight(), null);
			graphicsHolder.pop();
		});

		if (liftDirection != LiftDirection.NONE) {
			MainRenderer.scheduleRender(new Identifier(Init.MOD_ID, "textures/block/sign/lift_arrow.png"), false, QueuedRenderLayer.LIGHT_TRANSLUCENT, (graphicsHolder, offset) -> {
				storedMatrixTransformations.transform(graphicsHolder, offset);
				IDrawing.drawTexture(graphicsHolder, -width / 6, 0, width / 3, width / 3, 0, liftDirection == LiftDirection.UP ? 0 : 1, 1, liftDirection == LiftDirection.UP ? 1 : 0, Direction.UP, LIFT_DISPLAY_COLOR, GraphicsHolder.getDefaultLight());
				graphicsHolder.pop();
			});
		}
	}

	public static ObjectObjectImmutablePair<LiftDirection, ObjectObjectImmutablePair<String, String>> getLiftDetails(World world, Lift lift, BlockPos blockPos) {
		final LiftFloor liftFloor = lift.getCurrentFloor();
		final BlockEntity floorEntity = world.getBlockEntity(blockPos);
		final String floorNumber;
		final String floorDescription;

		if (floorEntity != null && floorEntity.data instanceof BlockLiftTrackFloor.BlockEntity) {
			floorNumber = ((BlockLiftTrackFloor.BlockEntity) floorEntity.data).getFloorNumber();
			floorDescription = ((BlockLiftTrackFloor.BlockEntity) floorEntity.data).getFloorDescription();
		} else {
			floorNumber = liftFloor.getNumber();
			floorDescription = liftFloor.getDescription();
		}

		return new ObjectObjectImmutablePair<>(lift.getDirection(), new ObjectObjectImmutablePair<>(floorNumber, floorDescription));
	}

	public static LiftResource getLiftResource(@Nullable String liftId) {
		final LiftResource liftResource;

		if (liftId == null) {
			liftResource = null;
		} else {
			final LiftResource[] tempLiftResource = {null};
			CustomResourceLoader.getLiftById(liftId, newLiftResource -> tempLiftResource[0] = newLiftResource);
			liftResource = tempLiftResource[0];
		}

		return liftResource == null ? CustomResourceLoader.getLifts().get(0) : liftResource;
	}

}
