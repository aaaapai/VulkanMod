package net.vulkanmod.interfaces.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.vulkanmod.render.engine.EGlProgram;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

public interface ExtendedRenderPipeline {

    static ExtendedRenderPipeline of(RenderPipeline renderPipeline) {
        return (ExtendedRenderPipeline) renderPipeline;
    }

    void setPipeline(GraphicsPipeline pipeline);

    void setProgram(EGlProgram program);

    Pipeline getPipeline();

    EGlProgram getProgram();
}
