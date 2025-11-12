package net.vulkanmod.vulkan;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.mixin.window.WindowAccessor;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.core.Drawer;
import net.vulkanmod.render.core.VRenderSystem;
import net.vulkanmod.render.chunk.buffer.UploadManager;
import net.vulkanmod.render.profiling.Profiler;
import net.vulkanmod.render.texture.ImageUploadHelper;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.shader.*;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.util.VUtil;
import net.vulkanmod.vulkan.VkResult;
import org.lwjgl.system.MemoryStack;
import net.vulkanmod.util.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mojang.blaze3d.opengl.GlConst.GL_COLOR_BUFFER_BIT;
import static com.mojang.blaze3d.opengl.GlConst.GL_DEPTH_BUFFER_BIT;
import static net.vulkanmod.vulkan.Vulkan.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Renderer {
    public static boolean skipRendering = false;
    private static Renderer INSTANCE;
    private static VkDevice device;
    private static boolean swapChainUpdate = false;
    private static int currentFrame = 0;
    private static int imageIndex;
    private static int lastReset = -1;
    private final Set<Pipeline> usedPipelines = new ObjectOpenHashSet<>();
    private final List<Runnable> onResizeCallbacks = new ObjectArrayList<>();
    MainPass mainPass;
    private Pipeline boundPipeline;
    private long boundPipelineHandle;

    private Drawer drawer;

    private SwapChain swapChain;

    private int framesNum;
    private List<VkCommandBuffer> commandBuffers;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;

    private Framebuffer boundFramebuffer;
    private RenderPass boundRenderPass;
    private VkCommandBuffer currentCmdBuffer;
    private boolean recordingCmds = false;
    public Renderer() {
        device = Vulkan.getVkDevice();
        framesNum = Initializer.CONFIG.frameQueueSize;
    }

    public static void initRenderer() {
        INSTANCE = new Renderer();
        INSTANCE.init();
    }

    public static Renderer getInstance() {
        return INSTANCE;
    }

    public static Drawer getDrawer() {
        return INSTANCE.drawer;
    }

    public static int getCurrentFrame() {
        return currentFrame;
    }

    public static int getCurrentImage() {
        return imageIndex;
    }

    public static void setLineWidth(float width) {
        if (INSTANCE.boundFramebuffer == null) {
            return;
        }
        vkCmdSetLineWidth(INSTANCE.currentCmdBuffer, width);
    }

    private static void resetDynamicState(VkCommandBuffer commandBuffer) {
        vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);

        vkCmdSetLineWidth(commandBuffer, 1.0F);
    }

    public static void setDepthBias(float constant, float slope) {
        VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

        vkCmdSetDepthBias(commandBuffer, constant, 0.0f, slope);
    }

    public static void clearAttachments(int v) {
        Framebuffer framebuffer = Renderer.getInstance().boundFramebuffer;
        if (framebuffer == null) return;

        clearAttachments(v, framebuffer.getWidth(), framebuffer.getHeight());
    }

    public static void clearAttachments(int v, int width, int height) {
        if (skipRendering) return;

        try (Arena arena = Arena.ofConfined()) {
            VkClearValue colorValue = VUtil.struct(
                arena,
                VkClearValue.SIZEOF,
                VkClearValue.ALIGNOF,
                VkClearValue::create
            );
            float clearR = VRenderSystem.clearColor.get(0);
            float clearG = VRenderSystem.clearColor.get(1);
            float clearB = VRenderSystem.clearColor.get(2);
            float clearA = VRenderSystem.clearColor.get(3);
            colorValue.color().float32(0, clearR);
            colorValue.color().float32(1, clearG);
            colorValue.color().float32(2, clearB);
            colorValue.color().float32(3, clearA);

            VkClearValue depthValue = VUtil.struct(
                arena,
                VkClearValue.SIZEOF,
                VkClearValue.ALIGNOF,
                VkClearValue::create
            );
            depthValue.depthStencil().set(VRenderSystem.clearDepthValue, 0);

            int attachmentsCount = v == (GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT) ? 2 : 1;
            VkClearAttachment.Buffer attachments = VUtil.structBuffer(
                arena,
                VkClearAttachment.SIZEOF,
                VkClearAttachment.ALIGNOF,
                attachmentsCount,
                VkClearAttachment::create
            );

            switch (v) {
                case GL_DEPTH_BUFFER_BIT -> {
                    VkClearAttachment clearDepth = attachments.get(0);
                    clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                    clearDepth.colorAttachment(0);
                    clearDepth.clearValue(depthValue);
                }
                case GL_COLOR_BUFFER_BIT -> {
                    VkClearAttachment clearColor = attachments.get(0);
                    clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    clearColor.colorAttachment(0);
                    clearColor.clearValue(colorValue);
                }
                case GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT -> {
                    VkClearAttachment clearColor = attachments.get(0);
                    clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    clearColor.colorAttachment(0);
                    clearColor.clearValue(colorValue);

                    VkClearAttachment clearDepth = attachments.get(1);
                    clearDepth.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                    clearDepth.colorAttachment(0);
                    clearDepth.clearValue(depthValue);
                }
                default -> throw new RuntimeException("unexpected value");
            }

            VkClearRect.Buffer rect = VUtil.structBuffer(
                arena,
                VkClearRect.SIZEOF,
                VkClearRect.ALIGNOF,
                1,
                VkClearRect::create
            );
            rect.get(0).rect().offset().set(0, 0);
            rect.get(0).rect().extent().set(width, height);
            rect.get(0).baseArrayLayer(0);
            rect.get(0).layerCount(1);

            vkCmdClearAttachments(INSTANCE.currentCmdBuffer, attachments, rect);
        }
    }

    public static void setInvertedViewport(int x, int y, int width, int height) {
        setViewportState(x, y + height, width, -height);
    }

    public static void resetViewport() {
        int width = INSTANCE.getSwapChain().getWidth();
        int height = INSTANCE.getSwapChain().getHeight();

        setViewportState(0, 0, width, height);
    }

    public static void setViewportState(int x, int y, int width, int height) {
        GlStateManager._viewport(x, y, width, height);
    }

    public static void setViewport(int x, int y, int width, int height) {
        if (!INSTANCE.recordingCmds) return;

        try (Arena arena = Arena.ofConfined()) {
            VkViewport.Buffer viewport = VUtil.structBuffer(
                arena,
                VkViewport.SIZEOF,
                VkViewport.ALIGNOF,
                1,
                VkViewport::create
            );
            viewport.x(x);
            viewport.y(height + y);
            viewport.width(width);
            viewport.height(-height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            vkCmdSetViewport(INSTANCE.currentCmdBuffer, 0, viewport);
        }
    }

    public static void setScissor(int x, int y, int width, int height) {
        if (INSTANCE.boundFramebuffer == null) return;

        try (Arena arena = Arena.ofConfined()) {
            int framebufferHeight = INSTANCE.boundFramebuffer.getHeight();

            x = Math.max(0, x);

            VkRect2D.Buffer scissor = VUtil.structBuffer(
                arena,
                VkRect2D.SIZEOF,
                VkRect2D.ALIGNOF,
                1,
                VkRect2D::create
            );
            scissor.offset().set(x, framebufferHeight - (y + height));
            scissor.extent().set(width, height);

            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void resetScissor() {
        if (INSTANCE.boundFramebuffer == null) return;

        INSTANCE.boundFramebuffer.applyScissor(INSTANCE.currentCmdBuffer);
    }

    public static void pushDebugSection(String s) {
        if (Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            try (Arena arena = Arena.ofConfined()) {
                VkDebugUtilsLabelEXT markerInfo = VUtil.struct(
                    arena,
                    VkDebugUtilsLabelEXT.SIZEOF,
                    VkDebugUtilsLabelEXT.ALIGNOF,
                    VkDebugUtilsLabelEXT::create
                );
                markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
                ByteBuffer label = VUtil.utf8String(arena, s);
                markerInfo.pLabelName(label);
                vkCmdBeginDebugUtilsLabelEXT(commandBuffer, markerInfo);
            }
        }
    }

    public static void popDebugSection() {
        if (Vulkan.ENABLE_VALIDATION_LAYERS) {
            VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

            vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        }
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }

    public static int getFramesNum() {
        return INSTANCE.framesNum;
    }

    public static VkCommandBuffer getCommandBuffer() {
        return INSTANCE.currentCmdBuffer;
    }

    public static boolean isRecording() {
        return INSTANCE.recordingCmds;
    }

    public static void scheduleSwapChainUpdate() {
        swapChainUpdate = true;
    }

    private void init() {
        MemoryManager.createInstance(Renderer.getFramesNum());
        Vulkan.createStagingBuffers();

        swapChain = new SwapChain();
        mainPass = DefaultMainPass.create();

        drawer = new Drawer();
        drawer.createResources(framesNum);

        Uniforms.setupDefaultUniforms();
        PipelineManager.init();
        UploadManager.createInstance();

        allocateCommandBuffers();
        createSyncObjects();
    }

    private void allocateCommandBuffers() {
        if (commandBuffers != null) {
            commandBuffers.forEach(commandBuffer -> vkFreeCommandBuffers(device, Vulkan.getCommandPool(), commandBuffer));
        }

        commandBuffers = new ArrayList<>(framesNum);

        try (Arena arena = Arena.ofConfined()) {
            VkCommandBufferAllocateInfo allocInfo = VUtil.struct(
                arena,
                VkCommandBufferAllocateInfo.SIZEOF,
                VkCommandBufferAllocateInfo.ALIGNOF,
                VkCommandBufferAllocateInfo::create
            );
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(framesNum);

            MemorySegment pCommandBuffers = arena.allocate(ValueLayout.ADDRESS, framesNum);

            int vkResult = VK10.nvkAllocateCommandBuffers(device, allocInfo.address(), pCommandBuffers.address());
            if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers: %s".formatted(VkResult.decode(vkResult)));
            }

            long stride = ValueLayout.ADDRESS.byteSize();
            for (int i = 0; i < framesNum; i++) {
                MemorySegment handleSegment = pCommandBuffers.get(ValueLayout.ADDRESS, (long) i * stride);
                long handle = handleSegment.address();
                commandBuffers.add(new VkCommandBuffer(handle, device));
            }
        }
    }

    private void createSyncObjects() {
        imageAvailableSemaphores = new ArrayList<>(framesNum);
        renderFinishedSemaphores = new ArrayList<>(framesNum);
        inFlightFences = new ArrayList<>(framesNum);

        try (Arena arena = Arena.ofConfined()) {
            VkSemaphoreCreateInfo semaphoreInfo = VUtil.struct(
                arena,
                VkSemaphoreCreateInfo.SIZEOF,
                VkSemaphoreCreateInfo.ALIGNOF,
                VkSemaphoreCreateInfo::create
            );
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VUtil.struct(
                arena,
                VkFenceCreateInfo.SIZEOF,
                VkFenceCreateInfo.ALIGNOF,
                VkFenceCreateInfo::create
            );
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = VUtil.allocateLongBuffer(arena, 1);
            LongBuffer pRenderFinishedSemaphore = VUtil.allocateLongBuffer(arena, 1);
            LongBuffer pFence = VUtil.allocateLongBuffer(arena, 1);

            for (int i = 0; i < framesNum; i++) {
                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                    || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                    || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create synchronization objects for the frame: " + i);
                }

                imageAvailableSemaphores.add(pImageAvailableSemaphore.get(0));
                renderFinishedSemaphores.add(pRenderFinishedSemaphore.get(0));
                inFlightFences.add(pFence.get(0));
            }
        }
    }

    public void preInitFrame() {
        Profiler p = Profiler.getMainProfiler();
        p.pop();
        p.round();
        p.push("Frame_ops");

        Minecraft minecraft = Minecraft.getInstance();
        float frameTime = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float baseTime = minecraft.level != null ? (float) minecraft.level.getGameTime() : 0.0f;
        VRenderSystem.setShaderGameTime(baseTime + frameTime);

        // runTick might be called recursively,
        // this check forces sync to avoid upload corruption
        if (lastReset == currentFrame) {
            waitFences();
        }
        lastReset = currentFrame;

        drawer.resetBuffers(currentFrame);

        WorldRenderer.getInstance().uploadSections();
        UploadManager.INSTANCE.submitUploads();
    }

    public void beginFrame() {
        Profiler p = Profiler.getMainProfiler();
        p.pop();
        p.push("Frame_fence");

        if (swapChainUpdate) {
            recreateSwapChain();
            swapChainUpdate = false;

            if (getSwapChain().getWidth() == 0 && getSwapChain().getHeight() == 0) {
                skipRendering = true;
                Minecraft.getInstance().noRender = true;
            } else {
                skipRendering = false;
                Minecraft.getInstance().noRender = false;
            }
        }


        if (skipRendering || recordingCmds) return;

        vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

        p.pop();
        p.push("Begin_rendering");

        MemoryManager.getInstance().initFrame(currentFrame);
        drawer.setCurrentFrame(currentFrame);

        resetDescriptors();

        currentCmdBuffer = commandBuffers.get(currentFrame);
        vkResetCommandBuffer(currentCmdBuffer, 0);

        try (MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, swapChain.getId(), VUtil.UINT64_MAX, imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);

            if (vkResult == VK_SUBOPTIMAL_KHR || vkResult == VK_ERROR_OUT_OF_DATE_KHR || swapChainUpdate) {
                swapChainUpdate = true;
                skipRendering = true;
                beginFrame();

                return;
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot acquire next swap chain image: %s".formatted(VkResult.decode(vkResult)));
            }

            imageIndex = pImageIndex.get(0);

            this.beginRenderPass();
        }

        p.pop();
    }

    private void beginRenderPass() {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        try (Arena arena = Arena.ofConfined()) {
            VkCommandBufferBeginInfo beginInfo = VUtil.struct(
                arena,
                VkCommandBufferBeginInfo.SIZEOF,
                VkCommandBufferBeginInfo.ALIGNOF,
                VkCommandBufferBeginInfo::create
            );
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            int vkResult = vkBeginCommandBuffer(commandBuffer, beginInfo);
            if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer: %s".formatted(VkResult.decode(vkResult)));
            }
        }

        recordingCmds = true;
        mainPass.begin(commandBuffer);

        resetDynamicState(commandBuffer);
    }

    public void endFrame() {
        if (skipRendering || !recordingCmds) return;

        Profiler p = Profiler.getMainProfiler();
        p.push("End_rendering");

        if (Initializer.CONFIG.enableRayTracing) {
            doRayTracing();
        }

        mainPass.end(currentCmdBuffer);

        waitFences();

        submitFrame();
        recordingCmds = false;

        p.pop();
        p.push("Post_rendering");
    }

    private void submitFrame() {
        if (swapChainUpdate) return;

        try (MemoryStack stack = stackPush()) {
            int vkResult;

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame)));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pSignalSemaphores(stack.longs(renderFinishedSemaphores.get(currentFrame)));
            submitInfo.pCommandBuffers(stack.pointers(currentCmdBuffer));

            vkResetFences(device, inFlightFences.get(currentFrame));

            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, inFlightFences.get(currentFrame));
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphores.get(currentFrame)));

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapChain.getId()));

            presentInfo.pImageIndices(stack.ints(imageIndex));

            vkResult = vkQueuePresentKHR(DeviceManager.getPresentQueue().queue(), presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || swapChainUpdate) {
                swapChainUpdate = true;
                return;
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present rendered frame: %s".formatted(VkResult.decode(vkResult)));
            }

            currentFrame = (currentFrame + 1) % framesNum;
        }
    }

    /**
     * Called in case draw results are needed before the end of the frame
     */
    public void flushCmds() {
        if (!this.recordingCmds) return;

        try (MemoryStack stack = stackPush()) {
            int vkResult;

            this.endRenderPass(currentCmdBuffer);
            vkEndCommandBuffer(currentCmdBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.pCommandBuffers(stack.pointers(currentCmdBuffer));

            vkResetFences(device, inFlightFences.get(currentFrame));

            waitFences();

            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, inFlightFences.get(currentFrame));
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }

            vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);
        }

        this.beginRenderPass();
    }

    public void endRenderPass() {
        endRenderPass(currentCmdBuffer);
    }

    public void endRenderPass(VkCommandBuffer commandBuffer) {
        if (skipRendering || !recordingCmds || this.boundFramebuffer == null) return;

        if (!DYNAMIC_RENDERING) this.boundRenderPass.endRenderPass(currentCmdBuffer);
        else KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

        this.boundRenderPass = null;
        this.boundFramebuffer = null;

        VkGlFramebuffer.resetBoundFramebuffer();
    }

    public boolean beginRendering(RenderPass renderPass, Framebuffer framebuffer) {
        if (skipRendering || !recordingCmds) return false;

        if (this.boundFramebuffer != framebuffer) {
            this.endRenderPass(currentCmdBuffer);
            framebuffer.beginRenderPass(currentCmdBuffer, renderPass);

            this.boundFramebuffer = framebuffer;
        }
        return true;
    }

    public void addUsedPipeline(Pipeline pipeline) {
        usedPipelines.add(pipeline);
    }

    public void removeUsedPipeline(Pipeline pipeline) {
        usedPipelines.remove(pipeline);
    }

    private void waitFences() {
        // Make sure there are no uploads/transitions scheduled
        ImageUploadHelper.INSTANCE.submitCommands();
        Synchronization.INSTANCE.waitFences();
        Vulkan.getStagingBuffer().reset();
    }

    private void resetDescriptors() {
        for (Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
        boundPipeline = null;
        boundPipelineHandle = 0;
    }

    void waitForSwapChain() {
        vkResetFences(device, inFlightFences.get(currentFrame));

//        constexpr VkPipelineStageFlags t=VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            //Empty Submit
            VkSubmitInfo info = VkSubmitInfo.calloc(stack).sType$Default().pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame))).pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));

            vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), info, inFlightFences.get(currentFrame));
            vkWaitForFences(device, inFlightFences.get(currentFrame), true, -1);
        }
    }

    @SuppressWarnings("UnreachableCode")
    private void recreateSwapChain() {
        waitFences();
        Vulkan.waitIdle();

        commandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));
        recordingCmds = false;

        swapChain.recreate();

        //Semaphores need to be recreated in order to make them unsignaled
        destroySyncObjects();

        int newFramesNum = Initializer.CONFIG.frameQueueSize;

        if (framesNum != newFramesNum) {
            UploadManager.INSTANCE.submitUploads();

            framesNum = newFramesNum;
            MemoryManager.getInstance().freeAllBuffers();
            MemoryManager.createInstance(newFramesNum);
            createStagingBuffers();
            allocateCommandBuffers();

            Pipeline.recreateDescriptorSets(framesNum);

            drawer.createResources(framesNum);
        }

        createSyncObjects();
        this.mainPass.onResize();

        this.onResizeCallbacks.forEach(Runnable::run);
        ((WindowAccessor) (Object) Minecraft.getInstance().getWindow()).getEventHandler().resizeDisplay();

        currentFrame = 0;
    }

    public void cleanUpResources() {
        WorldRenderer.getInstance().cleanUp();
        destroySyncObjects();

        drawer.cleanUpResources();
        mainPass.cleanUp();
        swapChain.cleanUp();

        PipelineManager.destroyPipelines();
        VTextureSelector.getWhiteTexture().free();
    }

    private void destroySyncObjects() {
        for (int i = 0; i < framesNum; ++i) {
            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }
    }

    public void addOnResizeCallback(Runnable runnable) {
        this.onResizeCallbacks.add(runnable);
    }

    public void bindGraphicsPipeline(GraphicsPipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PipelineState currentState = PipelineState.getCurrentPipelineState(boundRenderPass);
        final long handle = pipeline.getHandle(currentState);

        if (boundPipelineHandle == handle) {
            return;
        }

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, handle);
        boundPipelineHandle = handle;
        boundPipeline = pipeline;
        addUsedPipeline(pipeline);
    }

    public void bindGraphicsPipeline(RenderPipeline pipeline, VertexFormat vertexFormat) {
        GraphicsPipeline graphicsPipeline = PipelineManager.getPipeline(pipeline, vertexFormat);
        bindGraphicsPipeline(graphicsPipeline);
    }

    private void doRayTracing() {
        RayTracingPipeline pipeline = PipelineManager.getRayTracingPipeline();
        if (pipeline == null) {
            return;
        }
        bindRayTracingPipeline(pipeline);

        uploadAndBindUBOs(pipeline);

        try (MemoryStack stack = stackPush()) {
            VkStridedDeviceAddressRegionKHR raygenShaderSbtEntry = VkStridedDeviceAddressRegionKHR.calloc(stack);
            raygenShaderSbtEntry.deviceAddress(pipeline.getSbtBufferAddress());
            raygenShaderSbtEntry.stride(pipeline.getSbtStride());
            raygenShaderSbtEntry.size(pipeline.getSbtStride());

            VkStridedDeviceAddressRegionKHR missShaderSbtEntry = VkStridedDeviceAddressRegionKHR.calloc(stack);
            missShaderSbtEntry.deviceAddress(pipeline.getSbtBufferAddress() + pipeline.getSbtStride());
            missShaderSbtEntry.stride(pipeline.getSbtStride());
            missShaderSbtEntry.size(pipeline.getSbtStride());

            VkStridedDeviceAddressRegionKHR hitShaderSbtEntry = VkStridedDeviceAddressRegionKHR.calloc(stack);
            hitShaderSbtEntry.deviceAddress(pipeline.getSbtBufferAddress() + 2 * pipeline.getSbtStride());
            hitShaderSbtEntry.stride(pipeline.getSbtStride());
            hitShaderSbtEntry.size(pipeline.getSbtStride());

            VkStridedDeviceAddressRegionKHR callableShaderSbtEntry = VkStridedDeviceAddressRegionKHR.calloc(stack);

            KHRRayTracingPipeline.vkCmdTraceRaysKHR(currentCmdBuffer, raygenShaderSbtEntry, missShaderSbtEntry, hitShaderSbtEntry, callableShaderSbtEntry, swapChain.getWidth(), swapChain.getHeight(), 1);
        }
    }

    public void bindRayTracingPipeline(RayTracingPipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        final long handle = pipeline.getHandle();

        if (boundPipelineHandle == handle) {
            return;
        }

        vkCmdBindPipeline(commandBuffer, KHRRayTracingPipeline.VK_PIPELINE_BIND_POINT_RAY_TRACING_KHR, handle);
        boundPipelineHandle = handle;
        boundPipeline = pipeline;
        addUsedPipeline(pipeline);
    }

    public void uploadAndBindUBOs(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;
        pipeline.bindDescriptorSets(commandBuffer, currentFrame);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PushConstants pushConstants = pipeline.getPushConstants();

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = stack.malloc(pushConstants.getSize());
            long ptr = MemoryUtil.memAddress0(buffer);
            pushConstants.update(ptr);

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.getSize(), ptr);
        }

    }

    public Pipeline getBoundPipeline() {
        return boundPipeline;
    }

    public Framebuffer getBoundFramebuffer() {
        return boundFramebuffer;
    }

    public void setBoundFramebuffer(Framebuffer framebuffer) {
        this.boundFramebuffer = framebuffer;
    }

    public RenderPass getBoundRenderPass() {
        return boundRenderPass;
    }

    public void setBoundRenderPass(RenderPass boundRenderPass) {
        this.boundRenderPass = boundRenderPass;
    }

    public MainPass getMainPass() {
        return this.mainPass;
    }

    public void setMainPass(MainPass mainPass) {
        this.mainPass = mainPass;
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }
}
