package net.vulkanmod.render.core.backend;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.vulkanmod.util.VUtil;
import org.lwjgl.vulkan.*;

import java.lang.foreign.Arena;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Encapsulates the per-frame Vulkan synchronization objects (semaphores + fences).
 * Having a dedicated owner makes it easier to tighten memory-safety around raw handles.
 */
public final class FrameSyncManager implements AutoCloseable {

    private final VkDevice device;
    private final LongArrayList imageAvailableSemaphores = new LongArrayList();
    private final LongArrayList renderFinishedSemaphores = new LongArrayList();
    private final LongArrayList inFlightFences = new LongArrayList();

    public FrameSyncManager(VkDevice device, int frames) {
        this.device = device;
        recreate(frames);
    }

    public void recreate(int frames) {
        destroyHandles();

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

            LongBuffer pImageAvailable = VUtil.allocateLongBuffer(arena, 1);
            LongBuffer pRenderFinished = VUtil.allocateLongBuffer(arena, 1);
            LongBuffer pFence = VUtil.allocateLongBuffer(arena, 1);

            for (int i = 0; i < frames; i++) {
                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailable) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinished) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create synchronization objects for frame " + i);
                }

                imageAvailableSemaphores.add(pImageAvailable.get(0));
                renderFinishedSemaphores.add(pRenderFinished.get(0));
                inFlightFences.add(pFence.get(0));
            }
        }
    }

    public long imageAvailableSemaphore(int frameIndex) {
        return imageAvailableSemaphores.getLong(frameIndex);
    }

    public long renderFinishedSemaphore(int frameIndex) {
        return renderFinishedSemaphores.getLong(frameIndex);
    }

    public long fenceHandle(int frameIndex) {
        return inFlightFences.getLong(frameIndex);
    }

    public void waitForFence(int frameIndex, long timeout) {
        vkWaitForFences(device, fenceHandle(frameIndex), true, timeout);
    }

    public void resetFence(int frameIndex) {
        vkResetFences(device, fenceHandle(frameIndex));
    }

    @Override
    public void close() {
        destroyHandles();
    }

    private void destroyHandles() {
        for (int i = 0; i < imageAvailableSemaphores.size(); i++) {
            vkDestroySemaphore(device, imageAvailableSemaphores.getLong(i), null);
        }
        for (int i = 0; i < renderFinishedSemaphores.size(); i++) {
            vkDestroySemaphore(device, renderFinishedSemaphores.getLong(i), null);
        }
        for (int i = 0; i < inFlightFences.size(); i++) {
            vkDestroyFence(device, inFlightFences.getLong(i), null);
        }
        imageAvailableSemaphores.clear();
        renderFinishedSemaphores.clear();
        inFlightFences.clear();
    }
}
