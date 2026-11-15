package net.polyjot.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.polyjot.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class LanguageHotReloader {
    private static final Logger LOGGER = LogManager.getLogger(LanguageHotReloader.class);

    public static void applyTranslations(Map<String, String> updatedTranslations) {
        if (updatedTranslations == null || updatedTranslations.isEmpty()) {
            return;
        }

        try {
            Language currentLang = Language.getInstance();
            Map<String, String> currentData = Services.PLATFORM.getLanguageData();

            Map<String, String> mergedData = new HashMap<>(currentData);
            mergedData.putAll(updatedTranslations);

            boolean isRtl = currentLang.isDefaultRightToLeft();

            // try injecting into client language first
            try {
                Constructor<ClientLanguage> constructor = ClientLanguage.class.getDeclaredConstructor(Map.class, boolean.class);
                constructor.setAccessible(true);
                ClientLanguage newClientLang = constructor.newInstance(mergedData, isRtl);
                Language.inject(newClientLang);
                LOGGER.info("Successfully hot-injected {} translations into ClientLanguage.", updatedTranslations.size());
                return;
            } catch (Exception reflectionEx) {
                LOGGER.warn("Reflection instantiation of ClientLanguage failed, using fallback Language wrapper", reflectionEx);
            }

            // fallback or smth
            final ImmutableMap<String, String> immutableStorage = ImmutableMap.copyOf(mergedData);
            Language customLang = new Language() {
                @Override
                public String getOrDefault(String key, String defaultVal) {
                    return immutableStorage.getOrDefault(key, defaultVal);
                }

                @Override
                public boolean has(String key) {
                    return immutableStorage.containsKey(key);
                }

                @Override
                public boolean isDefaultRightToLeft() {
                    return isRtl;
                }

                @Override
                public FormattedCharSequence getVisualOrder(FormattedText text) {
                    return FormattedBidiReorder.reorder(text, isRtl);
                }

                public Map<String, String> getLanguageData() {
                    return immutableStorage;
                }
            };

            Language.inject(customLang);
            LOGGER.info("Successfully hot-injected {} translations via fallback wrapper.", updatedTranslations.size());
        } catch (Exception e) {
            LOGGER.error("Failed to hot-inject translations", e);
        }
    }
}
