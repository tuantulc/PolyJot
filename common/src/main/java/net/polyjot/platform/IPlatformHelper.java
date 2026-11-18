package net.polyjot.platform;

import net.minecraft.client.resources.language.ClientLanguage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface IPlatformHelper {
    /**
     * Name of the loader platform (e.g. "Forge", "Fabric", "NeoForge").
     */
    String getPlatformName();

    /**
     * Checks if a mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * Returns all installed mods.
     */
    List<DiscoveredMod> getInstalledMods();

    /**
     * Gets a loaded mod by ID, or null if not found.
     */
    DiscoveredMod getMod(String modId);

    /**
     * Gets active language key-value mappings from Minecraft.
     */
    Map<String, String> getLanguageData();

    /**
     * Updates the backing storage map of an existing ClientLanguage instance in-place.
     */
    boolean setClientLanguageStorage(ClientLanguage clientLanguage, Map<String, String> storage);

    /**
     * Injects updated language mappings straight into Minecraft's memory.
     */
    void injectLanguage(Map<String, String> translations);

    /**
     * Gets the game root directory (.minecraft).
     */
    Path getGameDirectory();
}
