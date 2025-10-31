package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WIP texture wrapper. The actual Vulkan image creation and upload code will be
 * connected once the command encoder is capable of recording the necessary operations.
 */
public final class VulkanGpuTexture extends GpuTexture {

    private final VulkanGpuDevice device;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private VulkanImage image;

    VulkanGpuTexture(VulkanGpuDevice device,
                     int usage,
                     String label,
                     TextureFormat format,
                     int width,
                     int height,
                     int depthOrLayers,
                     int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.device = device;
    }

    public VulkanGpuDevice device() {
        return device;
    }

    public void attachImage(VulkanImage image) {
        this.image = image;
    }

    public VulkanImage image() {
        return image;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && image != null) {
            image.free();
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }
}
