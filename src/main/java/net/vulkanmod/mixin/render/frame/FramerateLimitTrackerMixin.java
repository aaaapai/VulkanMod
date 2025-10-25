package net.vulkanmod.mixin.render.frame;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.FramerateLimitTracker;

import net.minecraft.client.Minecraft;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$limitWhenMinimized(CallbackInfoReturnable<Integer> cir) {
        if (this.minecraft.noRender) {
            cir.setReturnValue(10);
        }
    }
}