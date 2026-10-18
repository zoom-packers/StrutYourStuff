package com.cake.struts.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * Stand-in for Fabric API's {@code LevelRenderContext}, with the accessors the strut renderers use. Built from
 * NeoForge's {@code SubmitCustomGeometryEvent}.
 */
public record LevelRenderContext(LevelRenderState levelState, SubmitNodeCollector submitNodeCollector, PoseStack poseStack) {
}
