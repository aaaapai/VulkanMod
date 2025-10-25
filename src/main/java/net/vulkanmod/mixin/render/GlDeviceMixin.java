package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.gl.VkGlProgram;
import net.vulkanmod.interfaces.GlShaderModuleExt;
import net.vulkanmod.mixin.render.GlProgramInvoker;
import net.vulkanmod.render.pipeline.VulkanPipelineCompiler;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiFunction;

import java.util.function.BiFunction;

@Mixin(value = GlDevice.class, remap = false)
public abstract class GlDeviceMixin {

    @Shadow
    private BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource;

    @Shadow
    protected abstract GlShaderModule getOrCompileShader(ResourceLocation id, ShaderType type,
                                                         net.minecraft.client.renderer.ShaderDefines defines,
                                                         BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter);

    @Unique
    private static final String VULKANMOD_SHADER_ERROR = "[VulkanMod] Missing GLSL source for shader %s (%s)";

    /**
     * Intercept shader compilation to keep the processed GLSL source instead of compiling it with OpenGL.
     */
    @Overwrite(remap = false)
    private GlShaderModule compileShader(GlDevice.ShaderCompilationKey key,
                                         BiFunction<ResourceLocation, ShaderType, String> sourceGetter) {
        BiFunction<ResourceLocation, ShaderType, String> getter =
                sourceGetter != null ? sourceGetter : this.defaultShaderSource;

        String rawSource = getter.apply(key.id(), key.type());
        if (rawSource == null) {
            throw new IllegalStateException(VULKANMOD_SHADER_ERROR.formatted(key.type().getName(), key.id()));
        }

        String processedSource = GlslPreprocessor.injectDefines(rawSource, key.defines());

        GlShaderModule module = new GlShaderModule(-1, key.id(), key.type());
        ((GlShaderModuleExt) module).vulkanmod$setProcessedSource(processedSource);
        return module;
    }

    /**
     * Build a Vulkan-backed render pipeline instead of the default OpenGL program.
     */
    @Overwrite(remap = false)
    private GlRenderPipeline compilePipeline(RenderPipeline pipeline,
                                             @Nullable BiFunction<ResourceLocation, ShaderType, String> sourceGetter) {
        BiFunction<ResourceLocation, ShaderType, String> srcGetter =
                sourceGetter != null ? sourceGetter : (id, type) -> null;

        GlShaderModule vertexModule = this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX,
                                                              pipeline.getShaderDefines(), srcGetter);
        GlShaderModule fragmentModule = this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT,
                                                                pipeline.getShaderDefines(), srcGetter);

        String vertexSource = ((GlShaderModuleExt) vertexModule).vulkanmod$getProcessedSource();
        String fragmentSource = ((GlShaderModuleExt) fragmentModule).vulkanmod$getProcessedSource();

        if (vertexSource == null || fragmentSource == null) {
            throw new IllegalStateException("[VulkanMod] Missing processed GLSL for pipeline %s"
                    .formatted(pipeline.getLocation()));
        }

        GraphicsPipeline graphicsPipeline = VulkanPipelineCompiler.compile(pipeline, vertexSource, fragmentSource);

        int programId = VkGlProgram.genProgramId();
        VkGlProgram vkProgram = VkGlProgram.getProgram(programId);
        vkProgram.bindPipeline(graphicsPipeline);

        GlProgram program = GlProgramInvoker.vulkanmod$create(programId, pipeline.getLocation().toString());

        return new GlRenderPipeline(pipeline, program);
    }
}
