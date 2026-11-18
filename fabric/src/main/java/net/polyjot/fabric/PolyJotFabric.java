package net.polyjot.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.polyjot.PolyJotConstants;
import net.polyjot.client.PolyJotCommands;
import net.polyjot.client.screen.ModSelectionScreen;
import net.polyjot.client.screen.TranslationConfigBuilder;
import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationScanner;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.Services;

public class PolyJotFabric implements ClientModInitializer {

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.polyjot.open_gui",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.polyjot"
    );

    @Override
    public void onInitializeClient() {
        PolyJotConstants.LOGGER.info("PolyJot Fabric Client Initializer starting...");
        KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                while (OPEN_GUI_KEY.consumeClick()) {
                    client.setScreen(new ModSelectionScreen(null));
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var commandHandler = ClientCommandManager.literal("polyjot")
                    .executes(ctx -> {
                        PolyJotCommands.openModSelection();
                        return 1;
                    })
                    .then(ClientCommandManager.literal("hand")
                            .executes(ctx -> {
                                PolyJotCommands.openHandItemEdit();
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("edit")
                            .then(ClientCommandManager.argument("modid", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        for (DiscoveredMod mod : Services.PLATFORM.getInstalledMods()) {
                                            if (!"minecraft".equals(mod.getModId())) {
                                                builder.suggest(mod.getModId());
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        String modId = StringArgumentType.getString(ctx, "modid");
                                        PolyJotCommands.openModEdit(modId);
                                        return 1;
                                    })
                            )
                    );

            dispatcher.register(commandHandler);
        });

        PolyJotConstants.LOGGER.info("PolyJot Fabric Client initialized successfully.");
    }
}
