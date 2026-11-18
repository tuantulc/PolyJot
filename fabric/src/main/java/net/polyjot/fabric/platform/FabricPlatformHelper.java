package net.polyjot.fabric.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.polyjot.client.LanguageHotReloader;
import net.polyjot.fabric.mixin.ClientLanguageAccessor;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.IPlatformHelper;

import java.nio.file.Path;
import java.util.*;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<DiscoveredMod> getInstalledMods() {
        List<DiscoveredMod> list = new ArrayList<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            list.add(new FabricDiscoveredMod(container));
        }
        return list;
    }

    @Override
    public DiscoveredMod getMod(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(FabricDiscoveredMod::new)
                .orElse(null);
    }

    @Override
    public Map<String, String> getLanguageData() {
        ClientLanguage cl = LanguageHotReloader.findClientLanguage(Language.getInstance());
        if (cl instanceof ClientLanguageAccessor accessor) {
            return accessor.getStorage();
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean setClientLanguageStorage(ClientLanguage clientLanguage, Map<String, String> storage) {
        if (clientLanguage instanceof ClientLanguageAccessor accessor) {
            accessor.setStorage(storage);
            return true;
        }
        return false;
    }

    @Override
    public void injectLanguage(Map<String, String> translations) {
        LanguageHotReloader.applyTranslations(translations);
    }

    @Override
    public Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    private static class FabricDiscoveredMod implements DiscoveredMod {
        private final ModContainer container;

        public FabricDiscoveredMod(ModContainer container) {
            this.container = container;
        }

        @Override
        public String getModId() {
            return container.getMetadata().getId();
        }

        @Override
        public String getDisplayName() {
            return container.getMetadata().getName();
        }

        @Override
        public Path findResource(String... pathParts) {
            if (pathParts == null || pathParts.length == 0) {
                return null;
            }
            String joinedPath = String.join("/", pathParts);
            Optional<Path> pathOpt = container.findPath(joinedPath);
            return pathOpt.orElse(null);
        }
    }
}
