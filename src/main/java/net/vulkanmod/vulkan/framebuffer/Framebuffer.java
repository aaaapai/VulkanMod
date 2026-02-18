package net.vulkanmod.vulkan.framebuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Arrays;

import static net.vulkanmod.vulkan.Vulkan.DYNAMIC_RENDERING;
import static org.lwjgl.vulkan.VK10.*;

public class Framebuffer {
    public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;

//    private long id;

    protected int format;
    protected int depthFormat;
    protected int width, height;
    protected boolean linearFiltering;
    protected boolean depthLinearFiltering;
    protected int attachmentCount;

    boolean hasColorAttachment;
    boolean hasDepthAttachment;

    private VulkanImage colorAttachment;
    private java.util.List<VulkanImage> colorAttachmentList; // MRT support
    protected VulkanImage depthAttachment;

    private final ObjectArrayList<RenderPass> renderPasses = new ObjectArrayList<>();

    private final Reference2LongArrayMap<RenderPass> framebufferIds = new Reference2LongArrayMap<>();

    //SwapChain
    protected Framebuffer() {}

    public Framebuffer(Builder builder) {
        this.format = builder.format;
        this.depthFormat = builder.depthFormat;
        this.width = builder.width;
        this.height = builder.height;
        this.linearFiltering = builder.linearFiltering;
        this.depthLinearFiltering = builder.depthLinearFiltering;
        this.hasColorAttachment = builder.hasColorAttachment;
        this.hasDepthAttachment = builder.hasDepthAttachment;

        if (builder.createImages)
            this.createImages();
        else {
            this.colorAttachment = builder.colorAttachment;
            this.depthAttachment = builder.depthAttachment;

            // MRT: store the full list if provided
            if (builder.colorAttachmentList != null && builder.colorAttachmentList.size() > 1) {
                this.colorAttachmentList = new java.util.ArrayList<>(builder.colorAttachmentList);
            }
        }
    }

    public void addRenderPass(RenderPass renderPass) {
        this.renderPasses.add(renderPass);
    }

    public void createImages() {
        if (this.hasColorAttachment) {
            this.colorAttachment = VulkanImage.builder(this.width, this.height)
                    .setFormat(format)
                    .setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .setLinearFiltering(linearFiltering)
                    .setClamp(true)
                    .createVulkanImage();
        }

        if (this.hasDepthAttachment) {
            this.depthAttachment = VulkanImage.createDepthImage(depthFormat, this.width, this.height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    depthLinearFiltering, true);

            this.attachmentCount++;
        }
    }

    public void resize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;

        this.cleanUp();

        this.createImages();
    }

    private long createFramebuffer(RenderPass renderPass) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            LongBuffer attachments;

            // MRT: if we have multiple color attachments, include all of them
            if (colorAttachmentList != null && colorAttachmentList.size() > 1) {
                int totalAttachments = colorAttachmentList.size() + (depthAttachment != null ? 1 : 0);
                attachments = stack.mallocLong(totalAttachments);
                for (int i = 0; i < colorAttachmentList.size(); i++) {
                    attachments.put(i, colorAttachmentList.get(i).getImageView());
                }
                if (depthAttachment != null) {
                    attachments.put(colorAttachmentList.size(), depthAttachment.getImageView());
                }
            } else if (colorAttachment != null && depthAttachment != null) {
                attachments = stack.longs(colorAttachment.getImageView(), depthAttachment.getImageView());
            } else if (colorAttachment != null) {
                attachments = stack.longs(colorAttachment.getImageView());
            } else {
                throw new IllegalStateException();
            }

            LongBuffer pFramebuffer = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType$Default();
            framebufferInfo.renderPass(renderPass.getId());
            framebufferInfo.width(this.width);
            framebufferInfo.height(this.height);
            framebufferInfo.layers(1);
            framebufferInfo.pAttachments(attachments);

            if (VK10.vkCreateFramebuffer(Vulkan.getVkDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            return pFramebuffer.get(0);
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, RenderPass renderPass, MemoryStack stack) {
        // Layout transition is handled by the render pass internally:
        // initialLayout (= finalLayout from previous pass, e.g. SHADER_READ_ONLY)
        // is transitioned to the subpass layout (COLOR_ATTACHMENT_OPTIMAL) automatically.
        // An input subpass dependency (EXTERNAL→0) ensures proper synchronization.
        // Explicit barriers are NOT needed and would set the WRONG layout.

        if (!DYNAMIC_RENDERING) {
            long framebufferId = this.getFramebufferId(renderPass);
            renderPass.beginRenderPass(commandBuffer, framebufferId, stack);
        } else {
            renderPass.beginDynamicRendering(commandBuffer, stack);
        }

        Renderer.getInstance().setBoundRenderPass(renderPass);
        Renderer.getInstance().setBoundFramebuffer(this);

        Renderer.setViewport(0, 0, this.width, this.height);
        Renderer.setScissor(0, 0, this.width, this.height);
    }

    /**
     * Transitions all color and depth attachments to the layouts expected by render passes.
     * Safe to call redundantly — skips images already in the correct layout.
     */
    public void transitionAttachmentsForRendering(MemoryStack stack, VkCommandBuffer commandBuffer) {
        int colorCount = getColorAttachmentCount();
        for (int i = 0; i < colorCount; i++) {
            VulkanImage img = getColorAttachment(i);
            if (img != null && img.getCurrentLayout() != VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {
                VulkanImage.transitionImageLayout(stack, commandBuffer, img, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
        }
        if (depthAttachment != null && depthAttachment.getCurrentLayout() != VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
            VulkanImage.transitionImageLayout(stack, commandBuffer, depthAttachment, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        }
    }

    protected long getFramebufferId(RenderPass renderPass) {
        return this.framebufferIds.computeIfAbsent(renderPass, renderPass1 -> createFramebuffer(renderPass));
    }

    public VkViewport.Buffer viewport(MemoryStack stack) {
        VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
        viewport.x(0.0f);
        viewport.y(this.height);
        viewport.width(this.width);
        viewport.height(-this.height);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        return viewport;
    }

    public VkRect2D.Buffer scissor(MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
        scissor.offset().set(0, 0);
        scissor.extent().set(this.width, this.height);

        return scissor;
    }

    public void cleanUp() {
        cleanUp(true);
    }

    public void cleanUp(boolean cleanImages) {
        if (cleanImages) {
            if (this.colorAttachmentList != null) {
                for (VulkanImage img : this.colorAttachmentList)
                    img.free();
            } else if (this.colorAttachment != null) {
                this.colorAttachment.free();
            }

            if (this.depthAttachment != null)
                this.depthAttachment.free();
        }

        final VkDevice device = Vulkan.getVkDevice();
        final var ids = framebufferIds.values().toLongArray();

        MemoryManager.getInstance().addFrameOp(
                () -> Arrays.stream(ids).forEach(id ->
                        vkDestroyFramebuffer(device, id, null))
        );


        framebufferIds.clear();
    }

    public long getDepthImageView() {
        return depthAttachment.getImageView();
    }

    public VulkanImage getDepthAttachment() {
        return depthAttachment;
    }

    public VulkanImage getColorAttachment() {
        return colorAttachment;
    }

    public VulkanImage getColorAttachment(int index) {
        if (colorAttachmentList != null && index < colorAttachmentList.size()) {
            return colorAttachmentList.get(index);
        }
        if (index == 0) return colorAttachment;
        return null;
    }

    public int getColorAttachmentCount() {
        if (colorAttachmentList != null) return colorAttachmentList.size();
        return colorAttachment != null ? 1 : 0;
    }

    public java.util.List<VulkanImage> getColorAttachmentList() {
        if (colorAttachmentList != null) return colorAttachmentList;
        if (colorAttachment != null) return java.util.List.of(colorAttachment);
        return java.util.List.of();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getFormat() {
        return this.format;
    }

    public int getDepthFormat() {
        return this.depthFormat;
    }

    public static Builder builder(int width, int height, int colorAttachments, boolean hasDepthAttachment) {
        return new Builder(width, height, colorAttachments, hasDepthAttachment);
    }

    public static Builder builder(VulkanImage colorAttachment, VulkanImage depthAttachment) {
        return new Builder(colorAttachment, depthAttachment);
    }

    public static Builder builder(java.util.List<VulkanImage> colorAttachments, VulkanImage depthAttachment) {
        return new Builder(colorAttachments, depthAttachment);
    }

    public static class Builder {
        final boolean createImages;
        final int width, height;
        int format, depthFormat;

        VulkanImage colorAttachment;
        java.util.List<VulkanImage> colorAttachmentList; // MRT
        VulkanImage depthAttachment;

//        int colorAttachments;
        boolean hasColorAttachment;
        boolean hasDepthAttachment;

        boolean linearFiltering;
        boolean depthLinearFiltering;

        public Builder(int width, int height, int colorAttachments, boolean hasDepthAttachment) {
            Validate.isTrue(colorAttachments > 0 || hasDepthAttachment, "At least 1 attachment needed");

            this.createImages = true;
            this.format = DEFAULT_FORMAT;
            this.depthFormat = Vulkan.getDefaultDepthFormat();
            this.linearFiltering = true;
            this.depthLinearFiltering = false;

            this.width = width;
            this.height = height;
            this.hasColorAttachment = colorAttachments >= 1;
            this.hasDepthAttachment = hasDepthAttachment;
        }

        public Builder(VulkanImage colorAttachment, VulkanImage depthAttachment) {
            this.createImages = false;
            this.colorAttachment = colorAttachment;
            this.depthAttachment = depthAttachment;

            this.format = colorAttachment.format;

            this.width = colorAttachment.width;
            this.height = colorAttachment.height;
            this.hasColorAttachment = true;
            this.hasDepthAttachment = depthAttachment != null;

            this.depthFormat = this.hasDepthAttachment ? depthAttachment.format : 0;
            this.linearFiltering = true;
            this.depthLinearFiltering = false;
        }

        public Builder(java.util.List<VulkanImage> colorAttachments, VulkanImage depthAttachment) {
            Validate.isTrue(!colorAttachments.isEmpty(), "At least 1 color attachment needed");

            this.createImages = false;
            this.colorAttachment = colorAttachments.get(0);
            this.colorAttachmentList = colorAttachments;
            this.depthAttachment = depthAttachment;

            this.format = colorAttachments.get(0).format;

            this.width = colorAttachments.get(0).width;
            this.height = colorAttachments.get(0).height;
            this.hasColorAttachment = true;
            this.hasDepthAttachment = depthAttachment != null;

            this.depthFormat = this.hasDepthAttachment ? depthAttachment.format : 0;
            this.linearFiltering = true;
            this.depthLinearFiltering = false;
        }

        public Framebuffer build() {
            return new Framebuffer(this);
        }

        public Builder setFormat(int format) {
            this.format = format;

            return this;
        }

        public Builder setLinearFiltering(boolean b) {
            this.linearFiltering = b;

            return this;
        }

        public Builder setDepthLinearFiltering(boolean b) {
            this.depthLinearFiltering = b;

            return this;
        }

    }
}
