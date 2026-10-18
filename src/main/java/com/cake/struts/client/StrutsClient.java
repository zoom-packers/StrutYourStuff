package com.cake.struts.client;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.compat.flywheel.FlywheelCompat;
import com.cake.struts.compat.flywheel.StrutsFlywheelCompatLoader;
import com.cake.struts.content.StrutModelManipulator;
import com.cake.struts.content.StrutPlacementEffects;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.shape.StrutInteractionHandler;
import com.cake.struts.internal.microliner.Microliner;
import com.cake.struts.internal.util.ChunkedMap;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.neoforge.client.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class StrutsClient {

    private StrutsClient() {
    }

    // NeoForge: Fabric API's client events, mapped to NeoForge events
    public static void init(final IEventBus modBus) {
        StrutModelManipulator.prepareSegmentModels();
        StrutsFlywheelCompatLoader.bind(new FlywheelCompat());
        StrutBlockEntity.CLIENT_UPDATE_LISTENER = StrutInteractionHandler::updateOutlineShapes;
        StrutBlockEntity.CLIENT_REMOVE_LISTENER = StrutInteractionHandler::removeOutlineShapes;

        modBus.addListener(AddClientReloadListenersEvent.class, event -> event.addListener(
                Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "strut_meshes"),
                (ResourceManagerReloadListener) manager -> StrutModelManipulator.invalidateMeshes()));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> {
            final Minecraft client = Minecraft.getInstance();
            Microliner.get().tick();
            if (client.player != null) {
                StrutPlacementEffects.tick(client.player);
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class,
                event -> StrutInteractionHandler.onClientTick(Minecraft.getInstance()));

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            StrutInteractionHandler.clearSelection();
            final ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                LevelSafeStorage.clearAll(level);
                ChunkedMap.unbindAll(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ClientLevel) {
                StrutInteractionHandler.clearSelection();
            }
        });
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, event -> {
            if (event.getLevel() instanceof final ClientLevel level) {
                ChunkedMap.evictChunk(level, event.getChunk().getPos());
            }
        });

        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, event -> {
            final LevelRenderContext context = new LevelRenderContext(
                    event.getLevelRenderState(), event.getSubmitNodeCollector(), event.getPoseStack());
            Microliner.get().render(context);
            StrutInteractionHandler.renderSelection(context);
        });

        NeoForge.EVENT_BUS.addListener(ExtractBlockOutlineRenderStateEvent.class, event -> {
            if (StrutInteractionHandler.selectedShape != null) {
                event.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, event -> {
            if (event.isAttack() && StrutInteractionHandler.onClientAttack()) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        });
    }
}
