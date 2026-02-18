package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;

public interface ShaderMixed {

    GraphicsPipeline getPipeline();

    void setPipeline(GraphicsPipeline pipeline);

    /**
     * Update only the shader's uniform buffer data without binding framebuffers
     * or pipelines. Used by VBO.drawWithShader to populate Iris UBO data
     * (iris_ModelViewMat, etc.) without disrupting the active render pass.
     * Default no-op for vanilla shaders; overridden by ExtendedShader.
     */
    default void updateUniformsOnly() {}
}
