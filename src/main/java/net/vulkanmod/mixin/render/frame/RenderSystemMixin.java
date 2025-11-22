package net.vulkanmod.mixin.render.frame;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.blaze.VulkanGpuDevice;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiFunction;

@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {

    @Shadow @Nullable private static GpuDevice DEVICE;
    @Shadow private static String apiDescription;
    @Shadow @Nullable private static DynamicUniforms dynamicUniforms;

    /**
     * Replace Mojang's GL device bootstrap with our Vulkan-backed implementation.
     *
     * @author VulkanMod
     * @reason Ensure RenderSystem initializes the Vulkan GPU device instead of the GL fallback.
     */
    @Overwrite(remap = false)
    public static void initRenderer(long window,
                                    int debugVerbosity,
                                    boolean debugSync,
                                    BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter,
                                    boolean enableDebugLabels) {
        DEVICE = new VulkanGpuDevice(window, debugVerbosity, debugSync, shaderSourceGetter, enableDebugLabels);
        apiDescription = DEVICE.getImplementationInformation();
        dynamicUniforms = new DynamicUniforms();
    }

    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void endFrame(long window) {
        Renderer.getInstance().endFrame();
    }
}
