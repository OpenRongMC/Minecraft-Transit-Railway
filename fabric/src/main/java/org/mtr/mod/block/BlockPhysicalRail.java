package org.mtr.mod.block;

import org.mtr.core.data.Rail;
import org.mtr.core.data.TransportMode;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.tool.Utilities;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.BlockEntityTypes;
import org.mtr.mod.Init;
import org.mtr.mod.packet.PacketDeleteData;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import org.mtr.mod.data.RailBlockGeometry;

import java.util.ArrayList;
import java.util.List;

public final class BlockPhysicalRail extends BlockWaterloggable implements BlockWithEntity {
	public static final BooleanProperty EAST_WEST = BooleanProperty.of("east_west");

	public BlockPhysicalRail() {
		super(org.mtr.mod.Blocks.createDefaultBlockSettings(true).nonOpaque().dynamicShape());
	}

	@Override
	public void addBlockProperties(List<HolderBase<?>> properties) {
		super.addBlockProperties(properties);
		properties.add(EAST_WEST);
	}

	@Override
	public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shape(world, pos);
	}

	@Override
	public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shape(world, pos);
	}

	private static VoxelShape shape(BlockView world, BlockPos pos) {
		final org.mtr.mapping.holder.BlockEntity entity = world.getBlockEntity(pos);
		return entity != null && entity.data instanceof Data data ? data.shape : VoxelShapes.empty();
	}

	@Override
	public BlockEntityExtension createBlockEntity(BlockPos position, BlockState state) {
		return new Data(position, state);
	}

	@Override
	public void onBreak2(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient()) {
			final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity != null && blockEntity.data instanceof Data data && !data.owner.isEmpty()) {
				PacketDeleteData.sendDirectlyToServerRailId(ServerWorld.cast(world), data.owner);
			}
		}
		super.onBreak2(world, pos, state, player);
	}

	public static final class Node extends BlockNode implements BlockWithEntity {
		public Node() {
			super(TransportMode.TRAIN, org.mtr.mod.Blocks.createDefaultBlockSettings(true).nonOpaque().dynamicShape());
		}

		@Override
		public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
			return shape(world, pos);
		}

		@Override
		public BlockEntityExtension createBlockEntity(BlockPos position, BlockState state) {
			return Data.forNode(position, state);
		}
	}

	public static final class Data extends BlockEntityExtension implements RenderDataBlockEntity {
		private volatile RailBlockGeometry.Geometry geometry = new RailBlockGeometry.Geometry(List.of());
		private String encodedGeometry = "";
		private VoxelShape shape = VoxelShapes.empty();

		@Override
		public Object getRenderData() {
			return geometry;
		}

		public RailBlockGeometry.Geometry geometry() {
			return geometry;
		}

		private void setGeometry(String encoded) {
			if (!encodedGeometry.equals(encoded)) {
				geometry = RailBlockGeometry.decode(encoded);
				encodedGeometry = encoded;
				shape = geometry.collision();
			}
		}

		private String owner = "";
		private String encodedCurves = "";
		private List<Rail> curves = List.of();

		public Data(BlockPos position, BlockState state) {
			this(BlockEntityTypes.PHYSICAL_RAIL.get(), position, state);
		}

		private Data(BlockEntityType<?> type, BlockPos position, BlockState state) {
			super(type, position, state);
		}

		public static Data forNode(BlockPos position, BlockState state) {
			return new Data(BlockEntityTypes.PHYSICAL_RAIL_NODE.get(), position, state);
		}

		public String owner() {
			return owner;
		}

		public List<Rail> curves() {
			return curves;
		}

		public void update(String newOwner, String encoded, String mesh) {
			if (!owner.equals(newOwner) || !encodedCurves.equals(encoded) || !encodedGeometry.equals(mesh)) {
				owner = newOwner;
				setCurves(encoded);
				setGeometry(mesh);
				markDirty2();
				if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
					serverLevel.getChunkSource().blockChanged(getBlockPos());
				}
			}
		}

		@Override
		public void readCompoundTag(CompoundTag tag) {
			owner = tag.getString("rail_owner");
			setCurves(tag.getString("rail_curves"));
			setGeometry(tag.getString("rail_geometry"));
			if (level != null && level.isClientSide()) {
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 8);
			}
		}

		@Override
		public void writeCompoundTag(CompoundTag tag) {
			tag.putString("rail_owner", owner);
			tag.putString("rail_curves", encodedCurves);
			tag.putString("rail_geometry", encodedGeometry);
		}

		private void setCurves(String encoded) {
			encodedCurves = encoded;
			if (encoded.isEmpty()) {
				curves = List.of();
				return;
			}
			final List<Rail> decoded = new ArrayList<>();
			for (final String line : encoded.split("\n")) {
				try {
					decoded.add(new Rail(JsonReader.parse(line)));
				} catch (RuntimeException exception) {
					Init.LOGGER.warn("Could not read physical track at {}", getPos2(), exception);
				}
			}
			curves = List.copyOf(decoded);
		}
	}

	public static String serialize(Rail rail) {
		return Utilities.getJsonObjectFromData(rail).toString();
	}
}
