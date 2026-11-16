package net.polyjot.data;

import java.util.*;

public class ModTranslationData {
    public enum FilterMode {
        ALL,
        UNTRANSLATED,
        TRANSLATED,
        UPDATED,
        OBSOLETE
    }

    private static final Set<String> GAMEPLAY_PREFIXES = Set.of(
            "item.", "block.", "entity.", "effect.", "mob_effect.",
            "enchantment.", "advancement.", "biome.", "dimension.",
            "attribute.", "death.", "skill.", "spell.", "weapon.",
            "armor.", "food.", "potion.", "tooltip.", "curios.", "trinket."
    );

    private static final Set<String> NOISE_PREFIXES = Set.of(
            "key.", "cloth-client-forge-", "text.autoconfig.", "config.",
            "jei.", "rei.", "emi.", "cuiosconfig."
    );

    private final String modId;
    private final String displayName;
    private final Map<String, TranslationEntry> entries = new LinkedHashMap<>();

    public ModTranslationData(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName != null && !displayName.isEmpty() ? displayName : modId;
    }

    public String getModId() {
        return modId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Map<String, TranslationEntry> getEntries() {
        return entries;
    }

    public void addEntry(TranslationEntry entry) {
        this.entries.put(entry.getKey(), entry);
    }

    public int getTotalCount() {
        return entries.size();
    }

    public int getTranslatedCount() {
        int count = 0;
        for (TranslationEntry entry : entries.values()) {
            if (!entry.isUntranslated()) {
                count++;
            }
        }
        return count;
    }

    public int getUntranslatedCount() {
        return getTotalCount() - getTranslatedCount();
    }

    public float getCompletionPercentage() {
        if (entries.isEmpty()) {
            return 0.0f;
        }
        return ((float) getTranslatedCount() / entries.size()) * 100.0f;
    }

    public List<TranslationEntry> filter(String query, FilterMode mode, boolean gameplayOnly) {
        List<TranslationEntry> result = new ArrayList<>();
        String lowerQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";

        for (TranslationEntry entry : entries.values()) {
            if (gameplayOnly && isNoiseKey(entry.getKey())) {
                continue;
            }

            switch (mode) {
                case UNTRANSLATED -> {
                    if (!entry.isUntranslated()) continue;
                }
                case TRANSLATED -> {
                    if (entry.isUntranslated()) continue;
                }
                case UPDATED -> {
                    if (!entry.isEnglishModified()) continue;
                }
                case OBSOLETE -> {
                    if (!entry.isObsolete()) continue;
                }
                case ALL -> {}
            }

            if (!lowerQuery.isEmpty()) {
                boolean matchesKey = entry.getKey().toLowerCase(Locale.ROOT).contains(lowerQuery);
                boolean matchesSource = entry.getSourceEnglish().toLowerCase(Locale.ROOT).contains(lowerQuery);
                boolean matchesTarget = entry.getCurrentTranslation().toLowerCase(Locale.ROOT).contains(lowerQuery);

                if (!matchesKey && !matchesSource && !matchesTarget) {
                    continue;
                }
            }

            result.add(entry);
        }

        return result;
    }

    public static boolean isGameplayKey(String key) {
        for (String prefix : GAMEPLAY_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNoiseKey(String key) {
        for (String prefix : NOISE_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
