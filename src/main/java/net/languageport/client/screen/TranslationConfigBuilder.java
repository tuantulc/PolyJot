package net.languageport.client.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import net.languageport.client.LanguageHotReloader;
import net.languageport.data.ModTranslationData;
import net.languageport.data.TranslationEntry;
import net.languageport.data.TranslationStorage;
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

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // 1. Group entries into categories
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

        // 2. Build Gameplay Category
        if (!gameplayEntries.isEmpty()) {
            ConfigCategory gameplayCat = builder.getOrCreateCategory(Component.literal("Gameplay (" + countTranslated(gameplayEntries) + "/" + gameplayEntries.size() + ")"));
            for (TranslationEntry entry : gameplayEntries) {
                gameplayCat.addEntry(createEntry(entry));
            }
        }

        // 3. Build UI & Other Category
        if (!otherEntries.isEmpty()) {
            ConfigCategory otherCat = builder.getOrCreateCategory(Component.literal("UI & Other (" + countTranslated(otherEntries) + "/" + otherEntries.size() + ")"));
            for (TranslationEntry entry : otherEntries) {
                otherCat.addEntry(createEntry(entry));
            }
        }

        // 4. Build Noise Category (Configs, Keys, Debug)
        if (!noiseEntries.isEmpty()) {
            ConfigCategory noiseCat = builder.getOrCreateCategory(Component.literal("Configs & Keys (" + countTranslated(noiseEntries) + "/" + noiseEntries.size() + ")"));
            for (TranslationEntry entry : noiseEntries) {
                noiseCat.addEntry(createEntry(entry));
            }
        }

        // 5. Build Obsolete Category if any exist
        if (!obsoleteEntries.isEmpty()) {
            ConfigCategory obsoleteCat = builder.getOrCreateCategory(Component.literal("⚠️ Obsolete (" + obsoleteEntries.size() + ")"));
            for (TranslationEntry entry : obsoleteEntries) {
                obsoleteCat.addEntry(createEntry(entry));
            }
        }

        // 6. Save & Apply Action
        builder.setSavingRunnable(() -> {
            boolean saved = TranslationStorage.saveModTranslations(targetLang, modData);
            if (saved) {
                // Hot inject active translations into memory immediately
                Map<String, String> activeMap = new LinkedHashMap<>();
                for (TranslationEntry entry : modData.getEntries().values()) {
                    if (entry.isCustomized() && !entry.isUntranslated()) {
                        activeMap.put(entry.getKey(), entry.getCurrentTranslation());
                    }
                }
                LanguageHotReloader.applyTranslations(activeMap);

                // Show in-game toast notification
                Minecraft.getInstance().getToasts().addToast(
                        SystemToast.multiline(
                                Minecraft.getInstance(),
                                SystemToast.SystemToastIds.NARRATOR_TOGGLE,
                                Component.literal("Language Port"),
                                Component.literal("Saved translations for " + modData.getDisplayName())
                        )
                );
            }
        });

        return builder.build();
    }

    private static MultilineTranslationListEntry createEntry(TranslationEntry entry) {
        Component fieldTitle;
        if (!entry.getSourceEnglish().isEmpty() && !entry.getSourceEnglish().equalsIgnoreCase(entry.getKey())) {
            fieldTitle = Component.literal(entry.getSourceEnglish() + "\n§8" + entry.getKey());
        } else {
            fieldTitle = Component.literal(entry.getKey());
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(Component.literal("§7Key: §f" + entry.getKey()));
        if (!entry.getSourceEnglish().isEmpty()) {
            tooltips.add(Component.literal("§7Original English: §e" + entry.getSourceEnglish()));
        }
        if (entry.isObsolete()) {
            tooltips.add(Component.literal("§c⚠️ This key was removed or renamed in the current mod version."));
        }

        MultilineTranslationListEntry listEntry = new MultilineTranslationListEntry(
                fieldTitle,
                entry.getCurrentTranslation(),
                Component.translatable("text.cloth-config.reset_value"),
                () -> entry.getSourceEnglish(),
                newVal -> entry.setCurrentTranslation(newVal),
                () -> Optional.of(tooltips.toArray(new Component[0])),
                false
        );

        // Add comprehensive search tags so searching by key, token, or name matches instantly
        List<String> tags = new ArrayList<>();
        tags.add(entry.getKey());
        tags.add(entry.getKey().toLowerCase(Locale.ROOT));
        for (String token : entry.getKey().split("[._:]")) {
            if (!token.isEmpty()) {
                tags.add(token.toLowerCase(Locale.ROOT));
            }
        }
        if (!entry.getSourceEnglish().isEmpty()) {
            tags.add(entry.getSourceEnglish());
            tags.add(entry.getSourceEnglish().toLowerCase(Locale.ROOT));
        }
        if (!entry.getCurrentTranslation().isEmpty()) {
            tags.add(entry.getCurrentTranslation());
            tags.add(entry.getCurrentTranslation().toLowerCase(Locale.ROOT));
        }
        listEntry.appendSearchTags(tags);

        listEntry.setCustomErrorSupplier(text -> {
            if (text != null && (text.length() > 180 || entry.getKey().length() > 180)) {
                return Optional.of(Component.literal("Text is too long for comfortable in-game editing. We recommend editing this entry directly in the JSON file."));
            }
            if (entry.hasMissingPlaceholders()) {
                return Optional.of(Component.literal("Missing placeholder: " + entry.getMissingPlaceholders() + " — saving this may cause tooltip errors!"));
            }
            return Optional.empty();
        });

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
