package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import oshi.hardware.CentralProcessor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SystemInfo {
    private static final String PROC_CPUINFO = "/proc/cpuinfo";
    private static final String SOC_MANUFACTURER_PROP = "/system/bin/getprop ro.soc.manufacturer";
    private static final String SOC_MODEL_PROP = "/system/bin/getprop ro.soc.model";
    
    public static final String cpuInfo = isRunningOnAndroid() ? getCPUNameSafely() : getCPUNameForDesktop();

    public static String getCPUNameSafely() {
        return Optional.ofNullable(getCPUNameFromProp())
                      .orElseGet(() -> Optional.ofNullable(getCPUNameFromProc())
                      .orElse("Unknown CPU"));
    }

    private static String getCPUNameFromProc() {
        try {
            return Files.lines(Paths.get(PROC_CPUINFO))
                .filter(line -> line.startsWith("Hardware") || line.startsWith("model name"))
                .findFirst()
                .map(line -> {
                    String value = line.split(":", 2)[1].strip();
                    return line.startsWith("Hardware") ? value + " (SoC)" : value;
                }).orElse(null);
        } catch (IOException e) {
            Initializer.LOGGER.warn("Failed to read CPU info from proc", e);
            return null;
        }
    }

    private static String getCPUNameFromProp() {
        String manufacturer = executeCommand(SOC_MANUFACTURER_PROP);
        String model = executeCommand(SOC_MODEL_PROP);
        
        if (manufacturer == null) return model != null ? model : null;
        else return model != null ? manufacturer + " " + model
                                  : "A " + manufacturer + " CPU";
    }

    private static String executeCommand(String command) {
        try {
            Process process = new ProcessBuilder(command.split(" ")).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                if (process.waitFor() != 0) {
                    Initializer.LOGGER.warn("Command failed: " + command);
                    return null;
                }
                
                return reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            Initializer.LOGGER.warn("Failed to execute command: " + command, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static String getCPUNameForDesktop() {
        try {
            CentralProcessor centralProcessor = new oshi.SystemInfo().getHardware().getProcessor();
            return String.format("%s", centralProcessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
        } catch (NoClassDefFoundError | Exception e) {
            return getCPUNameSafely();
        }
    }

    private static boolean isRunningOnAndroid() {
        boolean hasAndroidEnvironmentVars = System.getenv("POJAV_ENVIRON") != null ||
                                            System.getenv("SCL_ENVIRON") != null ||
                                            System.getenv("SCL_RENDERER") != null ||
                                            System.getenv("POJAV_RENDERER") != null;
        boolean isBuildPropAvailable = Files.exists(Paths.get("/system/build.prop"));
        return (hasAndroidEnvironmentVars || isBuildPropAvailable);
    }
}
