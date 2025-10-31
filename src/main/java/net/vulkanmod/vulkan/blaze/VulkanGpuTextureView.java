package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Placeholder texture view implementation. Once the backing command encoder
 * supports render pass creation we will expose the actual Vulkan image view.
 */
public final class VulkanGpuTextureView extends GpuTextureView {

    private final VulkanGpuTexture texture;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    VulkanGpuTextureView(VulkanGpuTexture texture, int baseMip, int mipLevels) {
        super(texture, baseMip, mipLevels);
        this.texture = texture;
    }

    public VulkanGpuTexture textureHandle() {
        return texture;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }
}
