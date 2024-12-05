package net.vulkanmod.vulkan.util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

public class VkPhysicalDeviceVulkan14Features extends Struct<VkPhysicalDeviceVulkan14Features> implements NativeResource {

    /** The struct size in bytes. */
    public static final int SIZEOF;

    /** The struct alignment in bytes. */
    public static final int ALIGNOF;

    private static final int
            STYPE,
            PNEXT,
            GLOBALPRIORITYQUERY,
            SHADERSUBGROUPROTATE,
            SHADERSUBGROUPROTATECLUSTERED,
            SHADERFLOATCONTROLS2,
            SHADEREXPECTASSUME,
            RECTANGULARLINES,
            BRESENHAMLINES,
            SMOOTHLINES,
            STIPPLEDRECTANGULARLINES,
            STIPPLEDBRESENHAMLINES,
            STIPPLEDSMOOTHLINES,
            VERTEXATTRIBUTEINSTANCERATEDIVISOR,
            VERTEXATTRIBUTEINSTANCERATEZERODIVISOR,
            INDEXTYPEUINT8,
            DYNAMICRENDERINGLOCALREAD,
            MAINTENANCE5,
            MAINTENANCE6,
            PIPELINEPROTECTEDACCESS,
            PIPELINEROBUSTNESS,
            HOSTIMAGECOPY,
            PUSHDESCRIPTOR;

    static {
        Layout layout = __struct(
                __member(4),
                __member(POINTER_SIZE),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4),
                __member(4)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        STYPE = layout.offsetof(0);
        PNEXT = layout.offsetof(1);
        GLOBALPRIORITYQUERY = layout.offsetof(2);
        SHADERSUBGROUPROTATE = layout.offsetof(3);
        SHADERSUBGROUPROTATECLUSTERED = layout.offsetof(4);
        SHADERFLOATCONTROLS2 = layout.offsetof(5);
        SHADEREXPECTASSUME = layout.offsetof(6);
        RECTANGULARLINES = layout.offsetof(7);
        BRESENHAMLINES = layout.offsetof(8);
        SMOOTHLINES = layout.offsetof(9);
        STIPPLEDRECTANGULARLINES = layout.offsetof(10);
        STIPPLEDBRESENHAMLINES = layout.offsetof(11);
        STIPPLEDSMOOTHLINES = layout.offsetof(12);
        VERTEXATTRIBUTEINSTANCERATEDIVISOR = layout.offsetof(13);
        VERTEXATTRIBUTEINSTANCERATEZERODIVISOR = layout.offsetof(14);
        INDEXTYPEUINT8 = layout.offsetof(15);
        DYNAMICRENDERINGLOCALREAD = layout.offsetof(16);
        MAINTENANCE5 = layout.offsetof(17);
        MAINTENANCE6 = layout.offsetof(18);
        PIPELINEPROTECTEDACCESS = layout.offsetof(19);
        PIPELINEROBUSTNESS = layout.offsetof(20);
        HOSTIMAGECOPY = layout.offsetof(21);
        PUSHDESCRIPTOR = layout.offsetof(22);
    }
    /**
     * Creates a struct instance at the specified address.
     *
     * @param address   the struct memory address
     * @param container an optional container buffer, to be referenced strongly by the struct instance.
     */
    protected VkPhysicalDeviceVulkan14Features(long address, ByteBuffer container) {
        super(address, container);
    }

//public boolean globalPriorityQuery() { }
//public boolean shaderSubgroupRotate() { }
//public boolean shaderSubgroupRotateClustered() { }
//public boolean shaderFloatControls2() { }
//public boolean shaderExpectAssume() { }
//public boolean rectangularLines() { }
//public boolean bresenhamLines() { }
//public boolean smoothLines() { }
//public boolean stippledRectangularLines() { }
//public boolean stippledBresenhamLines() { }
//public boolean stippledSmoothLines() { }
//public boolean vertexAttributeInstanceRateDivisor() { }
//public boolean vertexAttributeInstanceRateZeroDivisor() { }
//public boolean indexTypeUint8() { }
//public boolean dynamicRenderingLocalRead() { }
//public boolean maintenance5() { }
//public boolean maintenance6() { }
//public boolean pipelineProtectedAccess() { }
//public boolean pipelineRobustness() { }

    public VkPhysicalDeviceVulkan14Features hostImageCopy(boolean a) { UNSAFE.putInt(null, address() + HOSTIMAGECOPY, a ? 1 : 0); return this; }

    public boolean hostImageCopy() { return UNSAFE.getBoolean(null, this.address + HOSTIMAGECOPY); }

    public VkPhysicalDeviceVulkan14Features pushDescriptor(boolean a) { UNSAFE.putInt(null, address() + PUSHDESCRIPTOR, a ? 1 : 0); return this; }

    public boolean pushDescriptor() { return UNSAFE.getBoolean(null, this.address + PUSHDESCRIPTOR); }


    public static VkPhysicalDeviceVulkan14Features malloc(MemoryStack stack) {
        return new VkPhysicalDeviceVulkan14Features(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    public static VkPhysicalDeviceVulkan14Features calloc(MemoryStack stack) {
        return new VkPhysicalDeviceVulkan14Features(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    public static void nsType(long struct, int value) { UNSAFE.putInt(null, struct + STYPE, value); }

    public VkPhysicalDeviceVulkan14Features sType(@NativeType("VkStructureType") int value) { nsType(address(), value); return this; }
//    public VkPhysicalDeviceVulkan14Features sType$Default() { return sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES); }

    @Override
    protected VkPhysicalDeviceVulkan14Features create(long address, ByteBuffer container) {
        return null;
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    @Override
    public void free() {

    }

    @Override
    public void close() {
        NativeResource.super.close();
    }
}