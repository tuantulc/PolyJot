package net.polyjot.client.screen;

import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationEntry;
import net.polyjot.data.TranslationStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DumpAllTranslationsScreen extends Screen {
    private final ModSelectionScreen parentScreen;
    private final String targetLang;
    private final Collection<ModTranslationData> allMods;

    public DumpAllTranslationsScreen(ModSelectionScreen parentScreen, String targetLang, Collection<ModTranslationData> allMods) {
        super(Component.literal("Sync & Generate All Translations"));
        this.parentScreen = parentScreen;
        this.targetLang = targetLang;
        this.allMods = allMods;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int btnY = Math.min(this.height - 35, centerY + 80);

        Button cancelBtn = Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX - 145, btnY, 130, 20)
                .build();
        this.addRenderableWidget(cancelBtn);
        this.setInitialFocus(cancelBtn);

        Button confirmBtn = Button.builder(Component.literal("§c§lI know what I'm doing!"), btn -> doDumpAll())
                .bounds(centerX + 15, btnY, 150, 20)
                .build();
        this.addRenderableWidget(confirmBtn);
    }

    private void doDumpAll() {
        int savedCount = 0;
        for (ModTranslationData modData : this.allMods) {
            // mark active keys as custom so everything stays in sync
            for (TranslationEntry entry : modData.getEntries().values()) {
                if (!entry.isUntranslated()) {
                    entry.setCustomized(true);
                }
            }
            boolean success = TranslationStorage.saveModTranslations(this.targetLang, modData);
            if (success) {
                savedCount++;
            }
        }

        Minecraft.getInstance().getToasts().addToast(
                SystemToast.multiline(
                        Minecraft.getInstance(),
                        SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                        Component.literal("PolyJot"),
                        Component.literal("Generated files for " + savedCount + " mods in translations/" + this.targetLang)
                )
        );

        this.parentScreen.refreshModList();
        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, "§6§lSync & Generate All Translation Files", centerX, centerY - 95, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Will create JSON files for §e" + this.allMods.size() + " mods§r in §7.minecraft/translations/" + this.targetLang + "/§r", centerX, centerY - 78, 0xAAAAAA);

        int textWidth = Math.min(this.width - 50, 420);
        int textX = centerX - textWidth / 2;
        int currentY = centerY - 55;
        Font font = this.font;

        List<Component> paragraphs = new ArrayList<>();
        paragraphs.add(Component.literal("§f§lWho should use this?"));
        paragraphs.add(Component.literal("§aTranslators & Modpack creators: §fIf you want all translation files readily available on disk to edit with external tools, commit to Git, or prepare a release."));
        paragraphs.add(Component.literal("§aSyncing progress to a resource pack: §fIf you received a translation pack and want to sync all its active translations into your local editable files."));
        paragraphs.add(Component.literal(" "));
        paragraphs.add(Component.literal("§f§lWho can skip this?"));
        paragraphs.add(Component.literal("§fIf you're a casual player, you don't need this! PolyJot automatically loads active resource packs, and editing any mod creates only that mod's file on demand to avoid disk clutter."));

        for (Component p : paragraphs) {
            List<FormattedCharSequence> lines = font.split(p, textWidth);
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(font, line, textX, currentY, 0xFFFFFF, false);
                currentY += font.lineHeight + 2;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
}
