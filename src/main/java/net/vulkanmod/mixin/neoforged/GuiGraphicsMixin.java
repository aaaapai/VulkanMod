package net.vulkanmod.mixin.neoforged;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ItemDecoratorHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * &#064;Author: KSmc_brigade
 * &#064;Date: 2025/9/20 下午7:58
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Redirect(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ItemDecoratorHandler;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void renderDecorations(ItemDecoratorHandler instance, GuiGraphics guiGraphics, Font font, ItemStack stack, int xo, int yo){

    }
}
