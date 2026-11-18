package net.polyjot.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.polyjot.PolyJotConstants;
import net.polyjot.client.PolyJotCommands;
import net.polyjot.client.screen.ModSelectionScreen;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.Services;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = PolyJotConstants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEvents {
    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.polyjot.open_gui",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.polyjot"
    );

    @EventBusSubscriber(modid = PolyJotConstants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_GUI_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) {
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
