package net.languageport.client.screen;

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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultilineTranslationListEntry extends StringListEntry {

    public MultilineTranslationListEntry(
            Component fieldName,
            String original,
            Component resetButtonKey,
            Supplier<String> defaultValue,
            Consumer<String> saveCallback,
            Supplier<Optional<Component[]>> tooltipSupplier,
            boolean requiresRestart) {
        super(fieldName, original, resetButtonKey, defaultValue, saveCallback, tooltipSupplier, requiresRestart);
    }

    public void setCustomErrorSupplier(Function<String, Optional<Component>> errorSupplier) {
        if (errorSupplier != null) {
            this.setErrorSupplier(() -> errorSupplier.apply(this.getValue()));
        }
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

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(this.getDisplayedFieldName(), availableLabelWidth);

        if (lines.size() <= 1) {
            return 24;
        }

        // Each line takes (lineHeight + 2), plus top/bottom margin
        return Math.max(24, lines.size() * (font.lineHeight + 2) + 8);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        // 1. Hover target & Tooltip
        if (this.isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
            Rectangle area = this.getEntryArea(x, y, entryWidth, entryHeight);
            if (this.getParent() instanceof ClothConfigScreen.ListWidget<?> listWidget) {
                listWidget.thisTimeTarget = new Rectangle(area.x, area.y + this.getParent().getScroll(), area.width, area.height);
            }
            this.getTooltip(mouseX, mouseY).ifPresent(comps -> {
                this.addTooltip(Tooltip.of(new Point(mouseX, mouseY), this.wrapLinesToScreen(comps)));
            });
        }

        // 2. Reset Button positioning & active state
        this.resetButton.active = this.isEditable()
                && this.defaultValue != null
                && this.defaultValue.get() != null
                && !this.isMatchDefault(this.textFieldWidget.getValue());

        int widgetY = y + (entryHeight - 20) / 2;
        int resetBtnWidth = this.resetButton.getWidth();
        this.resetButton.setY(widgetY);
        this.resetButton.setX(x + entryWidth - resetBtnWidth);

        // 3. Text Field positioning & width
        int fieldWidth = Math.max(140, Math.min(220, (int) (entryWidth * 0.38f)));
        int fieldX = this.resetButton.getX() - fieldWidth - 4;
        this.textFieldWidget.setEditable(this.isEditable());
        this.textFieldWidget.setY(widgetY + 1);
        this.textFieldWidget.setX(fieldX);
        setTextFieldWidth(this.textFieldWidget, fieldWidth);

        // Update text field text color based on errors
        this.textFieldPreRender(this.textFieldWidget);

        // 4. Wrap and render the label on the left
        Component displayedName = this.getDisplayedFieldName();
        int availableLabelWidth = Math.max(60, (fieldX - x) - 8);

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(displayedName, availableLabelWidth);

        int textY = y + (entryHeight - lines.size() * (font.lineHeight + 2)) / 2;
        int textColor = this.getPreferredTextColor();

        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, textY, textColor, false);
            textY += font.lineHeight + 2;
        }

        // 5. Render child widgets
        this.resetButton.render(guiGraphics, mouseX, mouseY, delta);
        this.textFieldWidget.render(guiGraphics, mouseX, mouseY, delta);
    }
}
