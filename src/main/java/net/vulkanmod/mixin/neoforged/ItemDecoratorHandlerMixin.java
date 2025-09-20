package net.vulkanmod.mixin.neoforged;

import net.neoforged.neoforge.client.ItemDecoratorHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * &#064;Author: KSmc_brigade
 * &#064;Date: 2025/9/20 下午8:01
 */
@Mixin(ItemDecoratorHandler.class)
public class ItemDecoratorHandlerMixin {
    @Inject(method = {"render","resetRenderState"},at = @At("HEAD"),cancellable = true)
    public void render(CallbackInfo ci){
        ci.cancel();
    }
}
