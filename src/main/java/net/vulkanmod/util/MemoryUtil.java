package net.vulkanmod.util;

import org.lwjgl.PointerBuffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Replacement for LWJGL's MemoryUtil that relies on the Foreign Function &amp; Memory (FFM) API.
 */
public final class MemoryUtil {
    public static final long NULL = 0L;

    private static final Map<Buffer, Arena> BUFFER_ARENAS = Collections.synchronizedMap(new IdentityHashMap<>());

    private MemoryUtil() {
    }

    private static ByteBuffer configure(ByteBuffer buffer) {
        return buffer.order(ByteOrder.nativeOrder());
    }

    private static void register(Buffer buffer, Arena arena) {
        BUFFER_ARENAS.put(buffer, arena);
    }

    private static Arena remove(Buffer buffer) {
        return BUFFER_ARENAS.remove(buffer);
    }

    public static ByteBuffer memAlloc(int bytes) {
        Arena arena = Arena.ofShared();
        ByteBuffer buffer = configure(arena.allocate(bytes, 1).asByteBuffer());
        register(buffer, arena);
        return buffer;
    }

    public static IntBuffer memAllocInt(int elements) {
        Arena arena = Arena.ofShared();
        IntBuffer buffer = configure(arena.allocate((long) elements * Integer.BYTES, Integer.BYTES).asByteBuffer()).asIntBuffer();
        register(buffer, arena);
        return buffer;
    }

    public static LongBuffer memAllocLong(int elements) {
        Arena arena = Arena.ofShared();
        LongBuffer buffer = configure(arena.allocate((long) elements * Long.BYTES, Long.BYTES).asByteBuffer()).asLongBuffer();
        register(buffer, arena);
        return buffer;
    }

    public static FloatBuffer memAllocFloat(int elements) {
        Arena arena = Arena.ofShared();
        FloatBuffer buffer = configure(arena.allocate((long) elements * Float.BYTES, Float.BYTES).asByteBuffer()).asFloatBuffer();
        register(buffer, arena);
        return buffer;
    }

    public static FloatBuffer memCallocFloat(int elements) {
        FloatBuffer buffer = memAllocFloat(elements);
        memSet(memAddress(buffer), (byte) 0, (long) elements * Float.BYTES);
        return buffer;
    }

    public static PointerBuffer memAllocPointer(int capacity) {
        Arena arena = Arena.ofShared();
        MemorySegment segment = arena.allocate((long) capacity * Long.BYTES, Long.BYTES);
        PointerBuffer buffer = PointerBuffer.create(segment.address(), capacity);
        register(buffer, arena);
        return buffer;
    }

    public static void memFree(Buffer buffer) {
        if (buffer == null) {
            return;
        }

        Arena arena = remove(buffer);
        if (arena != null) {
            arena.close();
        }
    }

    public static void memPutFloat(long address, float value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_FLOAT, 0, value);
    }

    public static void memPutInt(long address, int value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_INT, 0, value);
    }

    public static void memPutShort(long address, short value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_SHORT, 0, value);
    }

    public static void memPutLong(long address, long value) {
        MemorySegment.ofAddress(address).set(ValueLayout.JAVA_LONG, 0, value);
    }

    public static float memGetFloat(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_FLOAT, 0);
    }

    public static int memGetInt(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_INT, 0);
    }

    public static short memGetShort(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_SHORT, 0);
    }

    public static long memGetLong(long address) {
        return MemorySegment.ofAddress(address).get(ValueLayout.JAVA_LONG, 0);
    }

    public static long memAddress(Buffer buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return MemorySegment.ofBuffer(buffer).address();
    }

    public static long memAddress0(Buffer buffer) {
        return buffer == null ? NULL : memAddress(buffer);
    }

    public static void memCopy(long src, long dst, long bytes) {
        MemorySegment source = MemorySegment.ofAddress(src).reinterpret(bytes);
        MemorySegment destination = MemorySegment.ofAddress(dst).reinterpret(bytes);
        destination.copyFrom(source);
    }

    public static void memCopy(ByteBuffer src, ByteBuffer dst) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(dst, "dst");

        if (dst.remaining() < src.remaining()) {
            throw new IllegalArgumentException("Destination buffer is smaller than source buffer");
        }

        ByteBuffer srcDup = src.duplicate();
        ByteBuffer dstDup = dst.duplicate();
        dstDup.put(srcDup);
    }

    public static void memSet(long address, byte value, long bytes) {
        MemorySegment.ofAddress(address).reinterpret(bytes).fill(value);
    }

    public static ByteBuffer memByteBuffer(long address, int capacity) {
        MemorySegment segment = MemorySegment.ofAddress(address).reinterpret(capacity);
        ByteBuffer buffer = configure(segment.asByteBuffer());
        buffer.limit(capacity);
        buffer.position(0);
        return buffer;
    }

    public static ByteBuffer memSlice(ByteBuffer buffer, int offset, int length) {
        Objects.requireNonNull(buffer, "buffer");
        ByteBuffer dup = buffer.duplicate();
        dup.position(offset);
        dup.limit(offset + length);
        ByteBuffer slice = dup.slice();
        slice.order(buffer.order());
        return slice;
    }

    public static String memASCII(long address) {
        if (address == NULL) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        long offset = 0;

        while (true) {
            byte value = MemorySegment.ofAddress(address + offset).get(ValueLayout.JAVA_BYTE, 0);
            if (value == 0) {
                break;
            }
            builder.append((char) (value & 0xFF));
            offset++;
        }

        return builder.toString();
    }
}
