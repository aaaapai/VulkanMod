package net.vulkanmod.vulkan.util;

import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.Collection;

import static org.lwjgl.system.MemoryStack.stackGet;

public class VUtil {
    public static final boolean CHECKS = true;

    public static final int UINT32_MAX = 0xFFFFFFFF;
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    // Safe memory access methods using FFM (Foreign Function & Memory API)
    public static void putFloat(long address, float value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_FLOAT, 0, value);
    }

    public static void putInt(long address, int value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_INT, 0, value);
    }

    public static void putLong(long address, long value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_LONG, 0, value);
    }

    public static float getFloat(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_FLOAT, 0);
    }

    public static int getInt(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_INT, 0);
    }

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {

        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream().map(stack::UTF8).forEach(buffer::put);

        return buffer.rewind();
    }

    public static void memcpy(ByteBuffer src, long dstPtr) {
        MemorySegment srcSegment = MemorySegment.ofBuffer(src);
        MemorySegment dstSegment = MemorySegment.ofAddress(dstPtr).reinterpret(src.capacity());
        dstSegment.copyFrom(srcSegment);
    }

    public static void memcpy(ByteBuffer src, Buffer dst, long size) {
        if (CHECKS) {
            if (size > dst.getBufferSize() - dst.getUsedBytes()) {
                throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
            }
        }

        final long dstPtr = dst.getDataPtr() + dst.getUsedBytes();
        MemorySegment srcSegment = MemorySegment.ofBuffer(src).asSlice(0, size);
        MemorySegment dstSegment = MemorySegment.ofAddress(dstPtr).reinterpret(size);
        dstSegment.copyFrom(srcSegment);
    }

    public static void memcpy(Buffer src, ByteBuffer dst, long size) {
        if (CHECKS) {
            if (size > dst.remaining()) {
                throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
            }
        }

        final long srcPtr = src.getDataPtr();
        MemorySegment srcSegment = MemorySegment.ofAddress(srcPtr).reinterpret(size);
        MemorySegment dstSegment = MemorySegment.ofBuffer(dst).asSlice(0, size);
        dstSegment.copyFrom(srcSegment);
    }

    public static void memcpy(ByteBuffer src, Buffer dst, long size, long srcOffset, long dstOffset) {
        if (CHECKS) {
            if (size > dst.getBufferSize() - dstOffset) {
                throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
            }
        }

        final long dstPtr = dst.getDataPtr() + dstOffset;

        // Get source address from ByteBuffer using FFM
        MemorySegment srcBuffer = MemorySegment.ofBuffer(src);
        MemorySegment srcSegment = srcBuffer.asSlice(srcOffset, size);
        MemorySegment dstSegment = MemorySegment.ofAddress(dstPtr).reinterpret(size);
        dstSegment.copyFrom(srcSegment);
    }

    public static int align(int x, int align) {
        int r = x % align;
        return r == 0 ? x : x + align - r;
    }

}
