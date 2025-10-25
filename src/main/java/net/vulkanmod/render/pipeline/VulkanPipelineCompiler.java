package net.vulkanmod.render.pipeline;

import java.util.Collections;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.converter.GlslConverter;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

public final class VulkanPipelineCompiler {
    private VulkanPipelineCompiler() {
    }

    public static GraphicsPipeline compile(RenderPipeline pipeline, String vertexSource, String fragmentSource) {
        GlslConverter converter = new GlslConverter();
        converter.process(vertexSource, fragmentSource);

        UBO ubo = converter.createUBO();

        Pipeline.Builder builder = new Pipeline.Builder(pipeline.getVertexFormat(), pipeline.getLocation().toString());
        builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
        builder.compileShaders(pipeline.getLocation().toString(), converter.getVshConverted(), converter.getFshConverted());

        return builder.createGraphicsPipeline();
    }
}
