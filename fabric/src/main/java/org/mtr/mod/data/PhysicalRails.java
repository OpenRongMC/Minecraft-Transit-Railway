package org.mtr.mod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.mtr.core.Main;
import org.mtr.core.data.Position;
import org.mtr.core.data.Rail;
import org.mtr.core.data.TransportMode;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.mapping.mapper.SlabBlockExtension;
import org.mtr.mixin.SimulationMainAccessor;
import org.mtr.mod.Blocks;
import org.mtr.mod.block.BlockPhysicalRail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class PhysicalRails {
	private static final int REFRESH_INTERVAL = 100;
	private static final int BLOCKS_PER_TICK = 96;
	private final Map<String, Dimension> dimensions = new HashMap<>();

	public PhysicalRails(Main main) {
		for (final Simulator simulator : ((SimulationMainAccessor) (Object) main).mtr$getSimulators()) {
			dimensions.put(simulator.dimension, new Dimension(simulator));
		}
	}

	public void chunkLoaded(String dimension, LevelChunk chunk) {
		final Dimension state = dimensions.get(dimension);
		if (state != null) {
			chunk.getBlockEntities().forEach((position, entity) -> {
				if (entity instanceof BlockPhysicalRail.Data) {
					state.pending.add(position);
				}
			});
			state.pending.addAll(state.snapshot.getOrDefault(chunk.getPos().pack(), Map.of()).keySet());
		}
	}

	public void chunkUnloaded(String dimension, ChunkPos chunk) {
		final Dimension state = dimensions.get(dimension);
		if (state != null) {
			state.queuedChunks.remove(chunk.pack());
		}
	}

	public void tick(ServerLevel level, String dimension) {
		final Dimension state = dimensions.get(dimension);
		if (state == null) {
			return;
		}
		if (!state.refreshing && level.getGameTime() >= state.nextRefresh) {
			state.refreshing = true;
			state.nextRefresh = level.getGameTime() + REFRESH_INTERVAL;
			state.simulator.run(() -> {
				final List<String> source = state.simulator.rails.stream()
						.filter(rail -> rail.getTransportMode() == TransportMode.TRAIN)
						.map(BlockPhysicalRail::serialize).sorted().toList();
				final Map<Long, Map<BlockPos, Placement>> snapshot = source.equals(state.source) ? null : capture(state.simulator);
				if (snapshot != null) {
					state.source = source;
				}
				level.getServer().execute(() -> state.update(level, snapshot));
			});
		}
		if (!state.ready) {
			return;
		}
		// Placing a block can load a neighboring chunk and enqueue more work.
		for (int count = 0; count < BLOCKS_PER_TICK && !state.pending.isEmpty(); count++) {
			final BlockPos position = state.pending.removeFirst();
			final long chunkKey = ChunkPos.pack(position);
			if (level.getChunkSource().getChunkNow(ChunkPos.getX(chunkKey), ChunkPos.getZ(chunkKey)) == null) {
				state.queuedChunks.remove(chunkKey);
				continue;
			}
			final Placement placement = state.snapshot.getOrDefault(chunkKey, Map.of()).get(position);
			apply(level, position, placement);
		}
	}

	private static Map<Long, Map<BlockPos, Placement>> capture(Simulator simulator) {
		final Map<BlockPos, Placement> segments = new HashMap<>();
		final Map<BlockPos, List<RailBlockGeometry.Part>> geometry = new HashMap<>();
		final Map<BlockPos, Map<String, String>> nodes = new HashMap<>();
		for (final Rail rail : simulator.rails) {
			if (rail.getTransportMode() != TransportMode.TRAIN || !rail.isValid()) {
				continue;
			}
			final ObjectArraySet<Position> endpoints = new ObjectArraySet<>();
			rail.writePositions(endpoints);
			if (endpoints.size() != 2) {
				continue;
			}
			RailBlockGeometry.append(rail, geometry);
			final String encoded = BlockPhysicalRail.serialize(rail);
			for (final Position endpoint : endpoints) {
				final BlockPos position = new BlockPos((int) endpoint.getX(), (int) endpoint.getY(), (int) endpoint.getZ());
				nodes.computeIfAbsent(position, ignored -> new TreeMap<>()).put(rail.getHexId(), encoded);
			}
		}
		geometry.forEach((position, parts) -> segments.put(position,
				new Placement(parts.getFirst().owner(), false, "", new RailBlockGeometry.Geometry(List.copyOf(parts)).encode())));
		nodes.forEach((position, curves) -> {
			final Placement old = segments.get(position);
			segments.put(position, new Placement(old == null ? "" : old.owner, false,
					String.join("\n", curves.values()), old == null ? "" : old.geometry));
		});
		final Map<Long, Map<BlockPos, Placement>> result = new HashMap<>();
		segments.forEach((position, placement) -> result.computeIfAbsent(ChunkPos.pack(position), ignored -> new HashMap<>()).put(position, placement));
		return result;
	}

	public static boolean isStraight(Rail rail) {
		final ObjectArraySet<Position> endpoints = new ObjectArraySet<>();
		rail.writePositions(endpoints);
		if (endpoints.size() != 2) {
			return false;
		}
		final var iterator = endpoints.iterator();
		return straight(rail, iterator.next(), iterator.next());
	}

	private static boolean straight(Rail rail, Position start, Position end) {
		if (start.getY() != end.getY() || start.getX() != end.getX() && start.getZ() != end.getZ()
				|| rail.getStyles().size() != 1 || !rail.getStyles().get(0).equals("default")) {
			return false;
		}
		final double length = Math.abs(start.getX() - end.getX()) + Math.abs(start.getZ() - end.getZ());
		return length <= 16384 && Math.abs(rail.railMath.getLength() - length) < 1E-4
				&& Math.abs(rail.railMath.getPosition(length / 2, false).y - start.getY()) < 1E-4;
	}

	private static void apply(ServerLevel level, BlockPos position, Placement placement) {
		final var block = level.getBlockState(position);
		final boolean segment = block.getBlock() instanceof BlockPhysicalRail;
		if (block.getBlock() instanceof BlockPhysicalRail.Node) {
			if (level.getBlockEntity(position) instanceof BlockPhysicalRail.Data data) {
				data.update(placement == null ? "" : placement.owner, placement == null ? "" : placement.curves, placement == null ? "" : placement.geometry);
			}
			return;
		}
		if (placement == null || placement.geometry.isEmpty()) {
			if (segment) {
				level.setBlock(position, block.getFluidState().createLegacyBlock(), 3);
			}
			if (block.getBlock() instanceof BlockPhysicalRail.Node && level.getBlockEntity(position) instanceof BlockPhysicalRail.Data data) {
				data.update("", placement == null ? "" : placement.curves, "");
			}
			return;
		}
		final boolean snowLayer = block.is(net.minecraft.world.level.block.Blocks.SNOW);
		if (!segment && !block.isAir() && !snowLayer && !block.is(net.minecraft.world.level.block.Blocks.WATER)) {
			return;
		}
		final boolean waterlogged = block.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) && block.getFluidState().isSource();
		if (!segment && !block.isAir() && !snowLayer && !waterlogged) {
			return;
		}
		final var target = Blocks.PHYSICAL_RAIL.get().data.defaultBlockState()
				.setValue(BlockPhysicalRail.EAST_WEST.data, placement.eastWest)
				.setValue(SlabBlockExtension.WATERLOGGED, waterlogged);
		if (block != target) {
			level.setBlock(position, target, 3);
		}
		if (level.getBlockEntity(position) instanceof BlockPhysicalRail.Data data) {
			data.update(placement.owner, "", placement.geometry);
		}
	}

	private static final class Dimension {
		private final Simulator simulator;
		private final LinkedHashSet<BlockPos> pending = new LinkedHashSet<>();
		private final Set<Long> queuedChunks = new HashSet<>();
		private Map<Long, Map<BlockPos, Placement>> snapshot = Map.of();
		private List<String> source = List.of();
		private boolean refreshing;
		private boolean ready;
		private long nextRefresh;

		private Dimension(Simulator simulator) {
			this.simulator = simulator;
		}

		private void update(ServerLevel level, Map<Long, Map<BlockPos, Placement>> next) {
			if (next != null) {
				snapshot.forEach((key, entries) -> {
					if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null) {
						pending.addAll(entries.keySet());
					}
				});
				snapshot = next;
				queuedChunks.clear();
			}
			// CHUNK_LOAD can fire before the full chunk is visible to the server cache.
			snapshot.forEach((key, entries) -> {
				if (!queuedChunks.contains(key) && level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null) {
					pending.addAll(entries.keySet());
					queuedChunks.add(key);
				}
			});
			ready = true;
			refreshing = false;
		}
	}

	private record Placement(String owner, boolean eastWest, String curves, String geometry) {}
}
