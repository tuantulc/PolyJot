package net.polyjot.client.screen;

import net.minecraft.network.chat.Component;

public enum FilterMode {
    ALL(Component.literal("§fA"), Component.translatable("gui.polyjot.filter.all")),
    EDITED(Component.literal("§eE"), Component.translatable("gui.polyjot.filter.edited")),
    UNCHANGED(Component.literal("§bU"), Component.translatable("gui.polyjot.filter.unchanged")),
    TRANSLATED(Component.literal("§aT"), Component.translatable("gui.polyjot.filter.translated"));

    private final Component label;
    private final Component tooltip;

    FilterMode(Component label, Component tooltip) {
        this.label = label;
        this.tooltip = tooltip;
    }

    public Component getLabel() {
        return label;
    }

    public Component getTooltip() {
        return tooltip;
    }

    public FilterMode next() {
        FilterMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
