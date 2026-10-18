package com.cake.struts.neoforge;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.network.StrutPackets;
import com.cake.struts.registry.StrutDataComponents;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entrypoint, replacing the Fabric {@code main} entrypoint ({@code fabric/StrutsFabric}).
 * <p>
 * Fabric leaves the built-in registries open after bootstrap; NeoForge only opens them while it posts
 * {@link RegisterEvent}, so the data components are registered from there.
 */
@Mod(StrutYourStuff.MOD_ID)
public final class StrutsNeoForge {

    public StrutsNeoForge(final IEventBus modBus) {
        StrutsCreatePlugin.verifyEarlyRegistrationComplete();
        modBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
                StrutDataComponents.register();
            }
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, StrutPackets::register);
        StrutYourStuff.init();
    }
}
