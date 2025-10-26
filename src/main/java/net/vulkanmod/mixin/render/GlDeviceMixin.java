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
import net.vulkanmod.mixin.render.accessor.ShaderCompilationKeyAccessor;
import net.vulkanmod.render.pipeline.VulkanPipelineCompiler;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

@Mixin(value = GlDevice.class, remap = false)
public abstract class GlDeviceMixin {

    @Unique
    private static final String VULKANMOD_SHADER_ERROR = "[VulkanMod] Missing GLSL source for shader %s (%s)";
    @Shadow
    private BiFunction<ResourceLocation, ShaderType, String> defaultShaderSource;
    @Shadow
    @Final
    @Mutable
    private int uniformOffsetAlignment;

    @Inject(method = "getMaxSupportedTextureSize", at = @At("HEAD"), cancellable = true)
    private static void vulkanmod$maxTextureSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(16384);
    }

    @Shadow
    protected abstract GlShaderModule getOrCompileShader(ResourceLocation id, ShaderType type,
                                                         net.minecraft.client.renderer.ShaderDefines defines,
                                                         BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter);

    @Inject(
            method = "compileShader(Lcom/mojang/blaze3d/opengl/GlDevice$ShaderCompilationKey;Ljava/util/function/BiFunction;)Lcom/mojang/blaze3d/opengl/GlShaderModule;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vulkanmod$compileShader(@Coerce Object keyObject,
                                         BiFunction<ResourceLocation, ShaderType, String> sourceGetter,
                                         CallbackInfoReturnable<GlShaderModule> cir) {
        ShaderCompilationKeyAccessor key = (ShaderCompilationKeyAccessor) keyObject;

        BiFunction<ResourceLocation, ShaderType, String> getter = sourceGetter != null ? sourceGetter : this.defaultShaderSource;

        ResourceLocation id = key.vulkanmod$getId();
        ShaderType type = key.vulkanmod$getType();

        String rawSource = getter.apply(id, type);
        if (rawSource == null) {
            throw new IllegalStateException(VULKANMOD_SHADER_ERROR.formatted(type.getName(), id));
        }

        String processedSource = GlslPreprocessor.injectDefines(rawSource, key.vulkanmod$getDefines());

        GlShaderModule module = new GlShaderModule(-1, id, type);
        ((GlShaderModuleExt) module).vulkanmod$setProcessedSource(processedSource);
        cir.setReturnValue(module);
        cir.cancel();
    }

    @Inject(
            method = "compilePipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Ljava/util/function/BiFunction;)Lcom/mojang/blaze3d/opengl/GlRenderPipeline;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vulkanmod$compilePipeline(RenderPipeline pipeline,
                                           @Nullable BiFunction<ResourceLocation, ShaderType, String> sourceGetter,
                                           CallbackInfoReturnable<GlRenderPipeline> cir) {
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

        cir.setReturnValue(new GlRenderPipeline(pipeline, program));
        cir.cancel();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vulkanmod$adjustAlignment(long window, int debugVerbosity, boolean debugSync,
                                           BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter,
                                           boolean enableDebugLabels, CallbackInfo ci) {
        this.uniformOffsetAlignment = 256;
    }
}
