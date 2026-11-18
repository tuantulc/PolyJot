package net.polyjot.client.screen;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.ConfigScreen;
import me.shedaniel.clothconfig2.api.Tooltip;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import me.shedaniel.math.Point;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.ArrayList;
import java.util.List;

public class FilterableSearchFieldEntry extends SearchFieldEntry {
    private final ConfigScreen configScreen;
    private final EditBox editBox;
    private final Button filterButton;
    private static FilterMode currentFilter = FilterMode.ALL;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public FilterableSearchFieldEntry(ConfigScreen screen, ClothConfigScreen.ListWidget listWidget) {
        super(screen, listWidget);
        this.configScreen = screen;
        if (screen instanceof me.shedaniel.clothconfig2.gui.AbstractConfigScreen acs) {
            this.setScreen(acs);
        }

        EditBox found = null;
        for (var child : super.children()) {
            if (child instanceof EditBox eb) {
                found = eb;
                break;
            }
        }
        this.editBox = found;

        this.filterButton = Button.builder(currentFilter.getLabel(), btn -> {
            currentFilter = currentFilter.next();
            btn.setMessage(currentFilter.getLabel());
            listWidget.scrollTo(0, false);
        }).bounds(0, 0, 20, 18).build();

        listWidget.entriesTransformer = entries -> {
            List filtered = new ArrayList();
            for (Object item : (List) entries) {
                if (item == this || item instanceof me.shedaniel.clothconfig2.gui.entries.EmptyEntry) {
                    filtered.add(item);
                } else if (item instanceof AbstractConfigEntry<?> entry) {
                    if (entry.isDisplayed() && (this.editBox == null || this.editBox.getValue().isEmpty() || screen.matchesSearch(entry.getSearchTags()))) {
                        filtered.add(entry);
                    }
                }
            }
            return filtered;
        };
    }

    public static FilterMode getCurrentFilter() {
        return currentFilter;
    }

    public static void setCurrentFilter(FilterMode mode) {
        currentFilter = mode != null ? mode : FilterMode.ALL;
    }

    public EditBox getEditBox() {
        return editBox;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        if (this.editBox != null) {
            int totalWidth = Math.min(entryWidth - 10, 500);
            int btnWidth = 20;
            int gap = 3;
            int boxWidth = totalWidth - btnWidth - gap;

            int startX = x + entryWidth / 2 - totalWidth / 2;
            int boxY = y + entryHeight / 2 - 9;

            this.editBox.setWidth(boxWidth);
            this.editBox.setX(startX);
            this.editBox.setY(boxY);
            this.editBox.render(guiGraphics, mouseX, mouseY, delta);

            if (this.editBox.getValue().isEmpty()) {
                this.editBox.setSuggestion(net.minecraft.client.resources.language.I18n.get("gui.polyjot.search_hint"));
            } else {
                this.editBox.setSuggestion(null);
            }

            this.filterButton.setX(startX + boxWidth + gap);
            this.filterButton.setY(boxY);
            this.filterButton.setWidth(btnWidth);
            this.filterButton.render(guiGraphics, mouseX, mouseY, delta);

            if (this.filterButton.isMouseOver(mouseX, mouseY) && this.configScreen != null) {
                this.configScreen.addTooltip(Tooltip.of(new Point(mouseX, mouseY), currentFilter.getTooltip()));
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        if (this.editBox != null) {
            return List.of(this.editBox, this.filterButton);
        }
        return super.children();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        if (this.editBox != null) {
            return List.of(this.editBox, this.filterButton);
        }
        return super.narratables();
    }
}
