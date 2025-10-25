package net.vulkanmod.mixin.compatibility.gl;

import org.lwjgl.opengl.GL14;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.vulkanmod.vulkan.VRenderSystem;

@Mixin(GL14.class)
public class GL14M {

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static void glBlendFuncSeparate(@NativeType("GLenum") int sfactorRGB, @NativeType("GLenum") int dfactorRGB, @NativeType("GLenum") int sfactorAlpha, @NativeType("GLenum") int dfactorAlpha) {
        VRenderSystem.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }
}