package net.polyjot.pack;

import net.polyjot.PolyJot;
import net.polyjot.data.TranslationStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = PolyJot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
                PolyJot.LOGGER.error("Failed to initialize live translation pack directory", e);
                return;
            }

            RepositorySource source = packConsumer -> {
                Pack pack = Pack.readMetaAndCreate(
                        PACK_ID,
                        Component.literal("PolyJot (Live Translations)"),
                        false,
                        (id) -> new PathPackResources(id, livePackDir, true),
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP,
                        PackSource.BUILT_IN
                );
                if (pack != null) {
                    packConsumer.accept(pack);
                }
            };

            event.addRepositorySource(source);
        }
    }
}
