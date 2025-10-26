package net.vulkanmod.mixin.render;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    public Options options;
    @Unique
    private int vulkanmod$ticksRemaining;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void forceGraphicsMode(GameConfig gameConfig, CallbackInfo ci) {
        var graphicsModeOption = this.options.graphicsMode();

        if (graphicsModeOption.get() == GraphicsStatus.FABULOUS) {
            Initializer.LOGGER.error("Fabulous graphics mode not supported, forcing Fancy");
            graphicsModeOption.set(GraphicsStatus.FANCY);
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void vulkanmod$resetTickBudget(boolean bl, CallbackInfo ci) {
        this.vulkanmod$ticksRemaining = 0;
        SpriteUpdateUtil.setDoUpload(true);
    }

    @ModifyVariable(method = "runTick", at = @At(value = "STORE"), ordinal = 0)
    private int vulkanmod$captureTickBudget(int tickBudget) {
        this.vulkanmod$ticksRemaining = Math.min(10, tickBudget);
        if (this.vulkanmod$ticksRemaining == 0) {
            SpriteUpdateUtil.setDoUpload(true);
        }
        return tickBudget;
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
    private void vulkanmod$wrapTickForUploads(Minecraft instance) {
        int remaining = this.vulkanmod$ticksRemaining;
        SpriteUpdateUtil.setDoUpload(remaining <= 1);
        if (remaining > 0) {
            this.vulkanmod$ticksRemaining = remaining - 1;
        }
        instance.tick();
    }

    @Inject(method = "close", at = @At(value = "HEAD"))
    public void close(CallbackInfo ci) {
        Vulkan.waitIdle();
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/VirtualScreen;close()V"))
    public void close2(CallbackInfo ci) {
        Vulkan.cleanUp();
    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"))
    public void onResolutionChanged(CallbackInfo ci) {
        Renderer.scheduleSwapChainUpdate();
    }

    // Fixes crash when minimizing window before setScreen is called
    @Redirect(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;noRender:Z", opcode = Opcodes.PUTFIELD))
    private void keepVar(Minecraft instance, boolean value) {
    }

}
