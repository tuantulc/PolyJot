package net.polyjot.client.screen;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.polyjot.data.TranslationEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MultilineTranslationListEntry extends StringListEntry {

    private int cachedHeight = -1;
    private int lastCalculatedWidth = -1;
    private List<FormattedCharSequence> cachedLines = null;
    private int lastRenderWidth = -1;

    private final TranslationEntry entry;
    private Runnable onValueChange;

    public MultilineTranslationListEntry(
            Component fieldName,
            TranslationEntry entry,
            Component resetButtonKey,
            Consumer<String> saveCallback,
            boolean requiresRestart) {
        super(fieldName, entry.getCurrentTranslation(), resetButtonKey, () -> entry.getOriginalText(), saveCallback, null, requiresRestart);
        this.entry = entry;

        this.setTooltipSupplier(() -> {
            List<Component> warnings = new ArrayList<>();
            if (this.entry.isObsolete()) {
                warnings.add(Component.translatable("gui.polyjot.warning.obsolete"));
            }
            String currentVal = this.getValue();
            if ((currentVal != null && currentVal.length() > TranslationEntry.MAX_COMFORTABLE_LENGTH)
                    || (this.entry.getSourceEnglish() != null && this.entry.getSourceEnglish().length() > TranslationEntry.MAX_COMFORTABLE_LENGTH)
                    || (this.entry.getKey() != null && this.entry.getKey().length() > TranslationEntry.MAX_COMFORTABLE_LENGTH)) {
                warnings.add(Component.translatable("gui.polyjot.warning.too_long"));
            }
            List<String> missing = this.getMissingPlaceholders();
            if (!missing.isEmpty()) {
                warnings.add(Component.translatable("gui.polyjot.warning.missing_placeholder", missing.toString()));
            }

            if (warnings.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(warnings.toArray(new Component[0]));
        });
    }

    public void setOnValueChangeListener(Runnable onValueChange) {
        this.onValueChange = onValueChange;
        if (this.textFieldWidget != null) {
            this.textFieldWidget.setResponder(val -> {
                if (this.onValueChange != null) {
                    this.onValueChange.run();
                }
            });
        }
    }

    public List<String> getMissingPlaceholders() {
        if (this.entry == null) return Collections.emptyList();
        String currentVal = this.getValue();
        return TranslationEntry.calculateMissingPlaceholders(this.entry.getSourceEnglish(), currentVal != null ? currentVal : "");
    }

    public boolean hasMissingPlaceholders() {
        return !getMissingPlaceholders().isEmpty();
    }

    @Override
    public boolean isDisplayed() {
        if (!super.isDisplayed()) {
            return false;
        }
        FilterMode filter = FilterableSearchFieldEntry.getCurrentFilter();
        return switch (filter) {
            case ALL -> true;
            case EDITED -> this.entry.isCustomized();
            case UNCHANGED -> !this.entry.isCustomized();
            case TRANSLATED -> !this.entry.isUntranslated();
        };
    }

    @Override
    public int getItemHeight() {
        AbstractConfigScreen screen = this.getConfigScreen();
        int screenWidth = (screen != null && screen.width > 0)
                ? screen.width
                : Minecraft.getInstance().getWindow().getGuiScaledWidth();

        int entryWidth = Math.max(200, screenWidth - 80);
        int resetBtnWidth = 44;
        int fieldWidth = Math.max(140, Math.min(220, (int) (entryWidth * 0.38f)));
        int availableLabelWidth = Math.max(60, entryWidth - resetBtnWidth - fieldWidth - 16);

        if (this.cachedHeight > 0 && this.lastCalculatedWidth == availableLabelWidth) {
            return this.cachedHeight;
        }

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(this.getDisplayedFieldName(), availableLabelWidth);

        int height = (lines.size() <= 1) ? 24 : Math.max(24, lines.size() * (font.lineHeight + 2) + 8);
        this.cachedHeight = height;
        this.lastCalculatedWidth = availableLabelWidth;
        this.cachedLines = lines;
        this.lastRenderWidth = availableLabelWidth;
        return height;
    }

    @Override
    protected String stripAddText(String text) {
        if (text == null) return "";
        return super.stripAddText(text.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("\t", " "));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) { // right click to paste
            if (this.textFieldWidget.isMouseOver(mouseX, mouseY) && this.isEditable()) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clipboard != null && !clipboard.isEmpty()) {
                    String sanitized = stripAddText(clipboard);
                    this.textFieldWidget.setFocused(true);
                    this.textFieldWidget.setValue(sanitized);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        int widgetY = y + (entryHeight - 20) / 2;
        int resetBtnWidth = this.resetButton.getWidth();
        this.resetButton.setY(widgetY);
        this.resetButton.setX(x + entryWidth - resetBtnWidth);

        int fieldWidth = Math.max(140, Math.min(220, (int) (entryWidth * 0.38f)));
        int fieldX = this.resetButton.getX() - fieldWidth - 4;
        this.textFieldWidget.setEditable(this.isEditable());
        this.textFieldWidget.setY(widgetY + 1);
        this.textFieldWidget.setX(fieldX);
        setTextFieldWidth(this.textFieldWidget, fieldWidth);

        this.textFieldPreRender(this.textFieldWidget);

        this.resetButton.active = this.isEditable()
                && this.defaultValue != null
                && this.defaultValue.get() != null
                && !this.isMatchDefault(this.textFieldWidget.getValue());

        if (this.isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
            Rectangle area = this.getEntryArea(x, y, entryWidth, entryHeight);
            if (this.getParent() instanceof ClothConfigScreen.ListWidget<?> listWidget) {
                listWidget.thisTimeTarget = new Rectangle(area.x, area.y + this.getParent().getScroll(), area.width, area.height);
            }
            this.getTooltip(mouseX, mouseY).ifPresent(comps -> {
                if (comps != null && comps.length > 0) {
                    this.addTooltip(Tooltip.of(new Point(mouseX, mouseY), this.wrapLinesToScreen(comps)));
                }
            });
        }

        Component displayedName = this.getDisplayedFieldName();
        int availableLabelWidth = Math.max(60, (fieldX - x) - 8);

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines;
        if (this.cachedLines != null && this.lastRenderWidth == availableLabelWidth) {
            lines = this.cachedLines;
        } else {
            lines = font.split(displayedName, availableLabelWidth);
            this.cachedLines = lines;
            this.lastRenderWidth = availableLabelWidth;
        }

        int textY = y + (entryHeight - lines.size() * (font.lineHeight + 2)) / 2;
        int textColor = this.getPreferredTextColor();

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, textY, textColor, false);
            textY += font.lineHeight + 2;
        }

        this.resetButton.render(guiGraphics, mouseX, mouseY, delta);
        this.textFieldWidget.render(guiGraphics, mouseX, mouseY, delta);
    }
}
