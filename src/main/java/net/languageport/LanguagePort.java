package net.languageport;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LanguagePort.MOD_ID)
public class LanguagePort {
    public static final String MOD_ID = "language_port";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public LanguagePort() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        // Register Config Screen factory for the Forge Mods menu button
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientConfigScreen);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Language Port common setup initialized.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Language Port client setup initialized.");
    }

    private void registerClientConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) -> new net.languageport.client.screen.ModSelectionScreen(parentScreen))
        );
    }
}
