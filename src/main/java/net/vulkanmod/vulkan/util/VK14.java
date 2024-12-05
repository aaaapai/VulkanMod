package net.vulkanmod.vulkan.util;

import org.lwjgl.system.Checks;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.vulkan.VkCopyImageToImageInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Set;

import static org.lwjgl.system.APIUtil.apiLogMissing;
import static org.lwjgl.system.Checks.checkFunctions;
import static org.lwjgl.system.Checks.reportMissing;
import static org.lwjgl.system.JNI.callPPP;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.vulkan.VK10.VK_MAKE_API_VERSION;

public class VK14 {


    /** The API version number for Vulkan 1.4. */
    public static final int VK_API_VERSION_1_4 = VK_MAKE_API_VERSION(0, 1, 4, 0);

    //From Core Header: https://github.com/KhronosGroup/Vulkan-Headers/commit/49af1bfe467dd5a9efc22f7867d95fdde50e2b00#diff-e222ae95c2b0d5082b94d6086fb1c24da18ee31384c1a39840df3b9152023ee6R438
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_4_FEATURES = 55;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_4_PROPERTIES = 56;
    public static final int VK_STRUCTURE_TYPE_DEVICE_QUEUE_GLOBAL_PRIORITY_CREATE_INFO = 1000174000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GLOBAL_PRIORITY_QUERY_FEATURES = 1000388000;
    public static final int VK_STRUCTURE_TYPE_QUEUE_FAMILY_GLOBAL_PRIORITY_PROPERTIES = 1000388001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_SUBGROUP_ROTATE_FEATURES = 1000416000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_FLOAT_CONTROLS_2_FEATURES = 1000528000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_EXPECT_ASSUME_FEATURES = 1000544000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_LINE_RASTERIZATION_FEATURES = 1000259000;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_LINE_STATE_CREATE_INFO = 1000259001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_LINE_RASTERIZATION_PROPERTIES = 1000259002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_ATTRIBUTE_DIVISOR_PROPERTIES = 1000525000;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_DIVISOR_STATE_CREATE_INFO = 1000190001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_ATTRIBUTE_DIVISOR_FEATURES = 1000190002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INDEX_TYPE_UINT8_FEATURES = 1000265000;
    public static final int VK_STRUCTURE_TYPE_MEMORY_MAP_INFO = 1000271000;
    public static final int VK_STRUCTURE_TYPE_MEMORY_UNMAP_INFO = 1000271001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_5_FEATURES = 1000470000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_5_PROPERTIES = 1000470001;
    public static final int VK_STRUCTURE_TYPE_RENDERING_AREA_INFO = 1000470003;
    public static final int VK_STRUCTURE_TYPE_DEVICE_IMAGE_SUBRESOURCE_INFO = 1000470004;
    public static final int VK_STRUCTURE_TYPE_SUBRESOURCE_LAYOUT_2 = 1000338002;
    public static final int VK_STRUCTURE_TYPE_IMAGE_SUBRESOURCE_2 = 1000338003;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_CREATE_FLAGS_2_CREATE_INFO = 1000470005;
    public static final int VK_STRUCTURE_TYPE_BUFFER_USAGE_FLAGS_2_CREATE_INFO = 1000470006;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PUSH_DESCRIPTOR_PROPERTIES = 1000080000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES = 1000232000;
    public static final int VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_LOCATION_INFO = 1000232001;
    public static final int VK_STRUCTURE_TYPE_RENDERING_INPUT_ATTACHMENT_INDEX_INFO = 1000232002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_6_FEATURES = 1000545000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_6_PROPERTIES = 1000545001;
    public static final int VK_STRUCTURE_TYPE_BIND_MEMORY_STATUS = 1000545002;
    public static final int VK_STRUCTURE_TYPE_BIND_DESCRIPTOR_SETS_INFO = 1000545003;
    public static final int VK_STRUCTURE_TYPE_PUSH_CONSTANTS_INFO = 1000545004;
    public static final int VK_STRUCTURE_TYPE_PUSH_DESCRIPTOR_SET_INFO = 1000545005;
    public static final int VK_STRUCTURE_TYPE_PUSH_DESCRIPTOR_SET_WITH_TEMPLATE_INFO = 1000545006;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_PROTECTED_ACCESS_FEATURES = 1000466000;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_ROBUSTNESS_CREATE_INFO = 1000068000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_FEATURES = 1000068001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_PROPERTIES = 1000068002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_HOST_IMAGE_COPY_FEATURES = 1000270000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_HOST_IMAGE_COPY_PROPERTIES = 1000270001;
    public static final int VK_STRUCTURE_TYPE_MEMORY_TO_IMAGE_COPY = 1000270002;
    public static final int VK_STRUCTURE_TYPE_IMAGE_TO_MEMORY_COPY = 1000270003;
    public static final int VK_STRUCTURE_TYPE_COPY_IMAGE_TO_MEMORY_INFO = 1000270004;
    public static final int VK_STRUCTURE_TYPE_COPY_MEMORY_TO_IMAGE_INFO = 1000270005;
    public static final int VK_STRUCTURE_TYPE_HOST_IMAGE_LAYOUT_TRANSITION_INFO = 1000270006;
    public static final int VK_STRUCTURE_TYPE_COPY_IMAGE_TO_IMAGE_INFO = 1000270007;
    public static final int VK_STRUCTURE_TYPE_SUBRESOURCE_HOST_MEMCPY_SIZE = 1000270008;
    public static final int VK_STRUCTURE_TYPE_HOST_IMAGE_COPY_DEVICE_PERFORMANCE_QUERY = 1000270009;
//    private static final boolean vulkan14;
//
//
//    static
//    {
//
//        VkPhysicalDevice physicalDevice, VkDeviceCreateInfo ci, int apiVersion
//        //Stolen from VKCapabilitiesDevice
//        vulkan14 = check_VK14(functionName -> {
//            long address = callPPP(handle, memAddress(functionName), GetDeviceProcAddr);
//            if (address == NULL && Checks.DEBUG_FUNCTIONS) {
//                apiLogMissing("VK device", functionName);
//            }
//            return address;
//        }, )
//    }
//
//    private static boolean check_VK14(FunctionProvider provider, long[] caps, Set<String> ext) {
//        if (!ext.contains("Vulkan14")) {
//            return false;
//        }
//
//        return checkFunctions(provider, caps, new int[] {
//                        0
//                },
//                "vkCopyImageToImage, vkCmdPushDescriptors"
//        ) || reportMissing("VK", "Vulkan14");
//    }
//
//
//
//    public static int vkCopyImageToImage(VkDevice device, VkCopyImageToImageInfoEXT pCopyImageToImageInfo)
//    {
//
//    }
//
//    typedef struct VkPhysicalDeviceVulkan14Features {
//        VkStructureType     sType;
//        void*               pNext;
//        VkBool32            globalPriorityQuery;
//        VkBool32            shaderSubgroupRotate;
//        VkBool32            shaderSubgroupRotateClustered;
//        VkBool32            shaderFloatControls2;
//        VkBool32            shaderExpectAssume;
//        VkBool32            rectangularLines;
//        VkBool32            bresenhamLines;
//        VkBool32            smoothLines;
//        VkBool32            stippledRectangularLines;
//        VkBool32            stippledBresenhamLines;
//        VkBool32            stippledSmoothLines;
//        VkBool32            vertexAttributeInstanceRateDivisor;
//        VkBool32            vertexAttributeInstanceRateZeroDivisor;
//        VkBool32            indexTypeUint8;
//        VkBool32            dynamicRenderingLocalRead;
//        VkBool32            maintenance5;
//        VkBool32            maintenance6;
//        VkBool32            pipelineProtectedAccess;
//        VkBool32            pipelineRobustness;
//        VkBool32            hostImageCopy;
//        VkBool32            pushDescriptor;
//    } VkPhysicalDeviceVulkan14Features;
}
