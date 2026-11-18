package net.polyjot.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.polyjot.PolyJotConstants;
import net.polyjot.client.screen.ModSelectionScreen;
import org.apache.logging.log4j.Logger;

@Mod(value = PolyJotConstants.MOD_ID, dist = Dist.CLIENT)
public class PolyJotNeoForge {
    public static final String MOD_ID = PolyJotConstants.MOD_ID;
    public static final Logger LOGGER = PolyJotConstants.LOGGER;

    public PolyJotNeoForge(ModContainer modContainer) {
        LOGGER.info("PolyJot NeoForge initialized.");

        // NeoForge mod list config button
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parentScreen) -> new ModSelectionScreen(parentScreen)
        );
    }
}
