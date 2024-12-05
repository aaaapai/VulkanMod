package net.vulkanmod.vulkan.device;

import net.vulkanmod.vulkan.util.VK14;
import net.vulkanmod.vulkan.util.VkPhysicalDeviceVulkan14Features;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkEnumerateInstanceVersion;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;

public class Device {
    final VkPhysicalDevice physicalDevice;
    final VkPhysicalDeviceProperties2 properties;

    private final int vendorId;
    public final String vendorIdString;
    public final String deviceName;
    public final String driverVersion;
    public final String vkVersion;

    public final VkPhysicalDeviceFeatures2 availableFeatures;

//    public final VkPhysicalDeviceVulkan13Features availableFeatures13;
    private final boolean vulkan14;

    private final boolean drawIndirectSupported, hostImageCopy, pushDescriptor;

    public Device(VkPhysicalDevice device) {
        this.physicalDevice = device;
//        int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_4_FEATURES   = 55;
//        int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_4_PROPERTIES = 56;
        //Cleanup/Avoid unused heap allocations
        try(MemoryStack stack = MemoryStack.stackPush()) {

            VkPhysicalDeviceVulkan12Properties deviceVulkan12Properties = VkPhysicalDeviceVulkan12Properties.malloc(stack).sType$Default();

            properties = VkPhysicalDeviceProperties2.calloc().sType$Default().pNext(deviceVulkan12Properties);
            VK11.vkGetPhysicalDeviceProperties2(physicalDevice, properties);

            this.vendorId = properties.properties().vendorID();
            this.vendorIdString = deviceVulkan12Properties.driverNameString();
            this.deviceName = properties.properties().deviceNameString();
            this.driverVersion = deviceVulkan12Properties.driverInfoString();
            this.vkVersion = decDefVersion(properties.properties().apiVersion());

            this.availableFeatures = VkPhysicalDeviceFeatures2.calloc();
            this.availableFeatures.sType$Default();
            //TODO: Bad Alignment: Custom struct alignment is broken
            VkPhysicalDeviceVulkan14Features availableFeatures14 = VkPhysicalDeviceVulkan14Features.malloc(stack).sType(VK14.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_4_FEATURES);

            this.availableFeatures.pNext(availableFeatures14.address());

            this.vulkan14 = VK_VERSION_MINOR(properties.properties().apiVersion()) > 3; //VkCapabilitiesDevice.Vulkan1* is outdated and does not include vk14

            vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.availableFeatures);

            this.drawIndirectSupported = this.availableFeatures.features().multiDrawIndirect();
            this.hostImageCopy = availableFeatures14.hostImageCopy();
            this.pushDescriptor = availableFeatures14.pushDescriptor();
        }

    }

    static String decDefVersion(int v) {
        return VK_VERSION_MAJOR(v) + "." + VK_VERSION_MINOR(v) + "." + VK_VERSION_PATCH(v);
    }

    public Set<String> getUnsupportedExtensions(Set<String> requiredExtensions) {
        try (MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, availableExtensions);

            Set<String> extensions = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet());

            Set<String> unsupportedExtensions = new HashSet<>(requiredExtensions);
            unsupportedExtensions.removeAll(extensions);

            return unsupportedExtensions;
        }
    }

    public boolean isDrawIndirectSupported() {
        return drawIndirectSupported;
    }

    public boolean isHostImageCopy() {
        return hostImageCopy;
    }

    public boolean isPushDescriptor() {
        return pushDescriptor;
    }

    // Added these to allow detecting GPU vendor, to allow handling vendor specific circumstances:
    // (e.g. such as in case we encounter a vendor specific driver bug)
    public boolean isAMD() {
        return vendorId == 0x1022;
    }

    public boolean isNvidia() {
        return vendorId == 0x10DE;
    }

    public boolean isIntel() {
        return vendorId == 0x8086;
    }
}
