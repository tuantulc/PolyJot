package net.languageport.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationEntry {
    private static final int MAX_COMFORTABLE_LENGTH = 180;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?:\\d+\\$)?[sdf]|%\\.\\d+f|\\{\\d+\\}");

    private final String key;
    private final String sourceEnglish;
    private String currentTranslation;
    private boolean customized;
    private boolean obsolete;
    private boolean englishModified;

    public TranslationEntry(String key, String sourceEnglish, String currentTranslation, boolean customized) {
        this.key = key;
        this.sourceEnglish = sourceEnglish != null ? sourceEnglish : "";
        this.currentTranslation = currentTranslation != null ? currentTranslation : this.sourceEnglish;
        this.customized = customized;
        this.obsolete = false;
        this.englishModified = false;
    }

    public String getKey() {
        return key;
    }

    public String getSourceEnglish() {
        return sourceEnglish;
    }

    public String getCurrentTranslation() {
        return currentTranslation;
    }

    public void setCurrentTranslation(String translation) {
        this.currentTranslation = translation != null ? translation : "";
        this.customized = !this.currentTranslation.equals(this.sourceEnglish);
    }

    public boolean isCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public boolean isEnglishModified() {
        return englishModified;
    }

    public void setEnglishModified(boolean englishModified) {
        this.englishModified = englishModified;
    }

    public boolean isUntranslated() {
        return !customized || currentTranslation.isEmpty() || currentTranslation.equals(sourceEnglish);
    }

    public boolean isTooLong() {
        return (sourceEnglish != null && sourceEnglish.length() > MAX_COMFORTABLE_LENGTH) ||
               (currentTranslation != null && currentTranslation.length() > MAX_COMFORTABLE_LENGTH) ||
               (key != null && key.length() > MAX_COMFORTABLE_LENGTH);
    }

    public List<String> getMissingPlaceholders() {
        List<String> sourceTokens = extractPlaceholders(sourceEnglish);
        List<String> targetTokens = extractPlaceholders(currentTranslation);
        List<String> missing = new ArrayList<>();

        for (String token : sourceTokens) {
            if (!targetTokens.contains(token)) {
                missing.add(token);
            }
        }
        return missing;
    }

    public boolean hasMissingPlaceholders() {
        return !getMissingPlaceholders().isEmpty();
    }

    private static List<String> extractPlaceholders(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
