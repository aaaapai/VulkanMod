package net.vulkanmod.mixin.render.frame;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.gl.HiddenGlContext;
import net.vulkanmod.vulkan.Renderer;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Redirect(method = "initRenderer", at = @At(value = "NEW", target = "com/mojang/blaze3d/opengl/GlDevice"), remap = false)
    private static GlDevice vulkanmod$useStubContext(long window, int debugVerbosity, boolean debugSync,
                                                     BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter,
                                                     boolean enableDebugLabels) {
        long contextWindow = HiddenGlContext.getHandle();
        return new GlDevice(contextWindow, debugVerbosity, debugSync, shaderSourceGetter, enableDebugLabels);
    }

    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void endFrame(long window) {
        Renderer.getInstance().endFrame();
    }
}
