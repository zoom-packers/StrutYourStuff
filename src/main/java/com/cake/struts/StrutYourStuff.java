package com.cake.struts;

import com.cake.struts.content.structure.GirderStrutStructureShapes;
import com.cake.struts.internal.util.ChunkedMap;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.content.StrutUseGuard;
import com.cake.struts.registry.StrutBlocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrutYourStuff {

    public static final String MOD_ID = "struts";

    public static final Logger LOGGER = LoggerFactory.getLogger("Strut Your Stuff");

    private StrutYourStuff() {
    }

    public static void registerBlocksEarly() {
        StrutBlocks.register();
    }

    // NeoForge: data components and packets are registered from NeoForge events (neoforge/StrutsNeoForge)
    public static void init() {
        StrutUseGuard.register();
        registerServerEvents();
    }

    // NeoForge: Fabric API's server events, mapped to NeoForge's game bus. Fabric's server-only events skip client
    // levels, which NeoForge's fire for too.
    private static void registerServerEvents() {
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof final Level level && !level.isClientSide()) {
                LevelSafeStorage.clearAll(level);
                ChunkedMap.unbindAll(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, event -> {
            if (event.getLevel() instanceof final Level level && !level.isClientSide()) {
                ChunkedMap.evictChunk(level, event.getChunk().getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (!event.getLevel().isClientSide()) {
                GirderStrutStructureShapes.flushQueuedRestores(event.getLevel());
            }
        });
        NeoForge.EVENT_BUS.addListener(BreakBlockEvent.class, event -> {
            if (!event.getLevel().isClientSide() && event.getLevel() instanceof final Level level) {
                queueStructureRestore(level, event.getPos(), event.getState());
            }
        });
    }

    private static void queueStructureRestore(final net.minecraft.world.level.Level level,
                                              final net.minecraft.core.BlockPos pos,
                                              final BlockState brokenState) {
        if (brokenState.getBlock() instanceof com.cake.struts.content.block.StrutBlock
                || brokenState.is(StrutBlocks.GIRDER_STRUT_STRUCTURE)) {
            return;
        }
        if (GirderStrutStructureShapes.hasPositionData(level, pos)) {
            GirderStrutStructureShapes.queueRestoreFromBreak(level, pos);
        }
    }
}
