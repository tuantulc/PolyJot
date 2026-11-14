package net.languageport.client.screen;

import net.languageport.data.ModTranslationData;
import net.languageport.data.TranslationScanner;
import net.languageport.data.TranslationStorage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModSelectionScreen extends Screen {
    private final Screen parentScreen;
    final String targetLang;
    private Map<String, ModTranslationData> allMods = new LinkedHashMap<>();

    private EditBox searchBox;
    private ModListWidget modListWidget;

    public ModSelectionScreen(Screen parentScreen) {
        super(Component.literal("Translation Manager"));
        this.parentScreen = parentScreen;
        this.targetLang = Minecraft.getInstance().options.languageCode;
    }

    public ModSelectionScreen(Screen parentScreen, String targetLang) {
        super(Component.literal("Translation Manager"));
        this.parentScreen = parentScreen;
        this.targetLang = targetLang;
    }

    @Override
    protected void init() {
        super.init();

        // 1. Scan mods
        this.allMods = TranslationScanner.scanAllMods(this.targetLang);

        // 2. Layout calculations for a wider, comfortable interface
        int listWidth = Math.min(this.width - 50, 440);
        int startX = this.width / 2 - listWidth / 2;

        int exportBtnWidth = 75;
        int folderBtnWidth = 80;
        int gap = 6;
        int searchWidth = listWidth - exportBtnWidth - folderBtnWidth - (gap * 2);

        this.searchBox = new EditBox(this.font, startX, 22, searchWidth, 20, Component.literal("Search Mods"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);

        // 3. Top action buttons
        int folderBtnX = startX + searchWidth + gap;
        this.addRenderableWidget(Button.builder(Component.literal("Open Folder"), btn -> {
            Path folder = TranslationStorage.getLanguageDirectory(targetLang);
            try {
                Files.createDirectories(folder);
                Util.getPlatform().openFile(folder.toFile());
            } catch (Exception e) {
                // Ignore
            }
        }).bounds(folderBtnX, 22, folderBtnWidth, 20).build());

        int exportBtnX = folderBtnX + folderBtnWidth + gap;
        this.addRenderableWidget(Button.builder(Component.literal("Export .zip"), btn -> {
            Minecraft.getInstance().setScreen(new ExportPackScreen(this, this.targetLang, this.allMods.values()));
        }).bounds(exportBtnX, 22, exportBtnWidth, 20).build());

        // 4. Mod List Widget
        this.modListWidget = new ModListWidget(this.minecraft, this.width, this.height, 48, this.height - 36, 32);
        this.addWidget(this.modListWidget);
        populateList("");

        // 5. Done button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            this.minecraft.setScreen(this.parentScreen);
        }).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void onSearchChanged(String query) {
        populateList(query);
    }

    private void populateList(String query) {
        this.modListWidget.clearEntries();
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);

        for (ModTranslationData modData : allMods.values()) {
            if (!lowerQuery.isEmpty()) {
                boolean matchesName = modData.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerQuery);
                boolean matchesId = modData.getModId().toLowerCase(Locale.ROOT).contains(lowerQuery);
                if (!matchesName && !matchesId) {
                    continue;
                }
            }

            this.modListWidget.addModEntry(new ModEntry(this, modData));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.modListWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, "Translation Manager (" + targetLang + ")", this.width / 2, 8, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    public static class ModListWidget extends ObjectSelectionList<ModEntry> {

        public ModListWidget(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
        }

        public void addModEntry(ModEntry entry) {
            this.addEntry(entry);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(this.width - 50, 440);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + this.getRowWidth() / 2 + 6;
        }
    }

    public static class ModEntry extends ObjectSelectionList.Entry<ModEntry> {
        private final ModSelectionScreen screen;
        private final ModTranslationData modData;
        private final Button editButton;

        public ModEntry(ModSelectionScreen screen, ModTranslationData modData) {
            this.screen = screen;
            this.modData = modData;
            this.editButton = Button.builder(Component.literal("Translate"), btn -> {
                Screen editorScreen = TranslationConfigBuilder.createScreen(screen, modData, screen.targetLang);
                Minecraft.getInstance().setScreen(editorScreen);
            }).bounds(0, 0, 75, 20).build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            guiGraphics.drawString(screen.font, modData.getDisplayName(), left + 8, top + 4, 0xFFFFFF, false);

            int translated = modData.getTranslatedCount();
            int total = modData.getTotalCount();
            float pct = modData.getCompletionPercentage();

            String info = String.format("§8%s §7• §a%d§7/%d (§e%.1f%%§7)", modData.getModId(), translated, total, pct);
            guiGraphics.drawString(screen.font, info, left + 8, top + 16, 0xAAAAAA, false);

            this.editButton.setX(left + width - 80);
            this.editButton.setY(top + 6);
            this.editButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.editButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal(modData.getDisplayName());
        }
    }
}
