package net.vulkanmod.vulkan;

import oshi.hardware.CentralProcessor;

public class SystemInfo {
    public static final String cpuInfo;

    static {
        CentralProcessor centralProcessor = null;
        try {
        CentralProcessor centralProcessor = new oshi.SystemInfo().getHardware().getProcessor();
        } catch (NoClassDefFoundError e){
Initializer.LOGGER.warn("Failed to initialise OSHI class, no CPU info will be available");
        }
        centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ") :
                "Unknown";
    }
}
