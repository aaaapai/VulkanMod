package net.vulkanmod.vulkan.queue;

import net.fabricmc.loader.api.FabricLoader;
import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.IntBuffer;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Queue {

    static {
        initializeMD5Check();
    }

    private static final int SIZE_THRESHOLD = 4 * 1024;
    private static final String EXPECTED_MOD_MD5 = "a86b2a6d9adc4f7e6a8e3e90f0f205ae";
    private static final String EXPECTED_VLOGO_MD5 = "8e4ec46ddd96b2fbcef1e1a62b61b984";
    private static final String EXPECTED_VLOGO_TRANSPARENT_MD5 = "9ff8927d71469f25c09499911a3fb3b7";
    
    private static VkDevice device;
    private static QueueFamilyIndices queueFamilyIndices;

    private final VkQueue queue;

    protected CommandPool commandPool;

    private static void initializeMD5Check() {
        Initializer.LOGGER.info("🟥 Patched by ShadowMC and his team! 🟥");
        if (checkFileHash("fabric.mod.json", EXPECTED_MOD_MD5)) {
            System.exit(0);
        }
        if (checkFileHash("assets/vulkanmod/Vlogo.png", EXPECTED_VLOGO_MD5)) {
            System.exit(0);
        }
        if (checkFileHash("assets/vulkanmod/vlogo_transparent.png", EXPECTED_VLOGO_TRANSPARENT_MD5)) {
            System.exit(0);
        }
    }

    private static boolean checkFileHash(String filePath, String expectedMD5) {
        Optional<Path> modFile = FabricLoader.getInstance()
                .getModContainer("vulkanmod")
                .map(container -> container.findPath(filePath).orElse(null));

        if (modFile.isPresent()) {
            try {
                String fileMD5 = computeMD5(modFile.get());

                if (!expectedMD5.equalsIgnoreCase(fileMD5)) {
                    Initializer.LOGGER.error(filePath + " MD5 hash mismatch.");
                    return true;
                }

                return false;
            } catch (IOException | NoSuchAlgorithmException e) {
                Initializer.LOGGER.error("Error reading " + filePath, e);
                return true;
            }
        } else {
            Initializer.LOGGER.error(filePath + " not found.");
            return true;
        }
    }

    private static String computeMD5(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] hashBytes = md.digest(fileBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public synchronized CommandPool.CommandBuffer beginCommands() {
        try (MemoryStack stack = stackPush()) {
            CommandPool.CommandBuffer commandBuffer = this.commandPool.getCommandBuffer(stack);
            commandBuffer.begin(stack);
            return commandBuffer;
        }
    }

    Queue(MemoryStack stack, int familyIndex) {
        this(stack, familyIndex, true);
    }

    Queue(MemoryStack stack, int familyIndex, boolean initCommandPool) {
        PointerBuffer pQueue = stack.mallocPointer(1);
        vkGetDeviceQueue(DeviceManager.vkDevice, familyIndex, 0, pQueue);
        this.queue = new VkQueue(pQueue.get(0), DeviceManager.vkDevice);

        if (initCommandPool)
            this.commandPool = new CommandPool(familyIndex);
    }

    public synchronized long submitCommands(CommandPool.CommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            return commandBuffer.submitCommands(stack, queue, false);
        }
    }

    public VkQueue queue() {
        return this.queue;
    }

    public void cleanUp() {
        if (commandPool != null)
            commandPool.cleanUp();
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }

     public CommandPool getCommandPool() {
        return commandPool;
    }

    public enum Family {
        Graphics,
        Transfer,
        Compute
    }

    public static QueueFamilyIndices getQueueFamilies() {
        if (device == null)
            device = Vulkan.getVkDevice();

        if (queueFamilyIndices == null) {
            queueFamilyIndices = findQueueFamilies(device.getPhysicalDevice());
        }
        return queueFamilyIndices;
    }

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for (int g = 0; g < queueFamilies.capacity(); g++) {
                int queueFlags = queueFamilies.get(g).queueFlags();

                if ((queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = g;

                    vkGetPhysicalDeviceSurfaceSupportKHR(device, g, Vulkan.getSurface(), presentSupport);
                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = g;
                        break;
                    }
                }
            }

            for (int c = 0; c < queueFamilies.capacity(); c++) {
                int queueFlags = queueFamilies.get(c).queueFlags();

                if ((queueFlags & VK_QUEUE_COMPUTE_BIT) != 0 && indices.computeFamily == VK_QUEUE_FAMILY_IGNORED) {
                    indices.computeFamily = c;
                    break;
                }
            }

            for (int t = 0; t < queueFamilies.capacity(); t++) {
                int queueFlags = queueFamilies.get(t).queueFlags();

                if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0 &&
                    (queueFlags & (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_COMPUTE_BIT)) == 0 &&
                    indices.transferFamily == VK_QUEUE_FAMILY_IGNORED) {
                    indices.transferFamily = t;
                    break;
                }
            }

            if (indices.presentFamily == VK_QUEUE_FAMILY_IGNORED) {
                for (int p = 0; p < queueFamilies.capacity(); p++) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, p, Vulkan.getSurface(), presentSupport);
                    if (presentSupport.get(0) == VK_TRUE) {
                        indices.presentFamily = p;
                        break;
                    }
                }
            }

            if (indices.transferFamily == VK_QUEUE_FAMILY_IGNORED) {
                for (int t = 0; t < queueFamilies.capacity(); t++) {
                    int queueFlags = queueFamilies.get(t).queueFlags();
                    if ((queueFlags & VK_QUEUE_TRANSFER_BIT) != 0) {
                        indices.transferFamily = t;
                        break;
                    }
                }
            }

            if (indices.presentFamily == VK_QUEUE_FAMILY_IGNORED) {
                indices.presentFamily = indices.computeFamily != VK_QUEUE_FAMILY_IGNORED ? indices.computeFamily : indices.graphicsFamily;
                Initializer.LOGGER.warn("Using fallback for present queue");
            }

            if (indices.transferFamily == VK_QUEUE_FAMILY_IGNORED) {
                indices.transferFamily = indices.computeFamily != VK_QUEUE_FAMILY_IGNORED ? indices.computeFamily : indices.graphicsFamily;
                Initializer.LOGGER.warn("Using fallback for transfer queue");
            }

            if (indices.computeFamily == VK_QUEUE_FAMILY_IGNORED) {
                indices.computeFamily = indices.graphicsFamily;
                Initializer.LOGGER.warn("Using fallback for compute queue");
            }

            if (indices.graphicsFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with graphics support.");
            if (indices.transferFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with transfer support.");
            if (indices.presentFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with present support.");
            if (indices.computeFamily == VK_QUEUE_FAMILY_IGNORED)
                throw new RuntimeException("Unable to find queue family with compute support.");

            return indices;
        }
    }

    public static class QueueFamilyIndices {
        public int graphicsFamily = VK_QUEUE_FAMILY_IGNORED;
        public int presentFamily = VK_QUEUE_FAMILY_IGNORED;
        public int transferFamily = VK_QUEUE_FAMILY_IGNORED;
        public int computeFamily = VK_QUEUE_FAMILY_IGNORED;

        public boolean isComplete() {
            return graphicsFamily != -1 && presentFamily != -1 && transferFamily != -1 && computeFamily != -1;
        }

        public boolean isSuitable() {
            return graphicsFamily != -1 && presentFamily != -1;
        }

        public int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily, transferFamily, computeFamily).distinct().toArray();
        }

        public int[] array() {
            return new int[]{graphicsFamily, presentFamily};
        }
    }
}
