package net.polyjot.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TranslationStorage {
    private static final Logger LOGGER = LogManager.getLogger(TranslationStorage.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static Path getBaseDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("translations");
    }

    public static Path getLanguageDirectory(String targetLang) {
        return getBaseDirectory().resolve(targetLang);
    }

    public static Path getModFilePath(String targetLang, String modId) {
        return getLanguageDirectory(targetLang).resolve(modId + ".json");
    }

    public static Map<String, String> loadSavedTranslations(String targetLang, String modId) {
        Path filePath = getModFilePath(targetLang, modId);
        if (!Files.exists(filePath)) {
            return Collections.emptyMap();
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            Map<String, String> map = new LinkedHashMap<>();
            TranslationScanner.parseLangJson(reader, map);
            return map;
        } catch (Exception e) {
            LOGGER.error("Failed to load saved translations for mod {} at {}", modId, filePath, e);
            return Collections.emptyMap();
        }
    }

    public static boolean saveModTranslations(String targetLang, ModTranslationData data) {
        Path dir = getLanguageDirectory(targetLang);
        Path filePath = getModFilePath(targetLang, data.getModId());

        try {
            Files.createDirectories(dir);

            // sort: gameplay first, configs/keys last
            Map<String, String> gameplayKeys = new TreeMap<>();
            Map<String, String> otherKeys = new TreeMap<>();
            Map<String, String> noiseKeys = new TreeMap<>();

            for (TranslationEntry entry : data.getEntries().values()) {
                String val = entry.getCurrentTranslation();
                String key = entry.getKey();

                if (ModTranslationData.isNoiseKey(key)) {
                    noiseKeys.put(key, val);
                } else if (ModTranslationData.isGameplayKey(key)) {
                    gameplayKeys.put(key, val);
                } else {
                    otherKeys.put(key, val);
                }
            }

            Map<String, String> orderedOutput = new LinkedHashMap<>();
            orderedOutput.putAll(gameplayKeys);
            orderedOutput.putAll(otherKeys);
            orderedOutput.putAll(noiseKeys);

            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                GSON.toJson(orderedOutput, writer);
            }

            // mirror to live_pack so it loads on next launch
            try {
                Path livePackDir = getBaseDirectory().resolve("live_pack").resolve("assets").resolve(data.getModId()).resolve("lang");
                Files.createDirectories(livePackDir);
                Path liveLangFile = livePackDir.resolve(targetLang + ".json");
                try (Writer liveWriter = Files.newBufferedWriter(liveLangFile, StandardCharsets.UTF_8)) {
                    GSON.toJson(orderedOutput, liveWriter);
                }
            } catch (Exception liveEx) {
                LOGGER.warn("Could not sync live pack for mod {}", data.getModId(), liveEx);
            }

            LOGGER.info("Saved {} translations for mod {} to {}", orderedOutput.size(), data.getModId(), filePath);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save translations for mod {} to {}", data.getModId(), filePath, e);
            return false;
        }
    }

    public static boolean exportResourcePack(String targetLang, List<ModTranslationData> allMods, Path zipPath) {
        try {
            if (zipPath.getParent() != null) {
                Files.createDirectories(zipPath.getParent());
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
                JsonObject packMeta = new JsonObject();
                JsonObject packObj = new JsonObject();
                packObj.addProperty("pack_format", 15);
                packObj.addProperty("description", "PolyJot " + targetLang + " translations");
                packMeta.add("pack", packObj);

                ZipEntry mcmetaEntry = new ZipEntry("pack.mcmeta");
                zos.putNextEntry(mcmetaEntry);
                zos.write(GSON.toJson(packMeta).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                // only pack modified entries
                for (ModTranslationData modData : allMods) {
                    Map<String, String> customized = new LinkedHashMap<>();
                    for (TranslationEntry entry : modData.getEntries().values()) {
                        if (entry.isCustomized() && !entry.isUntranslated()) {
                            customized.put(entry.getKey(), entry.getCurrentTranslation());
                        }
                    }

                    if (customized.isEmpty()) {
                        continue;
                    }

                    String entryPath = "assets/" + modData.getModId() + "/lang/" + targetLang + ".json";
                    ZipEntry langEntry = new ZipEntry(entryPath);
                    zos.putNextEntry(langEntry);
                    zos.write(GSON.toJson(customized).getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            LOGGER.info("Successfully exported resource pack to {}", zipPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to export resource pack to {}", zipPath, e);
            return false;
        }
    }
}
