package net.vulkanmod.mixin.render.clouds;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.core.backend.BackendManager;
import net.vulkanmod.render.sky.CloudRenderer;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererM {

    @Shadow
    private int ticks;
    @Shadow
    private @Nullable ClientLevel level;

    @Unique
    private CloudRenderer cloudRenderer;
    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.resetBuffer();
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void onReload(ResourceManager resourceManager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.loadTexture();
        }
    }

    @Redirect(method = "addCloudsPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/CloudStatus;Lnet/minecraft/world/phys/Vec3;FIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;F)V"))
    private void vk$renderClouds(net.minecraft.client.renderer.CloudRenderer vanillaRenderer,
                                 int cloudColor,
                                 CloudStatus cloudStatus,
                                 float cloudHeight,
                                 Vec3 cameraPosition,
                                 float animationTicks) {
        if (this.level == null) {
            vanillaRenderer.render(cloudColor, cloudStatus, cloudHeight, cameraPosition, animationTicks);
            return;
        }

        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CloudRenderer();
        }

        Matrix4f modelView = new Matrix4f();
        Matrix4f projection = new Matrix4f();
        var context = BackendManager.currentContext();
        if (context != null) {
            modelView.set(context.modelViewMatrix());
            projection.set(context.projectionMatrix());
        }

        PoseStack poseStack = new PoseStack();

        double camX = cameraPosition.x();
        double camY = cameraPosition.y();
        double camZ = cameraPosition.z();
        float partialTicks = animationTicks - this.ticks;

        this.cloudRenderer.renderClouds(this.level, poseStack, modelView, projection, this.ticks, partialTicks, camX, camY, camZ);
    }
}
