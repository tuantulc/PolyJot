package net.languageport.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TranslationScanner {
    private static final Logger LOGGER = LogManager.getLogger(TranslationScanner.class);
    private static final Gson GSON = new Gson();

    public static Map<String, ModTranslationData> scanAllMods(String targetLang) {
        Map<String, ModTranslationData> result = new LinkedHashMap<>();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        List<IModInfo> mods = ModList.get().getMods();
        for (IModInfo mod : mods) {
            String modId = mod.getModId();
            if ("minecraft".equals(modId)) {
                // Minecraft ships its own complete translations
                continue;
            }

            ModTranslationData modData = scanMod(resourceManager, modId, mod.getDisplayName(), targetLang);
            if (modData != null && modData.getTotalCount() > 0) {
                result.put(modId, modData);
            }
        }

        return result;
    }

    public static ModTranslationData scanMod(ResourceManager rm, String modId, String displayName, String targetLang) {
        Map<String, String> englishSource = loadModLang(rm, modId, "en_us");
        if (englishSource.isEmpty()) {
            return null;
        }

        Map<String, String> shippedTarget = loadModLang(rm, modId, targetLang);
        Map<String, String> savedCustom = TranslationStorage.loadSavedTranslations(targetLang, modId);

        ModTranslationData data = new ModTranslationData(modId, displayName);

        // 1. Process all keys from current English source
        for (Map.Entry<String, String> entry : englishSource.entrySet()) {
            String key = entry.getKey();
            String engVal = entry.getValue();

            String activeTranslation;
            boolean isCustomized;

            if (savedCustom.containsKey(key)) {
                activeTranslation = savedCustom.get(key);
                isCustomized = true;
            } else if (shippedTarget.containsKey(key)) {
                activeTranslation = shippedTarget.get(key);
                isCustomized = false;
            } else {
                activeTranslation = engVal;
                isCustomized = false;
            }

            TranslationEntry transEntry = new TranslationEntry(key, engVal, activeTranslation, isCustomized);
            data.addEntry(transEntry);
        }

        // 2. Identify obsolete keys (keys in saved file that are no longer in mod's current English source)
        for (Map.Entry<String, String> savedEntry : savedCustom.entrySet()) {
            String savedKey = savedEntry.getKey();
            if (!englishSource.containsKey(savedKey)) {
                TranslationEntry obsoleteEntry = new TranslationEntry(savedKey, "", savedEntry.getValue(), true);
                obsoleteEntry.setObsolete(true);
                data.addEntry(obsoleteEntry);
            }
        }

        return data;
    }

    private static Map<String, String> loadModLang(ResourceManager rm, String modId, String langCode) {
        String cleanCode = langCode.toLowerCase(Locale.ROOT);
        ResourceLocation loc = new ResourceLocation(modId, "lang/" + cleanCode + ".json");
        List<Resource> resources = rm.getResourceStack(loc);
        if (resources.isEmpty()) {
            // Also try fallback without lowercase in case mod shipped uppercase like en_US.json
            ResourceLocation fallbackLoc = new ResourceLocation(modId, "lang/" + langCode + ".json");
            resources = rm.getResourceStack(fallbackLoc);
        }
        if (resources.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> merged = new LinkedHashMap<>();
        for (Resource res : resources) {
            try (Reader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                if (map != null) {
                    merged.putAll(map);
                }
            } catch (Exception e) {
                LOGGER.warn("Could not read lang file {} for mod {}", loc, modId, e);
            }
        }
        return merged;
    }
}
