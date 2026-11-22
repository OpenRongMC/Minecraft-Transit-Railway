package org.mtr.mod.data;

import net.minecraft.core.BlockPos;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.com.google.gson.Gson;
import org.mtr.mapping.holder.VoxelShape;
import org.mtr.mapping.holder.VoxelShapes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Geometry shared by server collision and the client's chunk model. */
public final class RailBlockGeometry {
	// Bounds measured from MTR's original models/rail/rail.obj. A centered
	// track spans three block columns even though its mesh is 2.232046 blocks wide.
	public static final double HALF_WIDTH = 1.116023;
	public static final double GAUGE_HALF = 0.7516915;
	private static final double STEP = 0.25;
	private static final Gson GSON = new Gson();

	public record Point(double x, double y, double z) {
		Point interpolate(Point other, double amount) {
			return new Point(x + (other.x - x) * amount, y + (other.y - y) * amount, z + (other.z - z) * amount);
		}
	}
	public record Part(String owner, boolean steel, double height, List<Point> polygon) {}
	public record Geometry(List<Part> parts) {
		public String encode() {
			return GSON.toJson(this);
		}

		public VoxelShape collision() {
			VoxelShape shape = VoxelShapes.empty();
			for (final Part part : parts) {
				double minX = 1, minY = Double.POSITIVE_INFINITY, minZ = 1, maxX = 0, maxY = 0, maxZ = 0;
				for (final Point point : part.polygon) {
					minX = Math.min(minX, point.x);
					minY = Math.min(minY, point.y);
					minZ = Math.min(minZ, point.z);
					maxX = Math.max(maxX, point.x);
					maxY = Math.max(maxY, point.y + part.height);
					maxZ = Math.max(maxZ, point.z);
				}
				if (maxX > minX && maxZ > minZ) {
					shape = VoxelShapes.union(shape, VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ));
				}
			}
			return shape;
		}
	}

	public static Geometry decode(String encoded) {
		return encoded.isEmpty() ? new Geometry(List.of()) : GSON.fromJson(encoded, Geometry.class);
	}

	public static void append(Rail rail, Map<BlockPos, List<Part>> blocks) {
		final double length = rail.railMath.getLength();
		for (double distance = 0; distance < length; distance += STEP) {
			final Vector from = rail.railMath.getPosition(distance, false);
			final Vector to = rail.railMath.getPosition(Math.min(length, distance + STEP), false);
			final double dx = to.x - from.x;
			final double dz = to.z - from.z;
			final double horizontal = Math.hypot(dx, dz);
			if (horizontal < 1E-8) {
				continue;
			}
			final double nx = dz / horizontal;
			final double nz = -dx / horizontal;
			for (final double side : new double[]{-GAUGE_HALF, GAUGE_HALF}) {
				prism(rail.getHexId(), true, 0.13928, List.of(
						point(from, nx, nz, side - 0.07, 0.125),
						point(to, nx, nz, side - 0.07, 0.125),
						point(to, nx, nz, side + 0.07, 0.125),
						point(from, nx, nz, side + 0.07, 0.125)), blocks);
			}
			if (Math.round(distance / STEP) % 3 == 0) {
				prism(rail.getHexId(), false, 0.125, List.of(
						point(from, nx, nz, -HALF_WIDTH, 0),
						point(to, nx, nz, -HALF_WIDTH, 0),
						point(to, nx, nz, HALF_WIDTH, 0),
						point(from, nx, nz, HALF_WIDTH, 0)), blocks);
			}
		}
	}

	private static Point point(Vector vector, double nx, double nz, double offset, double height) {
		return new Point(vector.x + nx * offset, vector.y + height, vector.z + nz * offset);
	}

	private static void prism(String owner, boolean steel, double height, List<Point> polygon, Map<BlockPos, List<Part>> blocks) {
		final int minX = (int) Math.floor(polygon.stream().mapToDouble(Point::x).min().orElseThrow());
		final int maxX = (int) Math.floor(polygon.stream().mapToDouble(Point::x).max().orElseThrow() - 1E-7);
		final int minZ = (int) Math.floor(polygon.stream().mapToDouble(Point::z).min().orElseThrow());
		final int maxZ = (int) Math.floor(polygon.stream().mapToDouble(Point::z).max().orElseThrow() - 1E-7);
		final int y = (int) Math.floor(polygon.stream().mapToDouble(Point::y).min().orElseThrow() + 1E-7);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				List<Point> clipped = clip(polygon, true, x, true);
				clipped = clip(clipped, true, x + 1, false);
				clipped = clip(clipped, false, z, true);
				clipped = clip(clipped, false, z + 1, false);
				if (clipped.size() < 3) {
					continue;
				}
				final List<Point> local = new ArrayList<>();
				for (final Point p : clipped) {
					local.add(new Point(p.x - x, p.y - y, p.z - z));
				}
				blocks.computeIfAbsent(new BlockPos(x, y, z), ignored -> new ArrayList<>())
						.add(new Part(owner, steel, height, List.copyOf(local)));
			}
		}
	}

	private static List<Point> clip(List<Point> polygon, boolean xAxis, double boundary, boolean keepGreater) {
		final List<Point> result = new ArrayList<>();
		if (polygon.isEmpty()) {
			return result;
		}
		Point previous = polygon.getLast();
		for (final Point current : polygon) {
			final double a = xAxis ? previous.x : previous.z;
			final double b = xAxis ? current.x : current.z;
			final boolean aInside = keepGreater ? a >= boundary : a <= boundary;
			final boolean bInside = keepGreater ? b >= boundary : b <= boundary;
			if (aInside != bInside) {
				result.add(previous.interpolate(current, (boundary - a) / (b - a)));
			}
			if (bInside) {
				result.add(current);
			}
			previous = current;
		}
		return result;
	}
}
