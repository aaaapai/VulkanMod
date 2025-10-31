package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.buffers.GpuFence;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight fence placeholder backed by a {@link CompletableFuture}. The
 * implementation will be updated to use Vulkan timeline semaphores once the
 * command encoder is fully functional.
 */
public final class VulkanGpuFence implements GpuFence {

    private final CompletableFuture<Void> signal = new CompletableFuture<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public void signal() {
        signal.complete(null);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            signal.complete(null);
        }
    }

    @Override
    public boolean awaitCompletion(long nanosTimeout) {
        if (signal.isDone()) {
            return true;
        }

        if (nanosTimeout == 0L) {
            return signal.isDone();
        }

        try {
            if (nanosTimeout < 0L) {
                signal.get();
            } else {
                signal.get(nanosTimeout, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
