package org.mtr.mod.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Util;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.ScreenExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.mapper.TransitUi;
import org.mtr.mod.InitClient;

public final class TrainModelLibraryScreen extends MTRScreenBase {
    private final ButtonWidgetExtension openPacks = new ButtonWidgetExtension(0, 0, 0, 20,
            TextHelper.translatable("gui.mtr.model_library_packs"), button -> openPackManager());
    private final ButtonWidgetExtension openCreator = new ButtonWidgetExtension(0, 0, 0, 20,
            TextHelper.translatable("gui.mtr.model_library_creator"), button -> {
                final int port = InitClient.getServerPort();
                if (port > 0) Util.getOperatingSystem().open("http://localhost:" + port + "/creator/");
            });
    private final ButtonWidgetExtension back = new ButtonWidgetExtension(0, 0, 0, 20,
            TextHelper.translatable("gui.back"), button -> onClose2());

    public TrainModelLibraryScreen(ScreenExtension parent) {
        super(parent);
    }

    @Override
    protected void init2() {
        super.init2();
        final int panelWidth = Math.min(330, width - 24);
        final int left = (width - panelWidth) / 2 + 12;
        final int buttonWidth = panelWidth - 24;
        final int top = Math.max(12, (height - 154) / 2);
        place(openPacks, left, top + 49, buttonWidth);
        place(openCreator, left, top + 84, buttonWidth);
        place(back, left, top + 119, buttonWidth);
    }

    private void place(ButtonWidgetExtension button, int x, int y, int width) {
        button.setX2(x);
        button.setY2(y);
        button.setWidth2(width);
        addChild(new ClickableWidget(button));
    }

    @Override
    public void render(GraphicsHolder graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        final int panelWidth = Math.min(330, width - 24);
        final int top = Math.max(12, (height - 154) / 2);
        TransitUi.panel(graphics, (width - panelWidth) / 2, top, panelWidth, 154);
        graphics.drawCenteredText(TextHelper.translatable("gui.mtr.model_library"), width / 2, top + 13, TransitUi.TEXT);
        graphics.drawCenteredText(TextHelper.translatable("gui.mtr.model_library_zip"), width / 2, top + 36, TransitUi.MUTED_TEXT);
        openCreator.active = InitClient.getServerPort() > 0;
        graphics.drawCenteredText(TextHelper.translatable(openCreator.active ? "gui.mtr.model_library_formats"
                : "gui.mtr.model_library_unavailable"), width / 2, top + 71, TransitUi.MUTED_TEXT);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen2() {
        return false;
    }

    private void openPackManager() {
        final Minecraft client = Minecraft.getInstance();
        final var repository = client.getResourcePackRepository();
        final var previousSelection = java.util.List.copyOf(repository.getSelectedIds());
        client.gui.setScreen(new PackSelectionScreen(repository, packs -> {
            if (!previousSelection.equals(java.util.List.copyOf(packs.getSelectedIds()))) {
                client.options.updateResourcePacks(packs);
            }
            client.gui.setScreen(this);
        }, client.getResourcePackDirectory(), Component.translatable("gui.mtr.model_library_packs")));
    }
}
