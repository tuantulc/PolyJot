package net.polyjot;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

@Mod(PolyJotConstants.MOD_ID)
public class PolyJot {
    public static final String MOD_ID = PolyJotConstants.MOD_ID;
    public static final Logger LOGGER = PolyJotConstants.LOGGER;

    @SuppressWarnings("removal")
    public PolyJot() {
        LOGGER.info("PolyJot Forge initialized.");

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

    @SuppressWarnings("removal")
    private void registerClientConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) -> new net.polyjot.client.screen.ModSelectionScreen(parentScreen))
        );
    }
}
