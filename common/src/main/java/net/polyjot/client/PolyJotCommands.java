package net.polyjot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.polyjot.client.screen.ModSelectionScreen;
import net.polyjot.client.screen.TranslationConfigBuilder;
import net.polyjot.data.ModTranslationData;
import net.polyjot.data.TranslationScanner;
import net.polyjot.platform.DiscoveredMod;
import net.polyjot.platform.Services;

public class PolyJotCommands {

    public static void openModSelection() {
        Minecraft.getInstance().tell(() -> {
            Minecraft.getInstance().setScreen(new ModSelectionScreen(null));
        });
    }

    public static void openModEdit(String modId) {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            String targetLang = mc.options.languageCode;
            DiscoveredMod modInfo = Services.PLATFORM.getMod(modId);

            if (modInfo == null) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§c[PolyJot] ").append(Component.translatable("command.polyjot.mod_not_found", modId)), false);
                }
                return;
            }

            ModTranslationData data = TranslationScanner.scanMod(
                    mc.getResourceManager(),
                    modInfo.getModId(),
                    modInfo.getDisplayName(),
                    targetLang
            );

            if (data == null || data.getTotalCount() == 0) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§e[PolyJot] ").append(Component.translatable("command.polyjot.no_keys", modInfo.getDisplayName())), false);
                }
                return;
            }

            Screen screen = TranslationConfigBuilder.createScreen(null, data, targetLang);
            mc.setScreen(screen);
        });
    }

    public static void openHandItemEdit() {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            LocalPlayer player = mc.player;
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                stack = player.getOffhandItem();
            }

            if (stack.isEmpty()) {
                player.displayClientMessage(Component.literal("§c[PolyJot] ").append(Component.translatable("command.polyjot.empty_hand")), false);
                return;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String modId = itemId.getNamespace();

            if ("minecraft".equals(modId)) {
                player.displayClientMessage(Component.literal("§e[PolyJot] ").append(Component.translatable("command.polyjot.vanilla_unsupported")), false);
                return;
            }

            DiscoveredMod modInfo = Services.PLATFORM.getMod(modId);
            if (modInfo == null) {
                player.displayClientMessage(Component.literal("§c[PolyJot] ").append(Component.translatable("command.polyjot.mod_not_found", modId)), false);
                return;
            }

            String targetLang = mc.options.languageCode;
            ModTranslationData data = TranslationScanner.scanMod(
                    mc.getResourceManager(),
                    modInfo.getModId(),
                    modInfo.getDisplayName(),
                    targetLang
            );

            if (data == null || data.getTotalCount() == 0) {
                player.displayClientMessage(Component.literal("§e[PolyJot] ").append(Component.translatable("command.polyjot.no_keys", modInfo.getDisplayName())), false);
                return;
            }

            String searchKey = stack.getDescriptionId();
            Screen screen = TranslationConfigBuilder.createScreen(null, data, targetLang, searchKey);
            mc.setScreen(screen);
        });
    }
}
