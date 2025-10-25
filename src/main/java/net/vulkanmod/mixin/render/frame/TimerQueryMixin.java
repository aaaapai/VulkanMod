package net.vulkanmod.mixin.render.frame;

import com.mojang.blaze3d.systems.TimerQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TimerQuery.class)
public class TimerQueryMixin {

    @Inject(method = "beginProfile", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$skipBegin(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "endProfile", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$skipEnd(CallbackInfoReturnable<TimerQuery.FrameProfile> cir) {
        cir.setReturnValue(null);
        cir.cancel();
    }

    @Inject(method = "isRecording", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$skipRecording(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
