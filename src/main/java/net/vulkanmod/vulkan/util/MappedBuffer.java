package net.vulkanmod.vulkan.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class MappedBuffer {

    public final ByteBuffer buffer;
    public final long ptr;

    MappedBuffer(ByteBuffer buffer, long ptr) {
        this.buffer = buffer;
        this.ptr = ptr;
    }

    public MappedBuffer(int size) {
        this.buffer = MemoryUtil.memAlloc(size);
        this.ptr = MemoryUtil.memAddress0(this.buffer);
    }

    public static MappedBuffer createFromBuffer(ByteBuffer buffer) {
        return new MappedBuffer(buffer, MemoryUtil.memAddress0(buffer));
    }

    public void putFloat(int idx, float f) {
        VUtil.putFloat(ptr + idx, f);
    }

    public void putInt(int idx, int f) {
        VUtil.putInt(ptr + idx, f);
    }

    public float getFloat(int idx) {
        return VUtil.getFloat(ptr + idx);
    }

    public int getInt(int idx) {
        return VUtil.getInt(ptr + idx);
    }
}
