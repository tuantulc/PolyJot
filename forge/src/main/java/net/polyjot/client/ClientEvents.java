package net.polyjot.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.polyjot.PolyJot;
import net.polyjot.client.screen.ModSelectionScreen;
import net.polyjot.client.screen.TranslationConfigBuilder;
import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationScanner;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.Services;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PolyJot.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.polyjot.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            "key.categories.polyjot"
    );

    @Mod.EventBusSubscriber(modid = PolyJot.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
        var commandHandler = Commands.literal("polyjot")
                .executes(ctx -> {
                    PolyJotCommands.openModSelection();
                    return 1;
                })
                .then(Commands.literal("hand")
                        .executes(ctx -> {
                            PolyJotCommands.openHandItemEdit();
                            return 1;
                        })
                )
                .then(Commands.literal("edit")
                        .then(Commands.argument("modid", StringArgumentType.word())
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

        event.getDispatcher().register(commandHandler);
    }
}
