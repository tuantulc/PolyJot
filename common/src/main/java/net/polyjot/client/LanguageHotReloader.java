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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class LanguageHotReloader {
    private static final Logger LOGGER = LogManager.getLogger(LanguageHotReloader.class);

    public static ClientLanguage findClientLanguage(Language root) {
        if (root == null) {
            return null;
        }
        if (root instanceof ClientLanguage cl) {
            return cl;
        }

        // Unwrap delegating Language wrappers (e.g. server-translations-api SystemDelegatedLanguage)
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Object> queue = new ArrayDeque<>();
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            Object current = queue.poll();
            Class<?> clazz = current.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object val = field.get(current);
                        if (val instanceof ClientLanguage cl) {
                            return cl;
                        }
                        if (val instanceof Language && visited.add(val)) {
                            queue.add(val);
                        }
                    } catch (Exception ignored) {
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static void applyTranslations(Map<String, String> updatedTranslations) {
        if (updatedTranslations == null || updatedTranslations.isEmpty()) {
            return;
        }

        try {
            Language currentLang = Language.getInstance();
            ClientLanguage clientLang = findClientLanguage(currentLang);
            Map<String, String> currentData = Services.PLATFORM.getLanguageData();

            Map<String, String> mergedData = new HashMap<>(currentData);
            mergedData.putAll(updatedTranslations);
            ImmutableMap<String, String> immutableStorage = ImmutableMap.copyOf(mergedData);

            // Update existing ClientLanguage in-place so mod mixin fields (e.g. owo$textMap) stay intact
            if (clientLang != null && Services.PLATFORM.setClientLanguageStorage(clientLang, immutableStorage)) {
                clearOwoTextCache(clientLang, updatedTranslations.keySet());
                LOGGER.info("Successfully hot-injected {} translations in-place into ClientLanguage.", updatedTranslations.size());
                return;
            }

            // Fallback wrapper if no ClientLanguage instance could be mutated
            boolean isRtl = currentLang.isDefaultRightToLeft();
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

    private static void clearOwoTextCache(ClientLanguage clientLang, Set<String> editedKeys) {
        try {
            Field owoField = clientLang.getClass().getDeclaredField("owo$textMap");
            owoField.setAccessible(true);
            Object val = owoField.get(clientLang);
            if (val instanceof Map<?, ?> map && !map.isEmpty()) {
                boolean anyPresent = false;
                for (String key : editedKeys) {
                    if (map.containsKey(key)) {
                        anyPresent = true;
                        break;
                    }
                }
                if (anyPresent) {
                    Map<Object, Object> copy = new HashMap<>(map);
                    for (String key : editedKeys) {
                        copy.remove(key);
                    }
                    owoField.set(clientLang, copy);
                }
            }
        } catch (NoSuchFieldException ignored) {
            // owo-lib not installed
        } catch (Exception e) {
            LOGGER.debug("Could not update owo$textMap cache: {}", e.getMessage());
        }
    }
}
