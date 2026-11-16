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
                        Minecraft.getInstance().tell(() -> {
                            Minecraft.getInstance().setScreen(new ModSelectionScreen(null));
                        });
                        return 1;
                    })
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
                                        Minecraft.getInstance().tell(() -> {
                                            String targetLang = Minecraft.getInstance().options.languageCode;
                                            DiscoveredMod modInfo = Services.PLATFORM.getMod(modId);

                                            if (modInfo == null) {
                                                if (Minecraft.getInstance().player != null) {
                                                    Minecraft.getInstance().player.displayClientMessage(
                                                            Component.literal("§c[PolyJot] Mod '" + modId + "' not found!"), false
                                                    );
                                                }
                                                return;
                                            }

                                            ModTranslationData data = TranslationScanner.scanMod(
                                                    Minecraft.getInstance().getResourceManager(),
                                                    modInfo.getModId(),
                                                    modInfo.getDisplayName(),
                                                    targetLang
                                            );

                                            if (data == null || data.getTotalCount() == 0) {
                                                if (Minecraft.getInstance().player != null) {
                                                    Minecraft.getInstance().player.displayClientMessage(
                                                            Component.literal("§e[PolyJot] Mod '" + modInfo.getDisplayName() + "' has no language keys."), false
                                                    );
                                                }
                                                return;
                                            }

                                            Screen screen = TranslationConfigBuilder.createScreen(null, data, targetLang);
                                            Minecraft.getInstance().setScreen(screen);
                                        });
                                        return 1;
                                    })
                            )
                    );

            dispatcher.register(commandHandler);
        });

        PolyJotConstants.LOGGER.info("PolyJot Fabric Client initialized successfully.");
    }
}
