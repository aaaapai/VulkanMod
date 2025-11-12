package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.util.ARGB;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.util.VUtil;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.lang.foreign.Arena;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_LOAD;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdClearAttachments;

final class VulkanRenderPassState {
    final VulkanFramebuffer framebuffer;
    final RenderPass renderPass;

    private final OptionalInt colorClear;
    private final OptionalDouble depthClear;

    private VulkanRenderPassState(VulkanFramebuffer framebuffer, RenderPass renderPass,
                                  OptionalInt colorClear, OptionalDouble depthClear) {
        this.framebuffer = framebuffer;
        this.renderPass = renderPass;
        this.colorClear = colorClear;
        this.depthClear = depthClear;
    }

    static VulkanRenderPassState create(VulkanFramebuffer framebuffer,
                                        GpuTextureView colorView,
                                        OptionalInt colorLevel,
                                        GpuTextureView depthView,
                                        OptionalDouble clearDepth) {
        Framebuffer fb = framebuffer.handle();
        RenderPass.Builder builder = RenderPass.builder(fb);

        if (builder.getColorAttachmentInfo() != null) {
            builder.getColorAttachmentInfo().setLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
        }
        if (builder.getDepthAttachmentInfo() != null) {
            builder.getDepthAttachmentInfo().setLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
        }

        RenderPass renderPass = builder.build();
        return new VulkanRenderPassState(framebuffer, renderPass, colorLevel, clearDepth);
    }

    void begin(VkCommandBuffer commandBuffer) {
        framebuffer.handle().beginRenderPass(commandBuffer, renderPass);

        if (colorClear.isPresent() || depthClear.isPresent()) {
            try (Arena arena = Arena.ofConfined()) {
                int attachmentCount = (colorClear.isPresent() ? 1 : 0) + (depthClear.isPresent() ? 1 : 0);
                VkClearAttachment.Buffer attachments = VUtil.structBuffer(
                    arena,
                    VkClearAttachment.SIZEOF,
                    VkClearAttachment.ALIGNOF,
                    attachmentCount,
                    VkClearAttachment::create
                );
                int index = 0;

                if (colorClear.isPresent()) {
                    VkClearAttachment colorAttachment = attachments.get(index++);
                    colorAttachment.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    colorAttachment.colorAttachment(0);
                    int argb = colorClear.getAsInt();
                    colorAttachment.clearValue().color().float32(0, ARGB.redFloat(argb));
                    colorAttachment.clearValue().color().float32(1, ARGB.greenFloat(argb));
                    colorAttachment.clearValue().color().float32(2, ARGB.blueFloat(argb));
                    colorAttachment.clearValue().color().float32(3, ARGB.alphaFloat(argb));
                }

                if (depthClear.isPresent()) {
                    VkClearAttachment depthAttachment = attachments.get(index);
                    depthAttachment.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                    depthAttachment.clearValue().depthStencil().set((float) depthClear.getAsDouble(), 0);
                }

                VkClearRect.Buffer rect = VUtil.structBuffer(
                    arena,
                    VkClearRect.SIZEOF,
                    VkClearRect.ALIGNOF,
                    1,
                    VkClearRect::create
                );
                rect.get(0).rect().offset().set(0, 0);
                rect.get(0).rect().extent().set(framebuffer.handle().getWidth(), framebuffer.handle().getHeight());
                rect.get(0).baseArrayLayer(0);
                rect.get(0).layerCount(1);

                vkCmdClearAttachments(commandBuffer, attachments, rect);
            }
        }
    }
}
