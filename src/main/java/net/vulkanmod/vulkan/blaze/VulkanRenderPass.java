package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass.Builder;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Vulkan-backed RenderPass driven by the existing {@link Builder}. Many draw
 * operations are still pending; for now we only set up descriptor bindings and
 * keep track of state.
 */
final class VulkanRenderPass implements RenderPass {

    private final VulkanCommandEncoder encoder;
    private final VulkanRenderPassState state;

    private boolean closed = false;

    VulkanRenderPass(VulkanCommandEncoder encoder, VulkanRenderPassState state) {
        this.encoder = encoder;
        this.state = state;
        state.begin(Renderer.getCommandBuffer());
    }

    private static UnsupportedOperationException nyi(String method) {
        return new UnsupportedOperationException("RenderPass#" + method + " is not implemented yet");
    }

    VulkanRenderPassState state() {
        return state;
    }

    void begin(VkCommandBuffer commandBuffer) {
        state.begin(commandBuffer);
    }

    void end(VkCommandBuffer commandBuffer) {
        if (!closed) {
            Renderer.getInstance().endRenderPass(commandBuffer);
            closed = true;
        }
    }

    @Override
    public void pushDebugGroup(Supplier<String> messageFactory) {
        // TODO integrate with VK_EXT_debug_utils
    }

    @Override
    public void popDebugGroup() {
    }

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        throw nyi("setPipeline");
    }

    @Override
    public void bindSampler(String name, GpuTextureView view) {
        // descriptor binding will be implemented later
    }

    @Override
    public void setUniform(String name, GpuBuffer buffer) {
        // descriptor binding will be implemented later
    }

    @Override
    public void setUniform(String name, GpuBufferSlice slice) {
        // descriptor binding will be implemented later
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    @Override
    public void disableScissor() {
        Framebuffer framebuffer = state.framebuffer.handle();
        Renderer.setScissor(0, 0, framebuffer.getWidth(), framebuffer.getHeight());
    }

    @Override
    public void setVertexBuffer(int slot, GpuBuffer buffer) {
        throw nyi("setVertexBuffer");
    }

    @Override
    public void setIndexBuffer(GpuBuffer buffer, VertexFormat.IndexType type) {
        throw nyi("setIndexBuffer");
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int baseVertex) {
        throw nyi("drawIndexed");
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<Draw<T>> draws,
                                        GpuBuffer indirectBuffer,
                                        VertexFormat.IndexType type,
                                        Collection<String> boundSamplers,
                                        T shaderState) {
        throw nyi("drawMultipleIndexed");
    }

    @Override
    public void draw(int vertexCount, int instanceCount) {
        throw nyi("draw");
    }

    @Override
    public void close() {
        end(Renderer.getCommandBuffer());
        encoder.onRenderPassClosed();
    }
}
