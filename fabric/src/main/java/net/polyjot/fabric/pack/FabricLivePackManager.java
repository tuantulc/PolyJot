package net.polyjot.fabric.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.polyjot.PolyJotConstants;
import net.polyjot.data.TranslationStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FabricLivePackManager {
    public static final String PACK_ID = "polyjot_live_translations";

    public static RepositorySource createRepositorySource() {
        Path translationsBase = TranslationStorage.getBaseDirectory();
        Path livePackDir = translationsBase.resolve("live_pack");

        try {
            Files.createDirectories(livePackDir.resolve("assets"));
            Path mcmeta = livePackDir.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                Files.writeString(mcmeta, """
                    {
                      "pack": {
                        "description": "PolyJot live translations",
                        "pack_format": 15
                      }
                    }
                    """);
            }
        } catch (IOException e) {
            PolyJotConstants.LOGGER.error("Failed to initialize live translation pack directory", e);
            return packConsumer -> {};
        }

        return packConsumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.literal("PolyJot (Live Translations)"),
                    true,
                    (id) -> new PathPackResources(id, livePackDir, true),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                packConsumer.accept(pack);
            }
        };
    }
}
