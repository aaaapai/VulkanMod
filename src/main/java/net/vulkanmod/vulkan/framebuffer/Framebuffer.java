package net.vulkanmod.vulkan.framebuffer;

import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.VUtil;
import org.apache.commons.lang3.Validate;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkViewport;

import java.lang.foreign.Arena;
import java.nio.LongBuffer;
import java.util.Arrays;

import static net.vulkanmod.vulkan.Vulkan.DYNAMIC_RENDERING;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCmdSetScissor;
import static org.lwjgl.vulkan.VK10.vkCmdSetViewport;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;

public class Framebuffer {
    public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_UNORM;

//    private long id;
    private final Reference2LongArrayMap<RenderPass> renderpassToFramebufferMap = new Reference2LongArrayMap<>();
    protected int format;
    protected int depthFormat;
    protected int width, height;
    protected boolean linearFiltering;
    protected boolean depthLinearFiltering;
    protected int attachmentCount;
    protected VulkanImage depthAttachment;
    boolean hasColorAttachment;
    boolean hasDepthAttachment;
    private VulkanImage colorAttachment;

    //SwapChain
    protected Framebuffer() {
    }

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
        }
    }

    public static Builder builder(int width, int height, int colorAttachments, boolean hasDepthAttachment) {
        return new Builder(width, height, colorAttachments, hasDepthAttachment);
    }

    public static Builder builder(VulkanImage colorAttachment, VulkanImage depthAttachment) {
        return new Builder(colorAttachment, depthAttachment);
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
        try (Arena arena = Arena.ofConfined()) {
            LongBuffer attachments = colorAttachment != null && depthAttachment != null
                ? VUtil.longBuffer(arena, colorAttachment.getImageView(), depthAttachment.getImageView())
                : colorAttachment != null
                    ? VUtil.longBuffer(arena, colorAttachment.getImageView())
                    : null;

            if (attachments == null) {
                throw new IllegalStateException("Framebuffer must have at least one attachment");
            }

            LongBuffer pFramebuffer = VUtil.allocateLongBuffer(arena, 1);

            VkFramebufferCreateInfo framebufferInfo = VUtil.struct(
                arena,
                VkFramebufferCreateInfo.SIZEOF,
                VkFramebufferCreateInfo.ALIGNOF,
                VkFramebufferCreateInfo::create
            );
            framebufferInfo.sType$Default();
            framebufferInfo.renderPass(renderPass.getId());
            framebufferInfo.width(this.width);
            framebufferInfo.height(this.height);
            framebufferInfo.layers(1);
            framebufferInfo.pAttachments(attachments);

            if (vkCreateFramebuffer(Vulkan.getVkDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer");
            }

            return pFramebuffer.get(0);
        }
    }

    public void beginRenderPass(VkCommandBuffer commandBuffer, RenderPass renderPass) {
        if (!DYNAMIC_RENDERING) {
            long framebufferId = this.getFramebufferId(renderPass);
            try (Arena arena = Arena.ofConfined()) {
                renderPass.beginRenderPass(commandBuffer, framebufferId, arena);
            }
        } else {
            try (Arena arena = Arena.ofConfined()) {
                renderPass.beginDynamicRendering(commandBuffer, arena, this);
            }
        }

        Renderer.getInstance().setBoundRenderPass(renderPass);
        Renderer.getInstance().setBoundFramebuffer(this);

        Renderer.setViewportState(0, 0, this.width, this.height);
        Renderer.setScissor(0, 0, this.width, this.height);
    }

    protected long getFramebufferId(RenderPass renderPass) {
        return this.renderpassToFramebufferMap.computeIfAbsent(renderPass, renderPass1 -> createFramebuffer(renderPass));
    }

    public void applyViewport(VkCommandBuffer commandBuffer) {
        try (Arena arena = Arena.ofConfined()) {
            VkViewport.Buffer viewport = VUtil.structBuffer(
                arena,
                VkViewport.SIZEOF,
                VkViewport.ALIGNOF,
                1,
                VkViewport::create
            );
            viewport.x(0.0f);
            viewport.y(this.height);
            viewport.width(this.width);
            viewport.height(-this.height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            vkCmdSetViewport(commandBuffer, 0, viewport);
        }
    }

    public void applyScissor(VkCommandBuffer commandBuffer) {
        try (Arena arena = Arena.ofConfined()) {
            VkRect2D.Buffer scissor = VUtil.structBuffer(
                arena,
                VkRect2D.SIZEOF,
                VkRect2D.ALIGNOF,
                1,
                VkRect2D::create
            );
            scissor.offset().set(0, 0);
            scissor.extent().set(this.width, this.height);

            vkCmdSetScissor(commandBuffer, 0, scissor);
        }
    }

    public void cleanUp() {
        cleanUp(true);
    }

    public void cleanUp(boolean cleanImages) {
        if (cleanImages) {
            if (this.colorAttachment != null)
                this.colorAttachment.free();

            if (this.depthAttachment != null)
                this.depthAttachment.free();
        }

        final VkDevice device = Vulkan.getVkDevice();
        final var ids = renderpassToFramebufferMap.values().toLongArray();

        MemoryManager.getInstance().addFrameOp(
                () -> Arrays.stream(ids).forEach(id ->
                        vkDestroyFramebuffer(device, id, null))
        );


        renderpassToFramebufferMap.clear();
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

    public static class Builder {
        final boolean createImages;
        final int width, height;
        int format, depthFormat;

        VulkanImage colorAttachment;
        VulkanImage depthAttachment;

        //        int colorAttachments;
        boolean hasColorAttachment;
        boolean hasDepthAttachment;

        boolean linearFiltering;
        boolean depthLinearFiltering;

        public Builder(int width, int height, int colorAttachments, boolean hasDepthAttachment) {
            Validate.isTrue(colorAttachments > 0 || hasDepthAttachment, "At least 1 attachment needed");

            //TODO multi color attachments
            Validate.isTrue(colorAttachments <= 1, "Not supported");

            this.createImages = true;
            this.format = DEFAULT_FORMAT;
            this.depthFormat = Vulkan.getDefaultDepthFormat();
            this.linearFiltering = true;
            this.depthLinearFiltering = false;

            this.width = width;
            this.height = height;
            this.hasColorAttachment = colorAttachments == 1;
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
