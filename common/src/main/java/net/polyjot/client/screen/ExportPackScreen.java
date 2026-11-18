package net.polyjot.client.screen;

import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ExportPackScreen extends Screen {
    private final Screen parentScreen;
    private final String targetLang;
    private final List<ModTranslationData> modDataList;

    private EditBox nameInput;
    private Button exportButton;

    public ExportPackScreen(Screen parentScreen, String targetLang, Collection<ModTranslationData> mods) {
        super(Component.literal("Export to Resource Pack"));
        this.parentScreen = parentScreen;
        this.targetLang = targetLang;
        this.modDataList = new ArrayList<>(mods);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int inputWidth = 200;
        this.nameInput = new EditBox(this.font, centerX - inputWidth / 2 - 12, centerY - 10, inputWidth, 20, Component.literal("Pack Name"));
        this.nameInput.setValue("Translations_" + this.targetLang);
        this.nameInput.setMaxLength(64);
        this.nameInput.setResponder(text -> updateExportButtonState());
        this.addRenderableWidget(this.nameInput);
        this.setInitialFocus(this.nameInput);

        Component infoTooltip = Component.literal(
                "§6Why export to .zip?\n\n" +
                "§7Normally, simply clicking §a'Save & Quit'§7 in the translation editor is all you need. Your translations are loaded live automatically into the game!\n\n" +
                "§fExporting as a .zip creates a standalone resource pack in your §e'resourcepacks'§f folder, useful if you want to share your translations with friends or include them in a modpack."
        );

        Button infoButton = Button.builder(Component.literal("§b?"), btn -> {})
                .tooltip(Tooltip.create(infoTooltip))
                .bounds(centerX + inputWidth / 2 - 8, centerY - 10, 20, 20)
                .build();
        this.addRenderableWidget(infoButton);

        this.exportButton = Button.builder(Component.literal("Export"), btn -> doExport())
                .bounds(centerX - 105, centerY + 25, 100, 20)
                .build();
        this.addRenderableWidget(this.exportButton);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX + 5, centerY + 25, 100, 20)
                .build());

        updateExportButtonState();
    }

    private void updateExportButtonState() {
        if (this.exportButton != null) {
            String name = this.nameInput.getValue().trim();
            this.exportButton.active = !name.isEmpty();
        }
    }

    private void doExport() {
        String packName = this.nameInput.getValue().trim();
        if (packName.isEmpty()) {
            packName = "Translations_" + this.targetLang;
        }

        if (packName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            packName = packName.substring(0, packName.length() - 4).trim();
        }

        // strip illegal chars
        packName = packName.replaceAll("[^a-zA-Z0-9_\\-\\s]", "").trim();
        if (packName.isEmpty()) {
            packName = "Translations_" + this.targetLang;
        }

        Path zipPath = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("resourcepacks")
                .resolve(packName + ".zip");

        boolean success = TranslationStorage.exportResourcePack(this.targetLang, this.modDataList, zipPath);
        if (success) {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.NARRATOR_TOGGLE,
                            Component.literal("PolyJot"),
                            Component.literal("Exported to resourcepacks/" + packName + ".zip")
                    )
            );
        } else {
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastId.NARRATOR_TOGGLE,
                            Component.literal("PolyJot"),
                            Component.literal("§cFailed to export resource pack!")
                    )
            );
        }

        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // enter
            if (this.exportButton != null && this.exportButton.active) {
                doExport();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, "Export to Resource Pack Folder", centerX, centerY - 55, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Resource Pack (.zip) Name:", centerX, centerY - 25, 0xAAAAAA);

        guiGraphics.drawCenteredString(this.font, "§8Destination: .minecraft/resourcepacks/<name>.zip", centerX, centerY + 55, 0x888888);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
}
