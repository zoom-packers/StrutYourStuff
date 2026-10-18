package com.cake.struts.neoforge.client;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.client.StrutsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge client entrypoint, replacing the Fabric {@code client} entrypoint ({@code fabric/StrutsClientFabric}).
 */
@Mod(value = StrutYourStuff.MOD_ID, dist = Dist.CLIENT)
public final class StrutsNeoForgeClient {

    public StrutsNeoForgeClient(final IEventBus modBus) {
        StrutsClient.init(modBus);
    }
}
