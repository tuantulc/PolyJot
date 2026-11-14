package net.polyjot.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TranslationScanner {
    private static final Logger LOGGER = LogManager.getLogger(TranslationScanner.class);

    public static Map<String, ModTranslationData> scanAllMods(String targetLang) {
        Map<String, ModTranslationData> result = new LinkedHashMap<>();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        List<IModInfo> mods = ModList.get().getMods();
        for (IModInfo mod : mods) {
            String modId = mod.getModId();
            if ("minecraft".equals(modId)) {
                // skip vanilla
                continue;
            }

            ModTranslationData modData = scanMod(resourceManager, mod, targetLang);
            if (modData != null && modData.getTotalCount() > 0) {
                result.put(modId, modData);
            }
        }

        return result;
    }

    public static ModTranslationData scanMod(ResourceManager rm, String modId, String displayName, String targetLang) {
        IModInfo foundInfo = null;
        for (IModInfo mod : ModList.get().getMods()) {
            if (mod.getModId().equalsIgnoreCase(modId)) {
                foundInfo = mod;
                break;
            }
        }
        if (foundInfo != null) {
            return scanMod(rm, foundInfo, targetLang);
        }
        return null;
    }

    public static ModTranslationData scanMod(ResourceManager rm, IModInfo mod, String targetLang) {
        String modId = mod.getModId();
        String displayName = mod.getDisplayName();

        Map<String, String> englishSource = loadModLang(rm, mod, "en_us");
        if (englishSource.isEmpty()) {
            return null;
        }

        Map<String, String> shippedTarget = loadModLang(rm, mod, targetLang);
        Map<String, String> savedCustom = TranslationStorage.loadSavedTranslations(targetLang, modId);

        ModTranslationData data = new ModTranslationData(modId, displayName);

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

        // check for removed/renamed keys
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

    private static Map<String, String> loadModLang(ResourceManager rm, IModInfo mod, String langCode) {
        String modId = mod.getModId();
        String cleanCode = langCode.toLowerCase(Locale.ROOT);
        Map<String, String> merged = new LinkedHashMap<>();

        // check resource packs first
        try {
            ResourceLocation loc = new ResourceLocation(modId, "lang/" + cleanCode + ".json");
            List<Resource> resources = rm.getResourceStack(loc);
            if (resources.isEmpty()) {
                ResourceLocation fallbackLoc = new ResourceLocation(modId, "lang/" + langCode + ".json");
                resources = rm.getResourceStack(fallbackLoc);
            }
            for (Resource res : resources) {
                try (Reader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    parseLangJson(reader, merged);
                } catch (Exception e) {
                    LOGGER.warn("Could not read lang resource {} for mod {}: {}", loc, modId, e.getMessage());
                }
            }
        } catch (Exception ignored) {
        }

        // fallback: inspect jar directly
        if (merged.isEmpty()) {
            try {
                IModFile modFile = (mod.getOwningFile() != null) ? mod.getOwningFile().getFile() : null;
                if (modFile != null) {
                    Path langPath = modFile.findResource("assets", modId, "lang", cleanCode + ".json");
                    if (langPath == null || !Files.exists(langPath)) {
                        langPath = modFile.findResource("assets", modId, "lang", langCode + ".json");
                    }
                    if (langPath != null && Files.exists(langPath)) {
                        try (Reader reader = Files.newBufferedReader(langPath, StandardCharsets.UTF_8)) {
                            parseLangJson(reader, merged);
                        }
                    } else {
                        // check any other namespaces in the jar
                        Path assetsDir = modFile.findResource("assets");
                        if (assetsDir != null && Files.isDirectory(assetsDir)) {
                            try (Stream<Path> stream = Files.list(assetsDir)) {
                                stream.filter(Files::isDirectory).forEach(subNs -> {
                                    Path subLangPath = subNs.resolve("lang/" + cleanCode + ".json");
                                    if (!Files.exists(subLangPath)) {
                                        subLangPath = subNs.resolve("lang/" + langCode + ".json");
                                    }
                                    if (Files.exists(subLangPath)) {
                                        try (Reader reader = Files.newBufferedReader(subLangPath, StandardCharsets.UTF_8)) {
                                            parseLangJson(reader, merged);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Direct JAR inspection failed for mod {}: {}", modId, e.getMessage());
            }
        }

        return merged;
    }

    public static void parseLangJson(Reader reader, Map<String, String> outMap) {
        try {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    JsonElement val = entry.getValue();
                    if (val != null && val.isJsonPrimitive()) {
                        outMap.put(entry.getKey(), val.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse lang JSON: {}", e.getMessage());
        }
    }
}
