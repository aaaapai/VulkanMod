package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicUniforms;
import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DynamicUniforms.class)
public class DynamicUniformsMixin {

    @Inject(method = "writeTransform", at = @At("HEAD"))
    private void captureTransform(Matrix4fc modelView, Vector4fc color, Vector3fc modelOffset, Matrix4fc textureMatrix, float lineWidth, CallbackInfoReturnable<GpuBufferSlice> cir) {
        VRenderSystem.applyModelViewMatrix(new Matrix4f(modelView));
        VRenderSystem.setShaderColor(color.x(), color.y(), color.z(), color.w());
        VRenderSystem.setModelOffset(modelOffset.x(), modelOffset.y(), modelOffset.z());
        VRenderSystem.setTextureMatrix(new Matrix4f(textureMatrix));
        VRenderSystem.setLineWidth(lineWidth);
        VRenderSystem.calculateMVP();
    }
}