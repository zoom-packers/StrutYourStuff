package com.cake.struts.content;

import com.cake.struts.registry.StrutItemTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

public final class StrutUseGuard {

    private static final Map<Player, Integer> BREAK_TICKS = new WeakHashMap<>();

    private StrutUseGuard() {
    }

    // NeoForge: Fabric's early UseBlockCallback phase becomes a HIGHEST-priority RightClickBlock listener
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class, event -> {
            if (!event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND && consumeBreak(event.getEntity())) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
        });
    }

    public static void markBreak(final ServerPlayer player) {
        if (player.getMainHandItem().is(StrutItemTags.WRENCHES)) {
            BREAK_TICKS.put(player, player.level().getServer().getTickCount());
        }
    }

    private static boolean consumeBreak(final Player player) {
        if (!(player instanceof final ServerPlayer serverPlayer)) {
            return false;
        }
        final Integer tick = BREAK_TICKS.remove(serverPlayer);
        return tick != null && serverPlayer.level().getServer().getTickCount() - tick <= 1;
    }
}
