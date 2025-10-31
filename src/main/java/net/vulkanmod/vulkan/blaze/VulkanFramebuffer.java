package net.vulkanmod.vulkan.blaze;

import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.texture.VulkanImage;

/**
 * Thin wrapper around the existing {@link Framebuffer} class so the Vulkan
 * render pass placeholder can store colour/depth attachments without touching
 * Mojang's GL objects.
 */
final class VulkanFramebuffer {

    private final Framebuffer framebuffer;

    VulkanFramebuffer(VulkanGpuTexture color, VulkanGpuTexture depth) {
        VulkanImage colorImage = color != null ? color.image() : null;
        VulkanImage depthImage = depth != null ? depth.image() : null;
        this.framebuffer = Framebuffer.builder(colorImage, depthImage)
            .build();
    }

    Framebuffer handle() {
        return framebuffer;
    }

    VulkanImage colorAttachment() {
        return framebuffer.getColorAttachment();
    }

    VulkanImage depthAttachment() {
        return framebuffer.getDepthAttachment();
    }
}
