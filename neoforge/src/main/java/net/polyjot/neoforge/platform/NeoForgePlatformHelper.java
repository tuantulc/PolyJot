package net.polyjot.neoforge.platform;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.polyjot.client.LanguageHotReloader;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.IPlatformHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public List<DiscoveredMod> getInstalledMods() {
        List<DiscoveredMod> result = new ArrayList<>();
        for (IModInfo mod : ModList.get().getMods()) {
            result.add(new NeoForgeDiscoveredMod(mod));
        }
        return result;
    }

    @Override
    public DiscoveredMod getMod(String modId) {
        for (IModInfo mod : ModList.get().getMods()) {
            if (mod.getModId().equalsIgnoreCase(modId)) {
                return new NeoForgeDiscoveredMod(mod);
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getLanguageData() {
        ClientLanguage cl = LanguageHotReloader.findClientLanguage(Language.getInstance());
        if (cl != null) {
            return cl.getLanguageData();
        }
        return Language.getInstance().getLanguageData();
    }

    @Override
    public boolean setClientLanguageStorage(ClientLanguage clientLanguage, Map<String, String> storage) {
        try {
            java.lang.reflect.Field storageField = ClientLanguage.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            storageField.set(clientLanguage, storage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void injectLanguage(Map<String, String> translations) {
        LanguageHotReloader.applyTranslations(translations);
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    private static class NeoForgeDiscoveredMod implements DiscoveredMod {
        private final IModInfo modInfo;

        public NeoForgeDiscoveredMod(IModInfo modInfo) {
            this.modInfo = modInfo;
        }

        @Override
        public String getModId() {
            return modInfo.getModId();
        }

        @Override
        public String getDisplayName() {
            return modInfo.getDisplayName();
        }

        @Override
        public Path findResource(String... pathParts) {
            if (modInfo.getOwningFile() == null || modInfo.getOwningFile().getFile() == null) {
                return null;
            }
            IModFile file = modInfo.getOwningFile().getFile();
            return file.findResource(pathParts);
        }
    }
}
