package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal Vulkan-backed buffer wrapper. Actual upload and command submission
 * will be implemented during later stages of the refactor.
 */
public final class VulkanGpuBuffer extends GpuBuffer {

    private final VulkanGpuDevice device;
    private final String label;
    private final Buffer backingBuffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    VulkanGpuBuffer(VulkanGpuDevice device, String label, int usage, int size) {
        super(usage, size);
        this.device = device;
        this.label = label;
        this.backingBuffer = new Buffer(usage, MemoryTypes.HOST_MEM);
        this.backingBuffer.createBuffer(size);
    }

    public VulkanGpuDevice device() {
        return device;
    }

    public Buffer backingBuffer() {
        return backingBuffer;
    }

    public String label() {
        return label;
    }

    void upload(ByteBuffer data) {
        backingBuffer.copyBuffer(data, data.remaining());
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            backingBuffer.scheduleFree();
        }
    }

    @Override
    public com.mojang.blaze3d.buffers.GpuBufferSlice slice(int offset, int length) {
        return new com.mojang.blaze3d.buffers.GpuBufferSlice(this, offset, length);
    }

    @Override
    public com.mojang.blaze3d.buffers.GpuBufferSlice slice() {
        return new com.mojang.blaze3d.buffers.GpuBufferSlice(this, 0, (int) backingBuffer.getBufferSize());
    }

    MemorySegment mapSegment(int offset, int length) {
        long base = backingBuffer.getDataPtr() + offset;
        return MemorySegment.ofAddress(base).reinterpret(length);
    }

    static final class VulkanMappedView implements GpuBuffer.MappedView {
        private final MemorySegment segment;
        private final ByteBuffer view;

        VulkanMappedView(MemorySegment segment) {
            this.segment = segment;
            this.view = segment.asByteBuffer();
        }

        @Override
        public ByteBuffer data() {
            return view;
        }

        @Override
        public void close() {
            // Host coherent memory, nothing to flush for now.
        }
    }
}
