package net.polyjot.client.screen;

import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationScanner;
import net.polyjot.data.TranslationStorage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
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
    private Button sortBtn;
    private ModListWidget modListWidget;

    private static SortMode currentSort = SortMode.NAME_ASC;

    public ModSelectionScreen(Screen parentScreen) {
        super(Component.translatable("gui.polyjot.title"));
        this.parentScreen = parentScreen;
        this.targetLang = Minecraft.getInstance().options.languageCode;
    }

    public ModSelectionScreen(Screen parentScreen, String targetLang) {
        super(Component.translatable("gui.polyjot.title"));
        this.parentScreen = parentScreen;
        this.targetLang = targetLang;
    }

    @Override
    protected void init() {
        super.init();

        String prevQuery = this.searchBox != null ? this.searchBox.getValue() : "";

        if (this.allMods.isEmpty()) {
            this.allMods = TranslationScanner.scanAllMods(this.targetLang);
        }

        int listWidth = Math.min(this.width - 50, 440);
        int startX = this.width / 2 - listWidth / 2;

        int sortBtnWidth = 20;
        int folderBtnWidth = 75;
        int exportBtnWidth = 70;
        int dumpBtnWidth = 65;
        int gap = 4;
        int searchWidth = listWidth - sortBtnWidth - folderBtnWidth - exportBtnWidth - dumpBtnWidth - (gap * 4);

        this.searchBox = new EditBox(this.font, startX, 22, searchWidth, 20, Component.translatable("gui.polyjot.search_mods"));
        this.searchBox.setHint(Component.translatable("gui.polyjot.search_mods_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addWidget(this.searchBox);
        if (!prevQuery.isEmpty()) {
            this.searchBox.setValue(prevQuery);
        }

        int sortBtnX = startX + searchWidth + gap;
        this.sortBtn = Button.builder(Component.literal("⇅"), btn -> {
            currentSort = currentSort.next();
            updateSortButton();
            populateList(this.searchBox != null ? this.searchBox.getValue() : "");
        }).bounds(sortBtnX, 22, sortBtnWidth, 20).build();
        updateSortButton();
        this.addRenderableWidget(this.sortBtn);

        int folderBtnX = sortBtnX + sortBtnWidth + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.polyjot.open_folder"), btn -> {
            Path folder = TranslationStorage.getLanguageDirectory(targetLang);
            try {
                Files.createDirectories(folder);
                Util.getPlatform().openFile(folder.toFile());
            } catch (Exception ignored) {
            }
        }).bounds(folderBtnX, 22, folderBtnWidth, 20)
        .tooltip(Tooltip.create(Component.translatable("gui.polyjot.open_folder.tooltip")))
        .build());

        int exportBtnX = folderBtnX + folderBtnWidth + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.polyjot.export_zip"), btn -> {
            Minecraft.getInstance().setScreen(new ExportPackScreen(this, this.targetLang, this.allMods.values()));
        }).bounds(exportBtnX, 22, exportBtnWidth, 20)
        .tooltip(Tooltip.create(Component.translatable("gui.polyjot.export_zip.tooltip")))
        .build());

        int dumpBtnX = exportBtnX + exportBtnWidth + gap;
        Button dumpBtn = Button.builder(Component.translatable("gui.polyjot.dump_missing"), btn -> {
            Minecraft.getInstance().setScreen(new DumpAllTranslationsScreen(this, this.targetLang, this.allMods.values()));
        }).bounds(dumpBtnX, 22, dumpBtnWidth, 20)
        .tooltip(Tooltip.create(Component.translatable("gui.polyjot.dump_missing.tooltip")))
        .build();
        this.addRenderableWidget(dumpBtn);

        this.modListWidget = new ModListWidget(this.minecraft, this.width, this.height, 48, this.height - 36, 32);
        this.addWidget(this.modListWidget);
        populateList(prevQuery);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            this.minecraft.setScreen(this.parentScreen);
        }).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private void updateSortButton() {
        if (this.sortBtn != null) {
            this.sortBtn.setTooltip(Tooltip.create(
                    Component.translatable("gui.polyjot.sort_tooltip", currentSort.getDisplayName())
            ));
        }
    }

    private void onSearchChanged(String query) {
        populateList(query);
    }

    private void populateList(String query) {
        this.modListWidget.clearEntries();
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);

        List<ModTranslationData> filtered = new ArrayList<>();
        for (ModTranslationData modData : allMods.values()) {
            if (!lowerQuery.isEmpty()) {
                boolean matchesName = modData.getDisplayName().toLowerCase(Locale.ROOT).contains(lowerQuery);
                boolean matchesId = modData.getModId().toLowerCase(Locale.ROOT).contains(lowerQuery);
                if (!matchesName && !matchesId) {
                    continue;
                }
            }
            filtered.add(modData);
        }

        filtered.sort(currentSort.getComparator());

        for (ModTranslationData modData : filtered) {
            this.modListWidget.addModEntry(new ModEntry(this, modData));
        }
        this.modListWidget.setScrollAmount(0);
    }

    public void refreshModList() {
        double scroll = this.modListWidget != null ? this.modListWidget.getScrollAmount() : 0;
        this.allMods = TranslationScanner.scanAllMods(this.targetLang);
        if (this.searchBox != null) {
            populateList(this.searchBox.getValue());
        } else {
            populateList("");
        }
        if (this.modListWidget != null) {
            this.modListWidget.setScrollAmount(scroll);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox != null && this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.modListWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        Component title = Component.translatable("gui.polyjot.title").append(" (" + targetLang + ")");
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 8, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    public enum SortMode {
        NAME_ASC(Component.translatable("gui.polyjot.sort.name_asc"),
                Comparator.comparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ModTranslationData::getModId)),
        NAME_DESC(Component.translatable("gui.polyjot.sort.name_desc"),
                Comparator.comparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER).reversed()
                        .thenComparing(ModTranslationData::getModId)),
        PROGRESS_DESC(Component.translatable("gui.polyjot.sort.progress_desc"),
                Comparator.comparingDouble(ModTranslationData::getCompletionPercentage).reversed()
                        .thenComparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ModTranslationData::getModId)),
        PROGRESS_ASC(Component.translatable("gui.polyjot.sort.progress_asc"),
                Comparator.comparingDouble(ModTranslationData::getCompletionPercentage)
                        .thenComparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ModTranslationData::getModId)),
        SIZE_DESC(Component.translatable("gui.polyjot.sort.size_desc"),
                Comparator.comparingInt(ModTranslationData::getTotalCount).reversed()
                        .thenComparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ModTranslationData::getModId)),
        SIZE_ASC(Component.translatable("gui.polyjot.sort.size_asc"),
                Comparator.comparingInt(ModTranslationData::getTotalCount)
                        .thenComparing(ModTranslationData::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ModTranslationData::getModId));

        private final Component displayName;
        private final Comparator<ModTranslationData> comparator;

        SortMode(Component displayName, Comparator<ModTranslationData> comparator) {
            this.displayName = displayName;
            this.comparator = comparator;
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public Comparator<ModTranslationData> getComparator() {
            return this.comparator;
        }

        public SortMode next() {
            SortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
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
        private final Button dumpButton;
        private final Button editButton;

        public ModEntry(ModSelectionScreen screen, ModTranslationData modData) {
            this.screen = screen;
            this.modData = modData;

            // dump just this mod
            boolean alreadyDumped = Files.exists(TranslationStorage.getModFilePath(screen.targetLang, modData.getModId()));
            this.dumpButton = Button.builder(alreadyDumped ? Component.translatable("gui.polyjot.dumped") : Component.translatable("gui.polyjot.dump"), btn -> {
                boolean saved = TranslationStorage.saveModTranslations(screen.targetLang, modData);
                if (saved) {
                    btn.active = false;
                    btn.setMessage(Component.translatable("gui.polyjot.dumped"));
                    btn.setTooltip(Tooltip.create(Component.translatable("gui.polyjot.dump.tooltip_dumped", screen.targetLang, modData.getModId())));
                    Minecraft.getInstance().getToasts().addToast(
                            SystemToast.multiline(
                                    Minecraft.getInstance(),
                                    SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                                    Component.translatable("polyjot.name"),
                                    Component.translatable("toast.polyjot.dump_mod", modData.getDisplayName())
                            )
                    );
                }
            }).bounds(0, 0, 50, 20).build();

            if (alreadyDumped) {
                this.dumpButton.active = false;
                this.dumpButton.setTooltip(Tooltip.create(Component.translatable("gui.polyjot.dump.tooltip_already", screen.targetLang, modData.getModId())));
            } else {
                this.dumpButton.setTooltip(Tooltip.create(Component.translatable("gui.polyjot.dump.tooltip_generate", screen.targetLang, modData.getModId())));
            }

            // warn on huge mods to avoid cloth lag
            Button.Builder editBuilder = Button.builder(Component.translatable("gui.polyjot.translate"), btn -> {
                Screen editorScreen = TranslationConfigBuilder.createScreen(screen, modData, screen.targetLang);
                Minecraft.getInstance().setScreen(editorScreen);
            }).bounds(0, 0, 68, 20);

            if ("polyjot".equals(modData.getModId())) {
                editBuilder.tooltip(Tooltip.create(Component.translatable("gui.polyjot.easter_egg")));
            } else if (modData.getTotalCount() >= 500) {
                editBuilder.tooltip(Tooltip.create(Component.translatable("gui.polyjot.large_mod_warning", modData.getTotalCount())));
            }
            this.editButton = editBuilder.build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            guiGraphics.drawString(screen.font, modData.getDisplayName(), left + 8, top + 4, 0xFFFFFF, false);

            int translated = modData.getTranslatedCount();
            int total = modData.getTotalCount();
            float pct = modData.getCompletionPercentage();

            String info = String.format("§8%s §7• §a%d§7/%d (§e%.1f%%§7)", modData.getModId(), translated, total, pct);
            guiGraphics.drawString(screen.font, info, left + 8, top + 16, 0xAAAAAA, false);

            int dumpBtnWidth = 50;
            int editBtnWidth = 68;
            int gap = 4;
            int editBtnX = left + width - editBtnWidth - 4;
            int dumpBtnX = editBtnX - dumpBtnWidth - gap;

            this.dumpButton.setX(dumpBtnX);
            this.dumpButton.setY(top + 6);
            this.dumpButton.render(guiGraphics, mouseX, mouseY, partialTick);

            this.editButton.setX(editBtnX);
            this.editButton.setY(top + 6);
            this.editButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.dumpButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
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
