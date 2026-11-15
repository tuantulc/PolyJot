package net.polyjot;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PolyJotConstants.MOD_ID)
public class PolyJot {
    public static final String MOD_ID = PolyJotConstants.MOD_ID;
    public static final Logger LOGGER = PolyJotConstants.LOGGER;

    public PolyJot() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        // marks this mod as purely client-side so clients can join servers without it
        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.fml.IExtensionPoint.DisplayTest.class,
                () -> new net.minecraftforge.fml.IExtensionPoint.DisplayTest(
                        () -> net.minecraftforge.fml.IExtensionPoint.DisplayTest.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> true
                )
        );

        // forge mod list config button
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientConfigScreen);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("PolyJot common setup initialized.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("PolyJot client setup initialized.");
    }

    private void registerClientConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) -> new net.polyjot.client.screen.ModSelectionScreen(parentScreen))
        );
    }
}
