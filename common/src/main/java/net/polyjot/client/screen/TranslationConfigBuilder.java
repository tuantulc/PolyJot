package net.polyjot.client.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationEntry;
import net.polyjot.data.TranslationStorage;
import net.polyjot.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class TranslationConfigBuilder {

    public static Screen createScreen(Screen parent, ModTranslationData modData, String targetLang) {
        return createScreen(parent, modData, targetLang, null);
    }

    public static Screen createScreen(Screen parent, ModTranslationData modData, String targetLang, String initialSearch) {
        FilterableSearchFieldEntry.setCurrentFilter(FilterMode.ALL);
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("gui.polyjot.editor_title", modData.getDisplayName(), modData.getModId()))
                .setDoesConfirmSave(true);

        List<TranslationEntry> gameplayEntries = new ArrayList<>();
        List<TranslationEntry> otherEntries = new ArrayList<>();
        List<TranslationEntry> noiseEntries = new ArrayList<>();
        List<TranslationEntry> obsoleteEntries = new ArrayList<>();

        for (TranslationEntry entry : modData.getEntries().values()) {
            if (entry.isObsolete()) {
                obsoleteEntries.add(entry);
            } else if (ModTranslationData.isNoiseKey(entry.getKey())) {
                noiseEntries.add(entry);
            } else if (ModTranslationData.isGameplayKey(entry.getKey())) {
                gameplayEntries.add(entry);
            } else {
                otherEntries.add(entry);
            }
        }

        List<MultilineTranslationListEntry> allEntries = new ArrayList<>();

        if (!gameplayEntries.isEmpty()) {
            ConfigCategory gameplayCat = builder.getOrCreateCategory(Component.translatable("gui.polyjot.category.gameplay", countTranslated(gameplayEntries), gameplayEntries.size()));
            for (TranslationEntry entry : gameplayEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                gameplayCat.addEntry(listEntry);
            }
        }

        if (!otherEntries.isEmpty()) {
            ConfigCategory otherCat = builder.getOrCreateCategory(Component.translatable("gui.polyjot.category.ui_other", countTranslated(otherEntries), otherEntries.size()));
            for (TranslationEntry entry : otherEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                otherCat.addEntry(listEntry);
            }
        }

        if (!noiseEntries.isEmpty()) {
            ConfigCategory noiseCat = builder.getOrCreateCategory(Component.translatable("gui.polyjot.category.configs_keys", countTranslated(noiseEntries), noiseEntries.size()));
            for (TranslationEntry entry : noiseEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                noiseCat.addEntry(listEntry);
            }
        }

        if (!obsoleteEntries.isEmpty()) {
            ConfigCategory obsoleteCat = builder.getOrCreateCategory(Component.translatable("gui.polyjot.category.obsolete", obsoleteEntries.size()));
            for (TranslationEntry entry : obsoleteEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                obsoleteCat.addEntry(listEntry);
            }
        }

        builder.setAfterInitConsumer(screen -> {
            AbstractWidget saveButton = null;
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.getY() == screen.height - 26 && widget.getX() > screen.width / 2) {
                        saveButton = widget;
                        break;
                    }
                }
            }

            final AbstractWidget finalSaveBtn = saveButton;
            Runnable refreshSaveTooltip = () -> updateSaveButtonWarning(finalSaveBtn, allEntries);

            for (MultilineTranslationListEntry listEntry : allEntries) {
                listEntry.setOnValueChangeListener(refreshSaveTooltip);
            }

            refreshSaveTooltip.run();

            if (screen instanceof ClothConfigScreen clothScreen && clothScreen.listWidget != null) {
                try {
                    java.lang.reflect.Field entriesField = null;
                    Class<?> c = clothScreen.listWidget.getClass();
                    while (c != null && c != Object.class) {
                        try {
                            entriesField = c.getDeclaredField("entries");
                            entriesField.setAccessible(true);
                            break;
                        } catch (NoSuchFieldException e) {
                            c = c.getSuperclass();
                        }
                    }
                    if (entriesField != null) {
                        @SuppressWarnings("rawtypes")
                        List rawList = (List) entriesField.get(clothScreen.listWidget);
                        for (int i = 0; i < rawList.size(); i++) {
                            if (rawList.get(i) instanceof SearchFieldEntry) {
                                FilterableSearchFieldEntry customSearch = new FilterableSearchFieldEntry(clothScreen, clothScreen.listWidget);
                                if (initialSearch != null && !initialSearch.isEmpty() && customSearch.getEditBox() != null) {
                                    customSearch.getEditBox().setValue(initialSearch);
                                }
                                rawList.set(i, customSearch);
                                for (java.lang.reflect.Field f : ClothConfigScreen.class.getDeclaredFields()) {
                                    if (SearchFieldEntry.class.isAssignableFrom(f.getType())) {
                                        f.setAccessible(true);
                                        f.set(clothScreen, customSearch);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });

        builder.setSavingRunnable(() -> {
            boolean saved = TranslationStorage.saveModTranslations(targetLang, modData);
            if (saved) {
                // hot-reload without game restart (includes edits and resets)
                Map<String, String> currentActive = Services.PLATFORM.getLanguageData();
                Map<String, String> activeMap = new LinkedHashMap<>();
                for (TranslationEntry entry : modData.getEntries().values()) {
                    if (entry.isObsolete()) {
                        continue;
                    }
                    String newVal = entry.getCurrentTranslation();
                    String existingVal = currentActive.get(entry.getKey());
                    if (entry.isCustomized() && !entry.isUntranslated()) {
                        activeMap.put(entry.getKey(), newVal);
                    } else if (existingVal != null && !existingVal.equals(newVal)) {
                        activeMap.put(entry.getKey(), newVal);
                    }
                }
                Services.PLATFORM.injectLanguage(activeMap);

                Minecraft.getInstance().getToasts().addToast(
                        SystemToast.multiline(
                                Minecraft.getInstance(),
                                SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                                Component.translatable("polyjot.name"),
                                Component.translatable("toast.polyjot.save_success", modData.getDisplayName(), modData.getModId())
                        )
                );
            }
        });

        return builder.build();
    }

    private static void updateSaveButtonWarning(AbstractWidget saveButton, List<MultilineTranslationListEntry> allEntries) {
        if (saveButton == null) return;
        int count = 0;
        List<String> missingSamples = new ArrayList<>();
        for (MultilineTranslationListEntry entry : allEntries) {
            List<String> missing = entry.getMissingPlaceholders();
            if (!missing.isEmpty()) {
                count++;
                for (String m : missing) {
                    if (!missingSamples.contains(m) && missingSamples.size() < 3) {
                        missingSamples.add(m);
                    }
                }
            }
        }

        if (count > 0) {
            String samples = String.join(", ", missingSamples);
            Component warning = Component.translatable("gui.polyjot.warning.save_missing_placeholders", count, samples);
            saveButton.setTooltip(Tooltip.create(warning));
        } else {
            saveButton.setTooltip(null);
        }
    }

    private static MultilineTranslationListEntry createEntry(TranslationEntry entry) {
        Component fieldTitle;
        if (!entry.getSourceEnglish().isEmpty() && !entry.getSourceEnglish().equalsIgnoreCase(entry.getKey())) {
            fieldTitle = Component.literal(entry.getSourceEnglish() + "\n§8" + entry.getKey());
        } else {
            fieldTitle = Component.literal(entry.getKey());
        }

        MultilineTranslationListEntry listEntry = new MultilineTranslationListEntry(
                fieldTitle,
                entry,
                Component.translatable("text.cloth-config.reset_value"),
                newVal -> entry.setCurrentTranslation(newVal),
                false
        );

        List<String> tags = new ArrayList<>();
        tags.add(entry.getKey());
        if (!entry.getSourceEnglish().isEmpty()) {
            tags.add(entry.getSourceEnglish());
        }
        listEntry.appendSearchTags(tags);

        return listEntry;
    }

    private static int countTranslated(List<TranslationEntry> list) {
        int count = 0;
        for (TranslationEntry entry : list) {
            if (!entry.isUntranslated()) {
                count++;
            }
        }
        return count;
    }
}
