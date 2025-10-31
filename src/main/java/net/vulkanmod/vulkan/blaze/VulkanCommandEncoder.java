package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.foreign.MemorySegment;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Placeholder implementation. Methods currently throw
 * {@link UnsupportedOperationException} until the Vulkan translation layer
 * is implemented.
 */
public final class VulkanCommandEncoder implements CommandEncoder {

    private final VulkanGpuDevice device;
    private VulkanRenderPass currentPass;
    private boolean inRenderPass = false;

    VulkanCommandEncoder(VulkanGpuDevice device) {
        this.device = device;
    }

    public VulkanGpuDevice device() {
        return device;
    }

    private static UnsupportedOperationException nyi(String method) {
        return new UnsupportedOperationException("CommandEncoder#" + method + " is not implemented yet");
    }

    private static VulkanGpuBuffer expectBuffer(GpuBuffer buffer) {
        if (buffer instanceof VulkanGpuBuffer vulkanBuffer) {
            return vulkanBuffer;
        }
        throw new IllegalArgumentException("Unsupported buffer implementation: " + buffer.getClass().getName());
    }

    private static VulkanGpuTexture expectTexture(GpuTexture texture) {
        if (texture instanceof VulkanGpuTexture vkTexture) {
            return vkTexture;
        }
        throw new IllegalArgumentException("Unsupported texture implementation: " + texture.getClass().getName());
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> label,
                                       GpuTextureView colorTarget,
                                       OptionalInt colorLevel) {
        return createRenderPass(label, colorTarget, colorLevel, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> label,
                                       GpuTextureView colorTarget,
                                       OptionalInt colorLevel,
                                       GpuTextureView depthTarget,
                                       OptionalDouble clearDepth) {
        VulkanGpuTexture color = expectTexture(colorTarget.texture());
        VulkanGpuTexture depth = depthTarget != null ? expectTexture(depthTarget.texture()) : null;
        if (inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }
        VulkanFramebuffer framebuffer = new VulkanFramebuffer(color, depth);
        VulkanRenderPassState state = VulkanRenderPassState.create(framebuffer,
            colorTarget, colorLevel, depthTarget, clearDepth);
        currentPass = new VulkanRenderPass(this, state);
        inRenderPass = true;
        return currentPass;
    }

    void onRenderPassClosed() {
        inRenderPass = false;
        currentPass = null;
    }

    @Override
    public void clearColorTexture(GpuTexture texture, int level) {
        throw nyi("clearColorTexture");
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture color,
                                           int level,
                                           GpuTexture depth,
                                           double depthValue) {
        throw nyi("clearColorAndDepthTextures");
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture color,
                                           int level,
                                           GpuTexture depth,
                                           double depthValue,
                                           int x,
                                           int y,
                                           int width,
                                           int height) {
        throw nyi("clearColorAndDepthTextures");
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double value) {
        throw nyi("clearDepthTexture");
    }

    @Override
    public void writeToBuffer(GpuBufferSlice slice, java.nio.ByteBuffer data) {
        VulkanGpuBuffer buffer = expectBuffer(slice.buffer());
        int available = Math.min(data.remaining(), slice.length());
        if (available <= 0) {
            return;
        }
        MemorySegment src = MemorySegment.ofBuffer(data).asSlice(data.position(), available);
        MemorySegment dst = buffer.mapSegment(slice.offset(), available);
        dst.copyFrom(src);
        data.position(data.position() + available);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write) {
        VulkanGpuBuffer vkBuffer = expectBuffer(buffer);
        MemorySegment segment = vkBuffer.mapSegment(0, buffer.size());
        return new VulkanGpuBuffer.VulkanMappedView(segment);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write) {
        VulkanGpuBuffer vkBuffer = expectBuffer(slice.buffer());
        MemorySegment segment = vkBuffer.mapSegment(slice.offset(), slice.length());
        return new VulkanGpuBuffer.VulkanMappedView(segment);
    }

    @Override
    public void copyToBuffer(GpuBufferSlice src, GpuBufferSlice dst) {
        VulkanGpuBuffer srcBuffer = expectBuffer(src.buffer());
        VulkanGpuBuffer dstBuffer = expectBuffer(dst.buffer());
        int length = Math.min(src.length(), dst.length());
        if (length <= 0) {
            return;
        }
        MemorySegment srcSegment = srcBuffer.mapSegment(src.offset(), length);
        MemorySegment dstSegment = dstBuffer.mapSegment(dst.offset(), length);
        dstSegment.copyFrom(srcSegment);
    }

    @Override
    public void writeToTexture(GpuTexture texture, NativeImage image) {
        throw nyi("writeToTexture");
    }

    @Override
    public void writeToTexture(GpuTexture texture,
                               NativeImage image,
                               int x,
                               int y,
                               int width,
                               int height,
                               int level,
                               int depth,
                               int layer,
                               int stride) {
        throw nyi("writeToTexture");
    }

    @Override
    public void writeToTexture(GpuTexture texture,
                               java.nio.ByteBuffer data,
                               NativeImage.Format format,
                               int x,
                               int y,
                               int width,
                               int height,
                               int level,
                               int depth) {
        throw nyi("writeToTexture");
    }

    @Override
    public void copyTextureToBuffer(GpuTexture texture,
                                    GpuBuffer buffer,
                                    int level,
                                    Runnable onComplete,
                                    int stride) {
        throw nyi("copyTextureToBuffer");
    }

    @Override
    public void copyTextureToBuffer(GpuTexture texture,
                                    GpuBuffer buffer,
                                    int level,
                                    Runnable onComplete,
                                    int x,
                                    int y,
                                    int width,
                                    int height,
                                    int stride) {
        throw nyi("copyTextureToBuffer");
    }

    @Override
    public void copyTextureToTexture(GpuTexture src,
                                     GpuTexture dst,
                                     int srcLevel,
                                     int dstLevel,
                                     int srcX,
                                     int srcY,
                                     int width,
                                     int height,
                                     int depth) {
        throw nyi("copyTextureToTexture");
    }

    @Override
    public void presentTexture(GpuTextureView view) {
        throw nyi("presentTexture");
    }

    @Override
    public VulkanGpuFence createFence() {
        return new VulkanGpuFence();
    }
}
