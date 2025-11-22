package net.vulkanmod.util;

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
        MemoryUtil.memPutFloat(ptr + idx, f);
    }

    public void putInt(int idx, int f) {
        MemoryUtil.memPutInt(ptr + idx, f);
    }

    public float getFloat(int idx) {
        return MemoryUtil.memGetFloat(ptr + idx);
    }

    public int getInt(int idx) {
        return MemoryUtil.memGetInt(ptr + idx);
    }
}
