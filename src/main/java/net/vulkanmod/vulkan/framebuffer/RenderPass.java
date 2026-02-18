package net.vulkanmod.vulkan.framebuffer;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class RenderPass {
    Framebuffer framebuffer;
    long id;

    final int attachmentCount;
    AttachmentInfo colorAttachmentInfo;
    java.util.List<AttachmentInfo> colorAttachmentInfos; // MRT: per-attachment info
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
            framebuffer.addRenderPass(this);

            createRenderPass();
        }

    }

    public RenderPass(Framebuffer framebuffer, java.util.List<AttachmentInfo> colorAttachmentInfos, AttachmentInfo depthAttachmentInfo) {
        this.framebuffer = framebuffer;
        this.colorAttachmentInfos = colorAttachmentInfos;
        this.colorAttachmentInfo = colorAttachmentInfos.isEmpty() ? null : colorAttachmentInfos.get(0);
        this.depthAttachmentInfo = depthAttachmentInfo;

        int count = colorAttachmentInfos.size();
        if (depthAttachmentInfo != null)
            count++;

        this.attachmentCount = count;

        if (!Vulkan.DYNAMIC_RENDERING) {
            framebuffer.addRenderPass(this);

            createRenderPass();
        }
    }

    public int getColorAttachmentCount() {
        if (colorAttachmentInfos != null) return colorAttachmentInfos.size();
        return colorAttachmentInfo != null ? 1 : 0;
    }

    private void createRenderPass() {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int colorCount = getColorAttachmentCount();

            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(attachmentCount, stack);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);

            int i = 0;

            // Color attachments (MRT support)
            if (colorCount > 0) {
                VkAttachmentReference.Buffer colorRefs = VkAttachmentReference.calloc(colorCount, stack);

                for (int c = 0; c < colorCount; c++) {
                    AttachmentInfo info = (colorAttachmentInfos != null) ? colorAttachmentInfos.get(c) : colorAttachmentInfo;
                    // CLEAR/DONT_CARE: content is discarded, UNDEFINED avoids tracking desync.
                    // LOAD: use info.finalLayout as initialLayout. The image is left in
                    // finalLayout by the previous render pass (or clear operation), so this
                    // matches the actual layout. The render pass internally transitions from
                    // initialLayout to the subpass layout (COLOR_ATTACHMENT_OPTIMAL).
                    int colorInitLayout = (info.loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR || info.loadOp == VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            ? VK_IMAGE_LAYOUT_UNDEFINED : info.finalLayout;
                    VkAttachmentDescription colorDesc = attachments.get(i);
                    colorDesc.format(info.format)
                            .samples(VK_SAMPLE_COUNT_1_BIT)
                            .loadOp(info.loadOp)
                            .storeOp(info.storeOp)
                            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .initialLayout(colorInitLayout)
                            .finalLayout(info.finalLayout);

                    colorRefs.get(c)
                            .attachment(i)
                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

                    ++i;
                }

                subpass.colorAttachmentCount(colorCount);
                subpass.pColorAttachments(colorRefs);
            }

            // Depth-Stencil attachment
            if (depthAttachmentInfo != null) {
                // Same as color: UNDEFINED for CLEAR/DONT_CARE, finalLayout for LOAD.
                int depthInitLayout = (depthAttachmentInfo.loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR || depthAttachmentInfo.loadOp == VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        ? VK_IMAGE_LAYOUT_UNDEFINED : depthAttachmentInfo.finalLayout;
                VkAttachmentDescription depthAttachment = attachments.get(i);
                depthAttachment.format(depthAttachmentInfo.format)
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(depthAttachmentInfo.loadOp)
                        .storeOp(depthAttachmentInfo.storeOp)
                        .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                        .initialLayout(depthInitLayout)
                        .finalLayout(depthAttachmentInfo.finalLayout);

                VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc(stack)
                        .attachment(i)
                        .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                subpass.pDepthStencilAttachment(depthAttachmentRef);
            }

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType$Default()
                    .pAttachments(attachments)
                    .pSubpasses(subpass);

            // Subpass dependencies for layout transitions and synchronization.
            // We need up to 2 dependencies: input (EXTERNAL→0) and output (0→EXTERNAL).
            boolean needsShaderReadDep = false;
            boolean needsPresentDep = false;
            boolean needsInputDep = false;

            // Check if any color attachment uses LOAD with a non-attachment initialLayout.
            // When initialLayout != subpass layout, the render pass does an implicit
            // layout transition that needs proper synchronization via input dependency.
            AttachmentInfo refInfo = colorAttachmentInfo;
            if (refInfo != null) {
                if (refInfo.finalLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) needsPresentDep = true;
                if (refInfo.finalLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) needsShaderReadDep = true;
                if (refInfo.loadOp == VK_ATTACHMENT_LOAD_OP_LOAD
                        && refInfo.finalLayout != VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
                    needsInputDep = true;
                }
            }
            if (depthAttachmentInfo != null) {
                if (depthAttachmentInfo.finalLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                    needsShaderReadDep = true;
                }
                if (depthAttachmentInfo.loadOp == VK_ATTACHMENT_LOAD_OP_LOAD
                        && depthAttachmentInfo.finalLayout != VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                    needsInputDep = true;
                }
            }

            // Count how many dependencies we need
            int depCount = 0;
            if (needsInputDep) depCount++;
            if (needsPresentDep || needsShaderReadDep) depCount++;

            if (depCount > 0) {
                VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(depCount, stack);
                int depIdx = 0;

                // Input dependency: synchronize previous operations with the render pass's
                // implicit initialLayout → subpass layout transition.
                if (needsInputDep) {
                    subpassDependencies.get(depIdx++)
                            .srcSubpass(VK_SUBPASS_EXTERNAL)
                            .dstSubpass(0)
                            .srcStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                                    | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                                    | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT
                                    | VK_PIPELINE_STAGE_TRANSFER_BIT)
                            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                                    | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                            .srcAccessMask(VK_ACCESS_SHADER_READ_BIT
                                    | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                                    | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
                                    | VK_ACCESS_TRANSFER_WRITE_BIT)
                            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
                                    | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                                    | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
                                    | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                }

                // Output dependency
                if (needsPresentDep) {
                    subpassDependencies.get(depIdx)
                            .srcSubpass(VK_SUBPASS_EXTERNAL)
                            .dstSubpass(0)
                            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                            .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                            .srcAccessMask(0)
                            .dstAccessMask(0);
                } else if (needsShaderReadDep) {
                    int srcStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT
                            | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
                    int srcAccess = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;

                    subpassDependencies.get(depIdx)
                            .srcSubpass(0)
                            .dstSubpass(VK_SUBPASS_EXTERNAL)
                            .srcStageMask(srcStage)
                            .dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
                            .srcAccessMask(srcAccess)
                            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                }

                renderPassInfo.pDependencies(subpassDependencies);
            }

            LongBuffer pRenderPass = stack.mallocLong(1);

            if (vkCreateRenderPass(Vulkan.getVkDevice(), renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            id = pRenderPass.get(0);
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, long framebufferId, MemoryStack stack) {
        int colorCount = getColorAttachmentCount();
        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
        renderPassInfo.sType$Default();
        renderPassInfo.renderPass(this.id);
        renderPassInfo.framebuffer(framebufferId);

        VkRect2D renderArea = VkRect2D.malloc(stack);
        renderArea.offset().set(0, 0);
        renderArea.extent().set(framebuffer.getWidth(), framebuffer.getHeight());
        renderPassInfo.renderArea(renderArea);

        // Allocate clear values for all attachments
        VkClearValue.Buffer clearValues = VkClearValue.malloc(attachmentCount, stack);
        for (int c = 0; c < colorCount; c++) {
            clearValues.get(c).color().float32(VRenderSystem.clearColor);
        }
        if (depthAttachmentInfo != null) {
            clearValues.get(colorCount).depthStencil().set(1.0f, 0);
        }

        renderPassInfo.pClearValues(clearValues);

        vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

        Renderer.getInstance().setBoundRenderPass(this);
    }

    public void endRenderPass(VkCommandBuffer commandBuffer) {
        vkCmdEndRenderPass(commandBuffer);

        // Update tracked image layouts to match finalLayout (mirrors endDynamicRendering).
        // vkCmdEndRenderPass physically transitions images to finalLayout, but without
        // updating VulkanImage.currentLayout the tracking becomes stale, causing
        // subsequent transitionImageLayout calls to use wrong oldLayout in barriers.
        int colorCount = getColorAttachmentCount();
        for (int c = 0; c < colorCount; c++) {
            AttachmentInfo info = (colorAttachmentInfos != null) ? colorAttachmentInfos.get(c) : colorAttachmentInfo;
            VulkanImage colorImg = framebuffer.getColorAttachment(c);
            if (info != null && colorImg != null) {
                colorImg.setCurrentLayout(info.finalLayout);
            }
        }
        if (depthAttachmentInfo != null && framebuffer.getDepthAttachment() != null)
            framebuffer.getDepthAttachment().setCurrentLayout(depthAttachmentInfo.finalLayout);

        Renderer.getInstance().setBoundRenderPass(null);
    }

    public void beginDynamicRendering(VkCommandBuffer commandBuffer, MemoryStack stack) {
        VkRect2D renderArea = VkRect2D.malloc(stack);
        renderArea.offset().set(0, 0);
        renderArea.extent().set(framebuffer.getWidth(), framebuffer.getHeight());

        int colorCount = getColorAttachmentCount();

        VkClearValue.Buffer clearValues = VkClearValue.malloc(colorCount + 1, stack);
        for (int c = 0; c < colorCount; c++) {
            clearValues.get(c).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
        }
        clearValues.get(colorCount).depthStencil().set(1.0f, 0);

        VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack);
        renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
        renderingInfo.renderArea(renderArea);
        renderingInfo.layerCount(1);

        // Color attachments (MRT)
        if (colorCount > 0) {
            VkRenderingAttachmentInfo.Buffer colorAttachments = VkRenderingAttachmentInfo.calloc(colorCount, stack);
            for (int c = 0; c < colorCount; c++) {
                AttachmentInfo info = (colorAttachmentInfos != null) ? colorAttachmentInfos.get(c) : colorAttachmentInfo;
                VulkanImage colorImg = framebuffer.getColorAttachment(c);

                VkRenderingAttachmentInfo colorAtt = colorAttachments.get(c);
                colorAtt.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                colorAtt.imageView(colorImg.getImageView());
                colorAtt.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                colorAtt.loadOp(info.loadOp);
                colorAtt.storeOp(info.storeOp);
                colorAtt.clearValue(clearValues.get(c));
            }

            renderingInfo.pColorAttachments(colorAttachments);
        }

        //Depth attachment
        if (depthAttachmentInfo != null) {
            VkRenderingAttachmentInfo depthAttachment = VkRenderingAttachmentInfo.calloc(stack);
            depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
            depthAttachment.imageView(framebuffer.getDepthAttachment().getImageView());
            depthAttachment.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            depthAttachment.loadOp(depthAttachmentInfo.loadOp);
            depthAttachment.storeOp(depthAttachmentInfo.storeOp);
            depthAttachment.clearValue(clearValues.get(colorCount));

            renderingInfo.pDepthAttachment(depthAttachment);
        }

        KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);
    }

    public void endDynamicRendering(VkCommandBuffer commandBuffer) {
        KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

        int colorCount = getColorAttachmentCount();
        for (int c = 0; c < colorCount; c++) {
            AttachmentInfo info = (colorAttachmentInfos != null) ? colorAttachmentInfos.get(c) : colorAttachmentInfo;
            VulkanImage colorImg = framebuffer.getColorAttachment(c);
            if (info != null && colorImg != null) {
                colorImg.setCurrentLayout(info.finalLayout);
            }
        }

        if (depthAttachmentInfo != null)
            framebuffer.getDepthAttachment().setCurrentLayout(depthAttachmentInfo.finalLayout);

        Renderer.getInstance().setBoundRenderPass(null);
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

    public static Builder builder(Framebuffer framebuffer) {
        return new Builder(framebuffer);
    }

    public static class Builder {
        Framebuffer framebuffer;
        AttachmentInfo colorAttachmentInfo;
        java.util.List<AttachmentInfo> colorAttachmentInfos; // MRT
        AttachmentInfo depthAttachmentInfo;

        public Builder(Framebuffer framebuffer) {
            this.framebuffer = framebuffer;

            int colorCount = framebuffer.getColorAttachmentCount();
            if (colorCount > 1) {
                // MRT: create per-attachment info for each color attachment
                colorAttachmentInfos = new java.util.ArrayList<>();
                for (int c = 0; c < colorCount; c++) {
                    VulkanImage colorImg = framebuffer.getColorAttachment(c);
                    int fmt = (colorImg != null) ? colorImg.format : framebuffer.format;
                    colorAttachmentInfos.add(
                        new AttachmentInfo(AttachmentInfo.Type.COLOR, fmt)
                            .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE));
                }
                colorAttachmentInfo = colorAttachmentInfos.get(0);
            } else if (framebuffer.hasColorAttachment) {
                colorAttachmentInfo = new AttachmentInfo(AttachmentInfo.Type.COLOR, framebuffer.format).setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE);
            }

            if (framebuffer.hasDepthAttachment)
                depthAttachmentInfo = new AttachmentInfo(AttachmentInfo.Type.DEPTH, framebuffer.depthFormat).setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_DONT_CARE);
        }

        public RenderPass build() {
            if (colorAttachmentInfos != null) {
                return new RenderPass(framebuffer, colorAttachmentInfos, depthAttachmentInfo);
            }
            return new RenderPass(framebuffer, colorAttachmentInfo, depthAttachmentInfo);
        }

        public Builder setLoadOp(int loadOp) {
            if (colorAttachmentInfos != null) {
                for (AttachmentInfo info : colorAttachmentInfos) {
                    info.setLoadOp(loadOp);
                }
            } else if (colorAttachmentInfo != null) {
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

        public java.util.List<AttachmentInfo> getColorAttachmentInfos() {
            return colorAttachmentInfos;
        }

        public AttachmentInfo getDepthAttachmentInfo() {
            return depthAttachmentInfo;
        }
    }
}
