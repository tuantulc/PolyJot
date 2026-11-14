package net.languageport.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.languageport.LanguagePort;
import net.languageport.client.screen.ModSelectionScreen;
import net.languageport.client.screen.TranslationConfigBuilder;
import net.languageport.data.ModTranslationData;
import net.languageport.data.TranslationScanner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;

@Mod.EventBusSubscriber(modid = LanguagePort.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.language_port.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, // Unbound by default as requested
            "key.categories.language_port"
    );

    @Mod.EventBusSubscriber(modid = LanguagePort.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GUI_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
            while (OPEN_GUI_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new ModSelectionScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("lang")
                        .executes(ctx -> {
                            Minecraft.getInstance().tell(() -> {
                                Minecraft.getInstance().setScreen(new ModSelectionScreen(null));
                            });
                            return 1;
                        })
                        .then(Commands.literal("edit")
                                .then(Commands.argument("modid", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (IModInfo mod : ModList.get().getMods()) {
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
                                                IModInfo modInfo = null;
                                                for (IModInfo mod : ModList.get().getMods()) {
                                                    if (mod.getModId().equalsIgnoreCase(modId)) {
                                                        modInfo = mod;
                                                        break;
                                                    }
                                                }

                                                if (modInfo == null) {
                                                    Minecraft.getInstance().player.displayClientMessage(
                                                            Component.literal("§c[Language Port] Mod '" + modId + "' not found!"), false
                                                    );
                                                    return;
                                                }

                                                ModTranslationData data = TranslationScanner.scanMod(
                                                        Minecraft.getInstance().getResourceManager(),
                                                        modInfo.getModId(),
                                                        modInfo.getDisplayName(),
                                                        targetLang
                                                );

                                                if (data == null || data.getTotalCount() == 0) {
                                                    Minecraft.getInstance().player.displayClientMessage(
                                                            Component.literal("§e[Language Port] Mod '" + modInfo.getDisplayName() + "' has no language keys."), false
                                                    );
                                                    return;
                                                }

                                                Screen screen = TranslationConfigBuilder.createScreen(null, data, targetLang);
                                                Minecraft.getInstance().setScreen(screen);
                                            });
                                            return 1;
                                        })
                                )
                        )
        );
    }
}
