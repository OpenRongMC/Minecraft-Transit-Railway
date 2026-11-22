package org.mtr.mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.mtr.core.Main;
import org.mtr.core.data.Lift;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.World;
import org.mtr.mixin.SimulationMainAccessor;
import org.mtr.mixin.SimulationSidingAccessor;
import org.mtr.mod.item.ItemLiftRefresher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class TransportEntities {
	private final Main main;
	private final Map<ServerLevel, Map<Key, TransportEntity>> entities = new HashMap<>();
	private final Map<String, Snapshot> snapshots = new HashMap<>();
	private final HashSet<String> pending = new HashSet<>();

	public TransportEntities(Main main) {
		this.main = main;
	}

	public void tick(ServerLevel level, String dimension) {
		if (pending.add(dimension)) {
			for (final Simulator simulator : ((SimulationMainAccessor) (Object) main).mtr$getSimulators()) {
				if (simulator.dimension.equals(dimension)) {
					simulator.run(() -> {
						try {
							final Snapshot snapshot = capture(simulator);
							level.getServer().execute(() -> snapshots.put(dimension, snapshot));
						} finally {
							level.getServer().execute(() -> pending.remove(dimension));
						}
					});
					break;
				}
			}
		}

		final Snapshot snapshot = snapshots.get(dimension);
		if (snapshot == null) {
			return;
		}
		final Map<Key, TransportEntity> active = entities.computeIfAbsent(level, ignored -> new HashMap<>());
		final HashSet<Key> seen = new HashSet<>();
		for (final Car car : snapshot.cars) {
			update(level, active, seen, car.key, car.pose, car.width, car.length, 3, car.boardable, car.depotId, car.sidingId, car.manual);
		}
		final World world = new World(level);
		for (final LiftCabin cabin : snapshot.lifts) {
			final Lift lift = cabin.lift;
			final Vector position;
			if (lift.getFloorCount() == 1) {
				final Vector[] floorPosition = new Vector[1];
				lift.iterateFloors(floor -> {
					final var point = floor.getPosition();
					floorPosition[0] = new Vector(point.getX(), point.getY(), point.getZ());
				});
				position = floorPosition[0];
			} else {
				position = lift.getPosition((from, to) -> ItemLiftRefresher.findPath(world, from, to));
			}
			final TransportPose pose = new TransportPose(position.add(lift.getOffsetX(), lift.getOffsetY(), lift.getOffsetZ()), -Math.PI / 2 - lift.getAngle().angleRadians, 0);
			final Key key = new Key(lift.getId(), -1);
			update(level, active, seen, key, pose, lift.getWidth(), lift.getDepth(), lift.getHeight(), cabin.boardable, 0, 0, false);
			final TransportEntity entity = active.get(key);
			if (entity != null) entity.configureLift(lift.getStyle(), lift.getIsDoubleSided(), lift.hasCoolDown() ? lift.getDoorValue() : 0);
		}
		active.entrySet().removeIf(entry -> {
			if (seen.contains(entry.getKey()) && !entry.getValue().isRemoved()) {
				return false;
			}
			entry.getValue().discard();
			return true;
		});
	}

	private static Snapshot capture(Simulator simulator) {
		final List<Car> cars = new ArrayList<>();
		simulator.sidings.forEach(siding -> {
			for (final var train : ((SimulationSidingAccessor) (Object) siding).mtr$getVehicles()) {
				if (!train.isValid()) {
					continue;
				}
				final var carPositions = train.getVehicleCarsAndPositions();
				for (int index = 0; index < carPositions.size(); index++) {
					final var pair = carPositions.get(index);
					final var car = pair.left();
					final var mode = train.getTransportMode();
					final var pose = TransportPose.forCar(car, pair.right(), mode.hasPitchAscending || mode.hasPitchDescending);
					if (pose != null) {
						cars.add(new Car(new Key(train.getId(), index), pose, car.getWidth(), car.getLength(),
								!train.isMoving() && train.vehicleExtraData.getDoorMultiplier() > 0,
								train.vehicleExtraData.getDepotId(), siding.getId(), train.vehicleExtraData.getIsManualAllowed()));
					}
				}
			}
		});
		final List<LiftCabin> lifts = new ArrayList<>();
		for (final Lift lift : simulator.lifts) {
			if (lift.isValid() && lift.getFloorCount() > 0) {
				lifts.add(new LiftCabin(new Lift(new JsonReader(Utilities.getJsonObjectFromData(lift)), simulator), lift.hasCoolDown() && lift.getDoorValue() > 0));
			}
		}
		return new Snapshot(cars, lifts);
	}

	private static void update(ServerLevel level, Map<Key, TransportEntity> active, HashSet<Key> seen, Key key, TransportPose pose, double width, double length, double height, boolean boardable, long depotId, long sidingId, boolean manual) {
		final Vector position = pose.position();
		if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
				|| !level.hasChunkAt(BlockPos.containing(position.x, position.y, position.z))) {
			return;
		}
		seen.add(key);
		TransportEntity entity = active.get(key);
		if (entity == null || entity.isRemoved()) {
			entity = new TransportEntity(TransportEntity.TYPE, level);
			entity.bind(key.id, key.car);
			entity.configure(width, length, height, boardable);
			entity.configureTrain(depotId, sidingId, manual);
			entity.updatePose(pose);
			entity.setOldPosAndRot();
			if (!level.addFreshEntity(entity)) {
				return;
			}
			active.put(key, entity);
		} else {
			entity.configure(width, length, height, boardable);
			entity.configureTrain(depotId, sidingId, manual);
			entity.updatePose(pose);
		}
	}

	private record Key(long id, int car) {}
	private record Car(Key key, TransportPose pose, double width, double length, boolean boardable, long depotId, long sidingId, boolean manual) {}
	private record LiftCabin(Lift lift, boolean boardable) {}
	private record Snapshot(List<Car> cars, List<LiftCabin> lifts) {}
}
