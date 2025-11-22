package net.vulkanmod.vulkan.framebuffer;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.render.core.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.util.VUtil;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkViewport;

import java.lang.foreign.Arena;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass {
    final int attachmentCount;
    Framebuffer framebuffer;
    long id;
    AttachmentInfo colorAttachmentInfo;
    AttachmentInfo depthAttachmentInfo;

    public RenderPass(Framebuffer framebuffer, AttachmentInfo colorAttachmentInfo, AttachmentInfo depthAttachmentInfo) {
        this.framebuffer = framebuffer;
        this.colorAttachmentInfo = colorAttachmentInfo;
        this.depthAttachmentInfo = depthAttachmentInfo;

        int count = 0;
        if (colorAttachmentInfo != null)
            count++;
        if (depthAttachmentInfo != null)
            count++;

        this.attachmentCount = count;

        if (!Vulkan.DYNAMIC_RENDERING) {
            createRenderPass();
        }

    }

    public static Builder builder(Framebuffer framebuffer) {
        return new Builder(framebuffer);
    }

    private void createRenderPass() {
        try (Arena arena = Arena.ofConfined()) {
            VkAttachmentDescription.Buffer attachments = VUtil.structBuffer(
                arena,
                VkAttachmentDescription.SIZEOF,
                VkAttachmentDescription.ALIGNOF,
                attachmentCount,
                VkAttachmentDescription::create
            );
            VkAttachmentReference.Buffer attachmentRefs = attachmentCount > 0
                ? VUtil.structBuffer(arena, VkAttachmentReference.SIZEOF, VkAttachmentReference.ALIGNOF, attachmentCount, VkAttachmentReference::create)
                : null;

            VkSubpassDescription.Buffer subpass = VUtil.structBuffer(
                arena,
                VkSubpassDescription.SIZEOF,
                VkSubpassDescription.ALIGNOF,
                1,
                VkSubpassDescription::create
            );
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);

            int attachmentIndex = 0;

            if (colorAttachmentInfo != null) {
                VkAttachmentDescription colorAttachment = attachments.get(attachmentIndex);
                colorAttachment.format(colorAttachmentInfo.format)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(colorAttachmentInfo.loadOp)
                    .storeOp(colorAttachmentInfo.storeOp)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .finalLayout(colorAttachmentInfo.finalLayout);

                VkAttachmentReference.Buffer colorRef = VUtil.structBuffer(
                    arena,
                    VkAttachmentReference.SIZEOF,
                    VkAttachmentReference.ALIGNOF,
                    1,
                    VkAttachmentReference::create
                );
                colorRef.get(0)
                    .attachment(attachmentIndex)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

                subpass.colorAttachmentCount(1);
                subpass.pColorAttachments(colorRef);
                attachmentIndex++;
            }

            if (depthAttachmentInfo != null) {
                VkAttachmentDescription depthAttachment = attachments.get(attachmentIndex);
                depthAttachment.format(depthAttachmentInfo.format)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(depthAttachmentInfo.loadOp)
                    .storeOp(depthAttachmentInfo.storeOp)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                    .finalLayout(depthAttachmentInfo.finalLayout);

                VkAttachmentReference.Buffer depthRef = VUtil.structBuffer(
                    arena,
                    VkAttachmentReference.SIZEOF,
                    VkAttachmentReference.ALIGNOF,
                    1,
                    VkAttachmentReference::create
                );
                depthRef.get(0)
                    .attachment(attachmentIndex)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                subpass.pDepthStencilAttachment(depthRef.get(0));
                attachmentIndex++;
            }

            VkRenderPassCreateInfo renderPassInfo = VUtil.struct(
                arena,
                VkRenderPassCreateInfo.SIZEOF,
                VkRenderPassCreateInfo.ALIGNOF,
                VkRenderPassCreateInfo::create
            );
            renderPassInfo.sType$Default();
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);

            if (colorAttachmentInfo != null) {
                switch (colorAttachmentInfo.finalLayout) {
                    case VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> {
                        VkSubpassDependency.Buffer dependency = VUtil.structBuffer(
                            arena,
                            VkSubpassDependency.SIZEOF,
                            VkSubpassDependency.ALIGNOF,
                            1,
                            VkSubpassDependency::create
                        );
                        dependency.get(0)
                            .srcSubpass(VK_SUBPASS_EXTERNAL)
                            .dstSubpass(0)
                            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                            .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                            .srcAccessMask(0)
                            .dstAccessMask(0);
                        renderPassInfo.pDependencies(dependency);
                    }
                    case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> {
                        VkSubpassDependency.Buffer dependency = VUtil.structBuffer(
                            arena,
                            VkSubpassDependency.SIZEOF,
                            VkSubpassDependency.ALIGNOF,
                            1,
                            VkSubpassDependency::create
                        );
                        dependency.get(0)
                            .srcSubpass(0)
                            .dstSubpass(VK_SUBPASS_EXTERNAL)
                            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                            .dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
                            .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                        renderPassInfo.pDependencies(dependency);
                    }
                    default -> {
                    }
                }
            }

            LongBuffer pRenderPass = VUtil.allocateLongBuffer(arena, 1);

            if (vkCreateRenderPass(Vulkan.getVkDevice(), renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            id = pRenderPass.get(0);
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, long framebufferId, Arena arena) {

        if (colorAttachmentInfo != null
                && framebuffer.getColorAttachment().getCurrentLayout() != VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
            VulkanImage.transitionImageLayout(arena, commandBuffer, framebuffer.getColorAttachment(), VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        }
        if (depthAttachmentInfo != null
                && framebuffer.getDepthAttachment().getCurrentLayout() != VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
            VulkanImage.transitionImageLayout(arena, commandBuffer, framebuffer.getDepthAttachment(), VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        }

        VkRenderPassBeginInfo renderPassInfo = VUtil.struct(
            arena,
            VkRenderPassBeginInfo.SIZEOF,
            VkRenderPassBeginInfo.ALIGNOF,
            VkRenderPassBeginInfo::create
        );
        renderPassInfo.sType$Default();
        renderPassInfo.renderPass(this.id);
        renderPassInfo.framebuffer(framebufferId);

        VkRect2D renderArea = VUtil.struct(
            arena,
            VkRect2D.SIZEOF,
            VkRect2D.ALIGNOF,
            VkRect2D::create
        );
        renderArea.offset().set(0, 0);
        renderArea.extent().set(framebuffer.getWidth(), framebuffer.getHeight());
        renderPassInfo.renderArea(renderArea);

        VkClearValue.Buffer clearValues = VUtil.structBuffer(
            arena,
            VkClearValue.SIZEOF,
            VkClearValue.ALIGNOF,
            2,
            VkClearValue::create
        );
        float clearR = VRenderSystem.clearColor.get(0);
        float clearG = VRenderSystem.clearColor.get(1);
        float clearB = VRenderSystem.clearColor.get(2);
        float clearA = VRenderSystem.clearColor.get(3);
        clearValues.get(0).color().float32(0, clearR);
        clearValues.get(0).color().float32(1, clearG);
        clearValues.get(0).color().float32(2, clearB);
        clearValues.get(0).color().float32(3, clearA);
        clearValues.get(1).depthStencil().set(1.0f, 0);

        renderPassInfo.pClearValues(clearValues);

        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        Renderer.getInstance().setBoundRenderPass(this);
    }

    public void endRenderPass(VkCommandBuffer commandBuffer) {
        vkCmdEndRenderPass(commandBuffer);

        if (colorAttachmentInfo != null)
            framebuffer.getColorAttachment().setCurrentLayout(colorAttachmentInfo.finalLayout);

        if (depthAttachmentInfo != null)
            framebuffer.getDepthAttachment().setCurrentLayout(depthAttachmentInfo.finalLayout);

        Renderer.getInstance().setBoundRenderPass(null);
    }

    public void beginDynamicRendering(VkCommandBuffer commandBuffer, Arena arena, Framebuffer framebuffer) {
        VkRect2D renderArea = VUtil.struct(
            arena,
            VkRect2D.SIZEOF,
            VkRect2D.ALIGNOF,
            VkRect2D::create
        );
        renderArea.offset().set(0, 0);
        renderArea.extent().set(framebuffer.getWidth(), framebuffer.getHeight());

        VkClearValue.Buffer clearValues = VUtil.structBuffer(
            arena,
            VkClearValue.SIZEOF,
            VkClearValue.ALIGNOF,
            2,
            VkClearValue::create
        );
        clearValues.get(0).color().float32(0, 0.0f);
        clearValues.get(0).color().float32(1, 0.0f);
        clearValues.get(0).color().float32(2, 0.0f);
        clearValues.get(0).color().float32(3, 1.0f);
        clearValues.get(1).depthStencil().set(1.0f, 0);

        VkRenderingInfo renderingInfo = VUtil.struct(
            arena,
            VkRenderingInfo.SIZEOF,
            VkRenderingInfo.ALIGNOF,
            VkRenderingInfo::create
        );
        renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
        renderingInfo.renderArea(renderArea);
        renderingInfo.layerCount(1);

        // Color attachment
        if (colorAttachmentInfo != null) {
            VkRenderingAttachmentInfo.Buffer colorAttachment = VUtil.structBuffer(
                arena,
                VkRenderingAttachmentInfo.SIZEOF,
                VkRenderingAttachmentInfo.ALIGNOF,
                1,
                VkRenderingAttachmentInfo::create
            );
            colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
            colorAttachment.imageView(framebuffer.getColorAttachment().getImageView());
            colorAttachment.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            colorAttachment.loadOp(colorAttachmentInfo.loadOp);
            colorAttachment.storeOp(colorAttachmentInfo.storeOp);
            colorAttachment.clearValue(clearValues.get(0));

            renderingInfo.pColorAttachments(colorAttachment);
        }

        //Depth attachment
        if (depthAttachmentInfo != null) {
            VkRenderingAttachmentInfo depthAttachment = VUtil.struct(
                arena,
                VkRenderingAttachmentInfo.SIZEOF,
                VkRenderingAttachmentInfo.ALIGNOF,
                VkRenderingAttachmentInfo::create
            );
            depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
            depthAttachment.imageView(framebuffer.getDepthAttachment().getImageView());
            depthAttachment.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            depthAttachment.loadOp(depthAttachmentInfo.loadOp);
            depthAttachment.storeOp(depthAttachmentInfo.storeOp);
            depthAttachment.clearValue(clearValues.get(1));

            renderingInfo.pDepthAttachment(depthAttachment);
        }

        KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);
    }

    public void endDynamicRendering(VkCommandBuffer commandBuffer) {
        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public void cleanUp() {
        //TODO

        if (!Vulkan.DYNAMIC_RENDERING)
            MemoryManager.getInstance().addFrameOp(
                    () -> vkDestroyRenderPass(Vulkan.getVkDevice(), this.id, null));

    }

    public long getId() {
        return id;
    }

    public static class AttachmentInfo {
        final Type type;
        final int format;
        int finalLayout;
        int loadOp;
        int storeOp;

        public AttachmentInfo(Type type, int format) {
            this.type = type;
            this.format = format;
            this.finalLayout = type.defaultLayout;

            this.loadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            this.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
        }

        public AttachmentInfo setOps(int loadOp, int storeOp) {
            this.loadOp = loadOp;
            this.storeOp = storeOp;

            return this;
        }

        public AttachmentInfo setLoadOp(int loadOp) {
            this.loadOp = loadOp;

            return this;
        }

        public AttachmentInfo setFinalLayout(int finalLayout) {
            this.finalLayout = finalLayout;

            return this;
        }

        public enum Type {
            COLOR(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL),
            DEPTH(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            final int defaultLayout;

            Type(int layout) {
                defaultLayout = layout;
            }
        }
    }

    public static class Builder {
        Framebuffer framebuffer;
        AttachmentInfo colorAttachmentInfo;
        AttachmentInfo depthAttachmentInfo;

        public Builder(Framebuffer framebuffer) {
            this.framebuffer = framebuffer;

            if (framebuffer.hasColorAttachment)
                colorAttachmentInfo = new AttachmentInfo(AttachmentInfo.Type.COLOR, framebuffer.format).setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE);
            if (framebuffer.hasDepthAttachment)
                depthAttachmentInfo = new AttachmentInfo(AttachmentInfo.Type.DEPTH, framebuffer.depthFormat).setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_DONT_CARE);
        }

        public RenderPass build() {
            return new RenderPass(framebuffer, colorAttachmentInfo, depthAttachmentInfo);
        }

        public Builder setLoadOp(int loadOp) {
            if (colorAttachmentInfo != null) {
                colorAttachmentInfo.setLoadOp(loadOp);
            }
            if (depthAttachmentInfo != null) {
                depthAttachmentInfo.setLoadOp(loadOp);
            }


            return this;
        }

        public AttachmentInfo getColorAttachmentInfo() {
            return colorAttachmentInfo;
        }

        public AttachmentInfo getDepthAttachmentInfo() {
            return depthAttachmentInfo;
        }
    }
}
