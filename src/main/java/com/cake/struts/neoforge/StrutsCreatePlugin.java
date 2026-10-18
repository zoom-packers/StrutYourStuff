package com.cake.struts.neoforge;

import com.cake.struts.StrutYourStuff;
import com.zurrtum.create.api.registry.CreateRegisterPlugin;

/**
 * Registers the strut structure block while Create registers its own blocks during vanilla bootstrap. Listed in
 * {@code META-INF/services/com.zurrtum.create.api.registry.CreateRegisterPlugin}, which replaces the Fabric
 * {@code create_plugin} entrypoint.
 */
public final class StrutsCreatePlugin implements CreateRegisterPlugin {
    private static boolean blocksRegistered;

    @Override
    public void onBlockRegister() {
        if (blocksRegistered) {
            throw new IllegalStateException("Create Fly invoked Strut Your Stuff block registration more than once");
        }
        StrutYourStuff.registerBlocksEarly();
        blocksRegistered = true;
    }

    public static void verifyEarlyRegistrationComplete() {
        if (!blocksRegistered) {
            throw new IllegalStateException("Create Fly did not invoke Strut Your Stuff early block registration");
        }
    }
}
