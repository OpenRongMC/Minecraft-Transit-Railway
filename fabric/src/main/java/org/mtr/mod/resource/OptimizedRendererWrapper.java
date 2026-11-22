package org.mtr.mod.resource;

import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.data.IGui;

import javax.annotation.Nullable;

public final class OptimizedRendererWrapper implements IGui {

	private static final boolean USE_OPTIMIZED_RENDERER = !Boolean.getBoolean("mtr.disableOptimizedRenderer")
			&& OptimizedRenderer.hasOptimizedRendering();

	public static boolean isEnabled() {
		return USE_OPTIMIZED_RENDERER;
	}


	@Nullable
	private final OptimizedRenderer optimizedRenderer;

	public OptimizedRendererWrapper() {
		this.optimizedRenderer = OptimizedRendererWrapper.isEnabled() ? new OptimizedRenderer() : null;
	}

	public void beginReload() {
		if (optimizedRenderer != null) {
			optimizedRenderer.beginReload();
		}
	}

	public void finishReload() {
		if (optimizedRenderer != null) {
			optimizedRenderer.finishReload();
		}
	}

	public void queue(OptimizedModelWrapper optimizedModel, GraphicsHolder graphicsHolder, int light) {
		if (optimizedRenderer != null && optimizedModel.optimizedModel != null) {
			optimizedRenderer.setRenderTranslucent(!org.mtr.mod.config.Config.getClient().getHideTranslucentParts());
			optimizedRenderer.queue(optimizedModel.optimizedModel, graphicsHolder, ARGB_WHITE, light);
		}
	}

	public void render(boolean renderTranslucent) {
		if (optimizedRenderer != null) {
			optimizedRenderer.render(renderTranslucent);
		}
	}
}
