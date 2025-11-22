package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.vulkanmod.render.core.VRenderSystem;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "updateBuffer", at = @At("TAIL"))
    private void vulkanmod$captureFog(ByteBuffer buffer, int offset, Vector4f color,
                                      float environmentalStart, float environmentalEnd,
                                      float renderStart, float renderEnd,
                                      float skyEnd, float cloudEnd, CallbackInfo ci) {
        VRenderSystem.setShaderFogColor(color.x, color.y, color.z, color.w);
        VRenderSystem.setFogParameters(environmentalStart, renderStart, environmentalEnd, renderEnd, skyEnd, cloudEnd);
        VRenderSystem.setFogShapeIndex(0);
    }
}
