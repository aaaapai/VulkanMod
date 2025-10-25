package net.vulkanmod.render.util;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;

public final class MatrixUniformBuffer implements AutoCloseable {
    private final GpuBuffer buffer;
    private final GpuBufferSlice slice;

    public MatrixUniformBuffer(String debugName) {
        this.buffer = RenderSystem.getDevice().createBuffer(() -> debugName, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        this.slice = this.buffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public GpuBufferSlice upload(Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f((Matrix4fc) matrix).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), data);
        }
        return this.slice;
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}

