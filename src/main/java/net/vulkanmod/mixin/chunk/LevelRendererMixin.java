package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.core.backend.BackendManager;
import net.vulkanmod.render.core.backend.RenderBackend;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        BackendManager.get().initialize((LevelRenderer) (Object) this, renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        BackendManager.get().setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        BackendManager.get().allChanged();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        PoseStack poseStack = new PoseStack();

        RenderBackend backend = BackendManager.get();
        backend.renderBlockEntities(poseStack, pos, this.destructionProgress, deltaTracker.getGameTimeDeltaPartialTick(false), null);
    }

    @Inject(method = "isSectionCompiled", at = @At("HEAD"), cancellable = true)
    private void vk$isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(BackendManager.get().isSectionCompiled(blockPos));
    }

    @Inject(method = "setSectionDirty", at = @At("HEAD"), cancellable = true)
    private void vk$setSectionDirty(int x, int y, int z, CallbackInfo ci) {
        BackendManager.get().setSectionDirty(x, y, z);
        ci.cancel();
    }

    @Inject(method = "getSectionStatistics", at = @At("HEAD"), cancellable = true)
    private void vk$getSectionStats(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(BackendManager.get().getSectionStatistics());
    }

    @Inject(method = "hasRenderedAllSections", at = @At("HEAD"), cancellable = true)
    private void vk$hasRenderedAllSections(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(BackendManager.get().hasRenderedAllSections());
    }

    @Inject(method = "countRenderedSections", at = @At("HEAD"), cancellable = true)
    private void vk$countRenderedSections(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BackendManager.get().countRenderedSections());
    }
}
