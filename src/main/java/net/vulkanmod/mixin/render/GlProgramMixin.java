package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import net.vulkanmod.gl.VkGlProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(value = GlProgram.class, remap = false)
public abstract class GlProgramMixin {

    @Shadow(remap = false)
    private int programId;

    @Shadow(remap = false)
    private Map<String, Uniform> uniformsByName;

    /**
     * Do not query OpenGL for uniforms – VulkanMod manages descriptors separately.
     *
     * @author VulkanMod
     * @reason Skip redundant GL calls when running on the Vulkan backend.
     */
    @Overwrite(remap = false)
    public void setupUniforms(List<com.mojang.blaze3d.pipeline.RenderPipeline.UniformDescription> uniforms,
                              List<String> samplers) {
        // No-op: uniforms are handled through Vulkan descriptor sets.
    }

    /**
     * Prevent OpenGL program destruction and forward cleanup to the Vulkan pipeline holder.
     *
     * @author VulkanMod
     * @reason Resource lifetime is owned by VkGlProgram instances.
     */
    @Overwrite(remap = false)
    public void close() {
        VkGlProgram program = VkGlProgram.getProgram(this.programId);
        if (program != null && program.getPipeline() != null) {
            program.getPipeline().scheduleCleanUp();
        }
    }

    /**
     * @author VulkanMod
     * @reason Expose cached uniforms without re-querying the GL driver.
     */
    @Overwrite(remap = false)
    public Uniform getUniform(String name) {
        return this.uniformsByName != null ? this.uniformsByName.get(name) : null;
    }

    /**
     * @author VulkanMod
     * @reason Keep parity with vanilla API while remaining a pure data accessor.
     */
    @Overwrite(remap = false)
    public Map<String, Uniform> getUniforms() {
        return this.uniformsByName != null ? this.uniformsByName : Map.of();
    }
}
