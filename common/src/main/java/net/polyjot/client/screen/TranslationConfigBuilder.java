package net.polyjot.client.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.components.AbstractWidget;
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
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Translating: " + modData.getDisplayName() + " [" + modData.getModId() + "]"))
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
            ConfigCategory gameplayCat = builder.getOrCreateCategory(Component.literal("Gameplay (" + countTranslated(gameplayEntries) + "/" + gameplayEntries.size() + ")"));
            for (TranslationEntry entry : gameplayEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                gameplayCat.addEntry(listEntry);
            }
        }

        if (!otherEntries.isEmpty()) {
            ConfigCategory otherCat = builder.getOrCreateCategory(Component.literal("UI & Other (" + countTranslated(otherEntries) + "/" + otherEntries.size() + ")"));
            for (TranslationEntry entry : otherEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                otherCat.addEntry(listEntry);
            }
        }

        if (!noiseEntries.isEmpty()) {
            ConfigCategory noiseCat = builder.getOrCreateCategory(Component.literal("Configs & Keys (" + countTranslated(noiseEntries) + "/" + noiseEntries.size() + ")"));
            for (TranslationEntry entry : noiseEntries) {
                MultilineTranslationListEntry listEntry = createEntry(entry);
                allEntries.add(listEntry);
                noiseCat.addEntry(listEntry);
            }
        }

        if (!obsoleteEntries.isEmpty()) {
            ConfigCategory obsoleteCat = builder.getOrCreateCategory(Component.literal("Obsolete (" + obsoleteEntries.size() + ")"));
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
        });

        builder.setSavingRunnable(() -> {
            boolean saved = TranslationStorage.saveModTranslations(targetLang, modData);
            if (saved) {
                // hot-reload without game restart
                Map<String, String> activeMap = new LinkedHashMap<>();
                for (TranslationEntry entry : modData.getEntries().values()) {
                    if (entry.isCustomized() && !entry.isUntranslated()) {
                        activeMap.put(entry.getKey(), entry.getCurrentTranslation());
                    }
                }
                Services.PLATFORM.injectLanguage(activeMap);

                Minecraft.getInstance().getToasts().addToast(
                        SystemToast.multiline(
                                Minecraft.getInstance(),
                                SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                                Component.literal("PolyJot"),
                                Component.literal("Saved translations for " + modData.getDisplayName())
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
            Component warning = Component.literal(
                    "§eWarning: " + count + " entry/entries have missing placeholders (" + samples + ").\n" +
                    "§7You can still save, but this may cause in-game errors."
            );
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
