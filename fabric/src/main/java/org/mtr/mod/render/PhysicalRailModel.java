package org.mtr.mod.render;

import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.mod.block.BlockPhysicalRail;
import org.mtr.mod.data.RailBlockGeometry;

import java.util.List;
import java.util.function.Predicate;

/** Emits immutable per-block track meshes into the normal chunk rebuild. */
public final class PhysicalRailModel implements BlockStateModel, FabricBlockStateModel {
	private final BlockStateModel original;
	private final boolean node;

	private PhysicalRailModel(BlockStateModel original, boolean node) {
		this.original = original;
		this.node = node;
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) -> {
			final var block = bake.state().getBlock();
			return block instanceof BlockPhysicalRail || block instanceof BlockPhysicalRail.Node
					? new PhysicalRailModel(model, block instanceof BlockPhysicalRail.Node) : model;
		}));
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		if (node) {
			original.collectParts(random, parts);
		}
	}

	@Override
	public Material.Baked particleMaterial() {
		return original.particleMaterial();
	}

	@Override
	public int materialFlags() { return original.materialFlags(); }

	@Override
	public Object createGeometryKey(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random) {
		return null;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter world, BlockPos pos, BlockState state,
						  RandomSource random, Predicate<Direction> cullTest) {
		if (node) {
			((FabricBlockStateModel) original).emitQuads(emitter, world, pos, state, random, cullTest);
		}
		final Object data = ((FabricBlockGetter) world).getBlockEntityRenderData(pos);
		if (!(data instanceof RailBlockGeometry.Geometry geometry)) {
			return;
		}
		final var dispatcher = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
		final Material.Baked steel = dispatcher.get(net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()).particleMaterial();
		final Material.Baked wood = dispatcher.get(net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS.defaultBlockState()).particleMaterial();
		for (final RailBlockGeometry.Part part : geometry.parts()) {
			final Material.Baked sprite = part.steel() ? steel : wood;
			final List<RailBlockGeometry.Point> polygon = part.polygon();
			for (int i = 1; i < polygon.size() - 1; i++) {
				face(emitter, sprite, polygon.get(0), polygon.get(i), polygon.get(i + 1), polygon.get(i + 1), part.height());
				face(emitter, sprite, polygon.get(0), polygon.get(i + 1), polygon.get(i), polygon.get(i), 0);
			}
			for (int i = 0; i < polygon.size(); i++) {
				final var a = polygon.get(i);
				final var b = polygon.get((i + 1) % polygon.size());
				face(emitter, sprite, a, b, raised(b, part.height()), raised(a, part.height()), 0);
			}
		}
	}

	private static RailBlockGeometry.Point raised(RailBlockGeometry.Point point, double height) {
		return new RailBlockGeometry.Point(point.x(), point.y() + height, point.z());
	}

	private static void face(QuadEmitter emitter, Material.Baked sprite,
							 RailBlockGeometry.Point a, RailBlockGeometry.Point b, RailBlockGeometry.Point c, RailBlockGeometry.Point d, double height) {
		final var points = List.of(a, b, c, d);
		for (int i = 0; i < 4; i++) {
			final var point = points.get(i);
			emitter.pos(i, (float) point.x(), (float) (point.y() + height), (float) point.z())
					.color(i, -1).uv(i, i == 0 || i == 3 ? 0 : 1, i < 2 ? 0 : 1);
		}
		emitter.materialBake(sprite, MutableQuadView.BAKE_NORMALIZED).emit();
	}
}
