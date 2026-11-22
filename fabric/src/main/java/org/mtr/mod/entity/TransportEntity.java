package org.mtr.mod.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.mtr.core.data.VehicleRidingEntity;
import org.mtr.core.operation.UpdateVehicleRidingEntities;
import org.mtr.core.servlet.OperationProcessor;
import org.mtr.mapping.holder.ItemStack;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;
import org.mtr.mod.item.ItemDriverKey;
import org.mtr.mod.item.ItemDepotDriverKey;
import org.mtr.mod.packet.PacketTrainControls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class TransportEntity extends Entity {
	private static final EntityDataAccessor<Long> TRANSPORT_ID = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> CAR_INDEX = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Long> DEPOT_ID = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> MANUAL = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> BOARDABLE = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<String> LIFT_STYLE = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> LIFT_DOUBLE_SIDED = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> LIFT_DOOR = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.FLOAT);

	public void configureLift(String style, boolean doubleSided, float door) {
		entityData.set(LIFT_STYLE, style);
		entityData.set(LIFT_DOUBLE_SIDED, doubleSided);
		entityData.set(LIFT_DOOR, door);
	}

	public float cabinWidth() { return entityData.get(WIDTH); }
	public float cabinLength() { return entityData.get(LENGTH); }
	public float cabinHeight() { return entityData.get(HEIGHT); }
	public String liftStyle() { return entityData.get(LIFT_STYLE); }
	public boolean liftDoubleSided() { return entityData.get(LIFT_DOUBLE_SIDED); }
	public float liftDoor() { return entityData.get(LIFT_DOOR); }

	private static final EntityDataAccessor<String> RIDER_OFFSETS = SynchedEntityData.defineId(TransportEntity.class, EntityDataSerializers.STRING);

	public static final EntityType<TransportEntity> TYPE = register();

	private final Map<UUID, Vec3> riderOffsets = new HashMap<>();
	private final HashSet<UUID> publishedDrivers = new HashSet<>();
	private long sidingId;
	private static final double RIDER_MARGIN = 0.35;
	private static final double FLOOR_HEIGHT = 0.15;

	void configureTrain(long depotId, long sidingId, boolean manual) {
		this.sidingId = sidingId;
		entityData.set(DEPOT_ID, depotId);
		entityData.set(MANUAL, manual);
	}
	

	public TransportEntity(EntityType<? extends TransportEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	private static EntityType<TransportEntity> register() {
		final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("mtr", "transport"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.<TransportEntity>of(TransportEntity::new, MobCategory.MISC)
						.sized(1, 1).noSummon().clientTrackingRange(12).updateInterval(1).build(key));
	}

	public static void init() {
		// Force registration during mod initialization.
	}

	public void bind(long transportId, int carIndex) {
		entityData.set(TRANSPORT_ID, transportId);
		entityData.set(CAR_INDEX, carIndex);
	}

	public long transportId() {
		return entityData.get(TRANSPORT_ID);
	}

	public int carIndex() {
		return entityData.get(CAR_INDEX);
	}

	public void configure(double width, double length, double height, boolean boardable) {
		entityData.set(WIDTH, (float) width);
		entityData.set(LENGTH, (float) length);
		entityData.set(HEIGHT, (float) height);
		entityData.set(BOARDABLE, boardable);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (accessor == RIDER_OFFSETS && level().isClientSide()) {
			riderOffsets.clear();
			final String offsets = entityData.get(RIDER_OFFSETS);
			if (!offsets.isEmpty()) {
				for (final String entry : offsets.split(";")) {
					final String[] fields = entry.split(",");
					riderOffsets.put(UUID.fromString(fields[0]), new Vec3(Double.parseDouble(fields[1]), 0.15, Double.parseDouble(fields[2])));
				}
			}
		}
		if (accessor == WIDTH || accessor == LENGTH || accessor == HEIGHT) {
			reapplyPosition();
		}
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		final double yaw = Math.toRadians(getYRot());
		final double width = cabinWidth() / 2;
		final double depth = cabinLength() / 2;
		final double halfX = Math.abs(Math.cos(yaw)) * width + Math.abs(Math.sin(yaw)) * depth;
		final double halfZ = Math.abs(Math.sin(yaw)) * width + Math.abs(Math.cos(yaw)) * depth;
		return new AABB(position.x - halfX, position.y, position.z - halfZ,
				position.x + halfX, position.y + cabinHeight(), position.z + halfZ);
	}

	private boolean collidesWith(Entity mover) {
		return mover == null || !mover.isSpectator() && !(mover instanceof TransportEntity)
				&& !isPassengerOfSameVehicle(mover);
	}

	@Override
	public boolean canBeCollidedWith(Entity mover) {
		return !isRemoved() && !hasWalkThroughDoors() && collidesWith(mover);
	}

	private boolean hasWalkThroughDoors() {
		// The open-door box layout uses Minecraft's axis-aligned collision primitives.
		return entityData.get(BOARDABLE) && Math.abs(getXRot()) < 0.5
				&& Math.abs(getYRot() - Math.round(getYRot() / 90F) * 90F) < 0.5;
	}

	/** Stationary open cabins have a floor, roof and only the walls without an open side. */
	public void addOpenCabinCollisions(Entity mover, AABB area, List<VoxelShape> collisions) {
		if (!hasWalkThroughDoors() || !collidesWith(mover)) return;
		final AABB box = getBoundingBox();
		final double thin = 0.125;
		addCollision(area, collisions, new AABB(box.minX, box.minY - FLOOR_HEIGHT, box.minZ, box.maxX, box.minY, box.maxZ));
		addCollision(area, collisions, new AABB(box.minX, box.maxY - thin, box.minZ, box.maxX, box.maxY, box.maxZ));
		final boolean sideAlongX = (Math.round(getYRot() / 90F) & 1) == 0;
		// Train doors are on the long sides; lift doors are on the short ends.
		if ((carIndex() < 0) == sideAlongX) {
			addCollision(area, collisions, new AABB(box.minX, box.minY, box.minZ, box.minX + thin, box.maxY, box.maxZ));
			addCollision(area, collisions, new AABB(box.maxX - thin, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
		} else {
			addCollision(area, collisions, new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ + thin));
			addCollision(area, collisions, new AABB(box.minX, box.minY, box.maxZ - thin, box.maxX, box.maxY, box.maxZ));
		}

		if (carIndex() < 0 && !liftDoubleSided()) {
			// ModelLift1's primary door is at negative local Z.
			final Vec3 rear = new Vec3(0, 0, 1).yRot((float) Math.toRadians(-getYRot()));
			if (sideAlongX) {
				final double z = rear.z >= 0 ? box.maxZ - thin : box.minZ;
				addCollision(area, collisions, new AABB(box.minX, box.minY, z, box.maxX, box.maxY, z + thin));
			} else {
				final double x = rear.x >= 0 ? box.maxX - thin : box.minX;
				addCollision(area, collisions, new AABB(x, box.minY, box.minZ, x + thin, box.maxY, box.maxZ));
			}
		}
	}

	private static void addCollision(AABB area, List<VoxelShape> collisions, AABB surface) {
		if (surface.intersects(area)) collisions.add(Shapes.create(surface));
	}

	public boolean opensLandingDoor(BlockPos door) {
		if (!hasWalkThroughDoors() || door.getY() + 1 <= getY() || door.getY() >= getY() + cabinHeight()) return false;
		final Vec3 local = localPosition(Vec3.atCenterOf(door));
		final boolean lift = carIndex() < 0;
		final double across = lift ? local.z : local.x;
		final double along = lift ? local.x : local.z;
		final double edge = (lift ? cabinLength() : cabinWidth()) / 2;
		final double halfSpan = (lift ? cabinWidth() : cabinLength()) / 2;
		return Math.abs(Math.abs(across) - edge) <= 1
				&& Math.abs(along) <= halfSpan + .5
				&& (!lift || liftDoubleSided() || across < 0);
	}

	private Vec3 localPosition(Vec3 world) {
		return world.subtract(position()).yRot((float) Math.toRadians(getYRot()));
	}

	private void boardPlayersInside() {
		if (!hasWalkThroughDoors()) return;
		for (final Player player : level().getEntitiesOfClass(Player.class, getBoundingBox())) {
			if (player.isSpectator() || player.isPassenger()) continue;
			final Vec3 local = localPosition(player.position());
			final double margin = player.getBbWidth() / 2;
			if (Math.abs(local.x) <= cabinWidth() / 2 - margin
					&& Math.abs(local.z) <= cabinLength() / 2 - margin
					&& local.y >= -.05 && local.y <= .55) {
				board(player, local);
			}
		}
	}

	private boolean board(Player player, Vec3 local) {
		final UUID id = player.getUUID();
		final Vec3 previous = riderOffsets.put(id, clampOffset(local));
		if (player.startRiding(this, false, true)) return true;
		if (previous == null) riderOffsets.remove(id);
		else riderOffsets.put(id, previous);
		return false;
	}

	@Override
	public boolean isPickable() {
		return entityData.get(BOARDABLE) || carIndex() >= 0 && (entityData.get(MANUAL) || entityData.get(DEPOT_ID) != 0);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 interactionPoint) {
		if (!mayBoard(player) || player.isSpectator() || player.isPassenger() || !isNearCabin(player)) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return board(player, localPosition(player.position())) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return passenger instanceof Player player && mayBoard(player) && getPassengers().size() < 64;
	}

	private boolean mayBoard(Player player) {
		if (entityData.get(BOARDABLE)) return true;
		if (carIndex() < 0) return false;
		final ItemDriverKey key = driverKey(player);
		return key != null && (key.canBoardAnyVehicle || entityData.get(MANUAL));
	}

	private ItemDriverKey driverKey(Player player) {
		for (var stack : java.util.List.of(player.getMainHandItem(), player.getOffhandItem())) {
			if (stack.getItem() instanceof ItemDriverKey key) {
				return ItemDepotDriverKey.isCreativeDriverKeyOrMatchesDepot(new ItemStack(stack), entityData.get(DEPOT_ID)) ? key : null;
			}
		}
		return null;
	}

	public void applyDriverInput(ServerPlayer player, int pressed) {
		if (carIndex() < 0 || sidingId == 0 || player.getVehicle() != this) return;
		final ItemDriverKey key = driverKey(player);
		if (key == null) {
			if (publishedDrivers.remove(player.getUUID())) publishDriver(player, false, null, 0);
		} else if (publishedDrivers.add(player.getUUID()) || pressed != 0) {
			publishDriver(player, true, key, pressed);
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof ServerPlayer player && publishedDrivers.remove(player.getUUID())) {
			publishDriver(player, false, null, 0);
		}
	}

	private void publishDriver(ServerPlayer player, boolean mounted, ItemDriverKey key, int pressed) {
		if (!(level() instanceof ServerLevel level)) return;
		final var update = new UpdateVehicleRidingEntities(sidingId, transportId());
		final Vec3 offset = riderOffsets.getOrDefault(player.getUUID(), Vec3.ZERO);
		final boolean canDrive = key != null && key.canDrive;
		final boolean toggle = (pressed & PacketTrainControls.TOGGLE_DOORS) != 0;
		update.add(new VehicleRidingEntity(player.getUUID(), mounted ? carIndex() : -1,
				offset.x, offset.y, offset.z, false, mounted,
				canDrive && (pressed & PacketTrainControls.ACCELERATE) != 0,
				canDrive && (pressed & PacketTrainControls.BRAKE) != 0,
				key != null && key.canOpenDoors && toggle, canDrive && toggle, false));
		Init.sendMessageC2S(OperationProcessor.UPDATE_RIDING_ENTITIES, new MinecraftServer(level.getServer()), new World(level), update, null, null);
	}

	private boolean isNearCabin(Player player) {
		final Vec3 local = player.position().subtract(position()).yRot((float) Math.toRadians(getYRot()));
		return Math.abs(local.x) <= entityData.get(WIDTH) / 2 + 1
				&& Math.abs(local.z) <= entityData.get(LENGTH) / 2 + 1
				&& Math.abs(local.y) <= 1.5;
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		final Vec3 offset = riderOffsets.getOrDefault(passenger.getUUID(), new Vec3(0, 0.15, 0));
		return position().add(offset.xRot((float) Math.toRadians(getXRot())).yRot((float) Math.toRadians(-getYRot())));
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction move) {
		final Vec3 feet = getPassengerRidingPosition(passenger);
		move.accept(passenger, feet.x, feet.y, feet.z);
	}

	private void updateRiderOffsets() {
		riderOffsets.keySet().removeIf(id -> getPassengers().stream().noneMatch(rider -> rider.getUUID().equals(id)));
		final StringBuilder data = new StringBuilder();
		for (final Entity passenger : getPassengers()) {
			if (!(passenger instanceof ServerPlayer player)) {
				continue;
			}
			applyDriverInput(player, 0);
			final var input = player.getLastClientInput();
			final Vec3 initial = player.position().subtract(position()).yRot((float) Math.toRadians(getYRot()));
			final Vec3 previous = riderOffsets.getOrDefault(player.getUUID(), initial);
			final double speed = input.sprint() ? 0.15 : 0.10;
			final Vec3 movement = new Vec3((input.left() ? 1 : 0) - (input.right() ? 1 : 0), 0,
					(input.forward() ? 1 : 0) - (input.backward() ? 1 : 0))
					.normalize().scale(speed).yRot((float) Math.toRadians(getYRot() - player.getYRot()));
			final Vec3 next = previous.add(movement);
			if (hasWalkThroughDoors() && tryWalkOut(player, next)) continue;
			final Vec3 offset = clampOffset(next);
			riderOffsets.put(player.getUUID(), offset);
			data.append(player.getUUID()).append(',').append(offset.x).append(',').append(offset.z).append(';');
		}
		entityData.set(RIDER_OFFSETS, data.toString());
	}

	private boolean tryWalkOut(ServerPlayer player, Vec3 next) {
		final boolean lift = carIndex() < 0;
		final double limit = (lift ? cabinLength() : cabinWidth()) / 2;
		final double across = lift ? next.z : next.x;
		if (Math.abs(across) <= Math.max(0, limit - RIDER_MARGIN)) return false;
		if (lift && across > 0 && !liftDoubleSided()) return false;

		final double doorSide = Math.copySign(limit + player.getBbWidth() / 2 + .08, across);
		final Vec3 localExit = lift ? new Vec3(next.x, FLOOR_HEIGHT, doorSide) : new Vec3(doorSide, FLOOR_HEIGHT, next.z);
		final Vec3 exit = position().add(localExit.yRot((float) Math.toRadians(-getYRot())));
		if (!level().noCollision(player, player.getBoundingBox().move(exit.subtract(player.position())))) return false;
		player.stopRiding();
		player.teleportTo(exit.x, exit.y, exit.z);
		return true;
	}

	private Vec3 clampOffset(Vec3 localPosition) {
		final double halfWidth = Math.max(0, entityData.get(WIDTH) / 2 - RIDER_MARGIN);
		final double halfLength = Math.max(0, entityData.get(LENGTH) / 2 - RIDER_MARGIN);
		return new Vec3(Mth.clamp(localPosition.x, -halfWidth, halfWidth), FLOOR_HEIGHT, Mth.clamp(localPosition.z, -halfLength, halfLength));
	}

	public void updatePose(TransportPose pose) {
		setYRot((float) -Math.toDegrees(pose.yaw()));
		setXRot((float) Math.toDegrees(pose.pitch()));
		setPos(pose.position().x, pose.position().y, pose.position().z);
		reapplyPosition();
	}

	@Override
	protected InterpolationHandler createInterpolationHandler() {
		return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) reapplyPosition();
		if (!level().isClientSide()) {
			boardPlayersInside();
			updateRiderOffsets();
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(TRANSPORT_ID, 0L).define(CAR_INDEX, -1)
				.define(WIDTH, 1F).define(LENGTH, 1F).define(HEIGHT, 1F).define(BOARDABLE, false).define(RIDER_OFFSETS, "")
				.define(DEPOT_ID, 0L).define(MANUAL, false)
				.define(LIFT_STYLE, "").define(LIFT_DOUBLE_SIDED, false).define(LIFT_DOOR, 0F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	// Minecraft requires a serializable entity type for startRiding. The simulator
	// recreates the actual instances, so neither chunk saves nor player saves store them.
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean save(ValueOutput output) {
		return false;
	}

	@Override
	public boolean saveAsPassenger(ValueOutput output) {
		return false;
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		final AABB box = getBoundingBox();
		final double margin = passenger.getBbWidth() / 2 + 0.05;
		final Vec3[] exits = {
				new Vec3(box.minX - margin, getY(), getZ()), new Vec3(box.maxX + margin, getY(), getZ()),
				new Vec3(getX(), getY(), box.minZ - margin), new Vec3(getX(), getY(), box.maxZ + margin)
		};
		Vec3 closest = null;
		for (final Vec3 exit : exits) {
			if (level().noCollision(passenger, passenger.getBoundingBox().move(exit.subtract(passenger.position())))
					&& (closest == null || exit.distanceToSqr(passenger.position()) < closest.distanceToSqr(passenger.position()))) {
				closest = exit;
			}
		}
		return closest == null ? new Vec3(getX(), box.maxY, getZ()) : closest;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
