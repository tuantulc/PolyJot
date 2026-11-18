package net.polyjot.neoforge.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.polyjot.PolyJotConstants;
import net.polyjot.data.TranslationStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@EventBusSubscriber(modid = PolyJotConstants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class DynamicTranslationPack {
    private static final String PACK_ID = "polyjot_live_translations";

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path translationsBase = TranslationStorage.getBaseDirectory();
            Path livePackDir = translationsBase.resolve("live_pack");

            try {
                Files.createDirectories(livePackDir.resolve("assets"));
                Path mcmeta = livePackDir.resolve("pack.mcmeta");
                Files.writeString(mcmeta, """
                    {
                      "pack": {
                        "description": "PolyJot live translations",
                        "pack_format": 34
                      }
                    }
                    """);
            } catch (IOException e) {
                PolyJotConstants.LOGGER.error("Failed to initialize live translation pack directory", e);
                return;
            }

            RepositorySource source = packConsumer -> {
                PackLocationInfo locationInfo = new PackLocationInfo(
                        PACK_ID,
                        Component.literal("PolyJot (Live Translations)"),
                        PackSource.BUILT_IN,
                        Optional.empty()
                );
                PackSelectionConfig selectionConfig = new PackSelectionConfig(
                        true,
                        Pack.Position.TOP,
                        false
                );
                Pack pack = Pack.readMetaAndCreate(
                        locationInfo,
                        new PathPackResources.PathResourcesSupplier(livePackDir),
                        PackType.CLIENT_RESOURCES,
                        selectionConfig
                );
                if (pack != null) {
                    packConsumer.accept(pack);
                }
            };

            event.addRepositorySource(source);
        }
    }
}
