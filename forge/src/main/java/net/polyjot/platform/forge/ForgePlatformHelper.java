package net.polyjot.platform.forge;

import net.minecraft.locale.Language;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.polyjot.client.LanguageHotReloader;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.IPlatformHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public List<DiscoveredMod> getInstalledMods() {
        List<DiscoveredMod> result = new ArrayList<>();
        for (IModInfo mod : ModList.get().getMods()) {
            result.add(new ForgeDiscoveredMod(mod));
        }
        return result;
    }

    @Override
    public DiscoveredMod getMod(String modId) {
        for (IModInfo mod : ModList.get().getMods()) {
            if (mod.getModId().equalsIgnoreCase(modId)) {
                return new ForgeDiscoveredMod(mod);
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getLanguageData() {
        return Language.getInstance().getLanguageData();
    }

    @Override
    public void injectLanguage(Map<String, String> translations) {
        LanguageHotReloader.applyTranslations(translations);
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    private static class ForgeDiscoveredMod implements DiscoveredMod {
        private final IModInfo modInfo;

        public ForgeDiscoveredMod(IModInfo modInfo) {
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
