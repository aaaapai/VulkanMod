package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRRayTracingPipeline.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceProperties2;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;

public class RayTracingPipeline extends Pipeline {

    private static final int VK_BUFFER_USAGE_SHADER_BINDING_TABLE_BIT_KHR = 0x00000400;

    private long pipelineHandle;

    private long raygenShaderModule = 0;
    private long missShaderModule = 0;
    private long chitShaderModule = 0;

    private Buffer shaderBindingTable;
    private long sbtBufferAddress;
    private int sbtStride;

    RayTracingPipeline(Builder builder) {
        super(builder.shaderPath);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.imageDescriptors = builder.imageDescriptors;
        this.pushConstants = builder.pushConstants;

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(builder.raygenShaderSPIRV, builder.missShaderSPIRV, builder.chitShaderSPIRV);

        createRayTracingPipeline();

        createDescriptorSets(3); //Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    private static int alignUp(int value, int alignment) {
        long aligned = ((long) value + alignment - 1L) / alignment * alignment;
        return (int) aligned;
    }

    private void createShaderModules(SPIRVUtils.SPIRV raygenShaderSPIRV, SPIRVUtils.SPIRV missShaderSPIRV, SPIRVUtils.SPIRV chitShaderSPIRV) {
        this.raygenShaderModule = createShaderModule(raygenShaderSPIRV.bytecode());
        this.missShaderModule = createShaderModule(missShaderSPIRV.bytecode());
        this.chitShaderModule = createShaderModule(chitShaderSPIRV.bytecode());
    }

    private void createRayTracingPipeline() {
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(3, stack);

            // Ray Generation Shader
            shaderStages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStages.get(0).stage(VK_SHADER_STAGE_RAYGEN_BIT_KHR);
            shaderStages.get(0).module(raygenShaderModule);
            shaderStages.get(0).pName(stack.UTF8("main"));

            // Miss Shader
            shaderStages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStages.get(1).stage(VK_SHADER_STAGE_MISS_BIT_KHR);
            shaderStages.get(1).module(missShaderModule);
            shaderStages.get(1).pName(stack.UTF8("main"));

            // Closest Hit Shader
            shaderStages.get(2).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStages.get(2).stage(VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR);
            shaderStages.get(2).module(chitShaderModule);
            shaderStages.get(2).pName(stack.UTF8("main"));

            VkRayTracingShaderGroupCreateInfoKHR.Buffer shaderGroups = VkRayTracingShaderGroupCreateInfoKHR.calloc(3, stack);

            // Ray Generation Shader Group
            shaderGroups.get(0).sType(VK_STRUCTURE_TYPE_RAY_TRACING_SHADER_GROUP_CREATE_INFO_KHR);
            shaderGroups.get(0).type(VK_RAY_TRACING_SHADER_GROUP_TYPE_GENERAL_KHR);
            shaderGroups.get(0).generalShader(0);
            shaderGroups.get(0).closestHitShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(0).anyHitShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(0).intersectionShader(VK_SHADER_UNUSED_KHR);

            // Miss Shader Group
            shaderGroups.get(1).sType(VK_STRUCTURE_TYPE_RAY_TRACING_SHADER_GROUP_CREATE_INFO_KHR);
            shaderGroups.get(1).type(VK_RAY_TRACING_SHADER_GROUP_TYPE_GENERAL_KHR);
            shaderGroups.get(1).generalShader(1);
            shaderGroups.get(1).closestHitShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(1).anyHitShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(1).intersectionShader(VK_SHADER_UNUSED_KHR);

            // Closest Hit Shader Group
            shaderGroups.get(2).sType(VK_STRUCTURE_TYPE_RAY_TRACING_SHADER_GROUP_CREATE_INFO_KHR);
            shaderGroups.get(2).type(VK_RAY_TRACING_SHADER_GROUP_TYPE_TRIANGLES_HIT_GROUP_KHR);
            shaderGroups.get(2).generalShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(2).closestHitShader(2);
            shaderGroups.get(2).anyHitShader(VK_SHADER_UNUSED_KHR);
            shaderGroups.get(2).intersectionShader(VK_SHADER_UNUSED_KHR);

            VkRayTracingPipelineCreateInfoKHR.Buffer pipelineInfo = VkRayTracingPipelineCreateInfoKHR.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_RAY_TRACING_PIPELINE_CREATE_INFO_KHR);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pGroups(shaderGroups);
            pipelineInfo.maxPipelineRayRecursionDepth(1);
            pipelineInfo.layout(pipelineLayout);

            LongBuffer pPipeline = stack.mallocLong(1);
            if (vkCreateRayTracingPipelinesKHR(DeviceManager.vkDevice, VK_NULL_HANDLE, VK_NULL_HANDLE, pipelineInfo, null, pPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create ray tracing pipeline");
            }
            pipelineHandle = pPipeline.get(0);

            createShaderBindingTable();
        }
    }

    private void createShaderBindingTable() {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceRayTracingPipelinePropertiesKHR rayTracingProperties = VkPhysicalDeviceRayTracingPipelinePropertiesKHR.calloc(stack);
            rayTracingProperties.sType$Default();

            VkPhysicalDeviceProperties2 deviceProps2 = VkPhysicalDeviceProperties2.calloc(stack);
            deviceProps2.sType$Default();
            deviceProps2.pNext(rayTracingProperties.address());
            vkGetPhysicalDeviceProperties2(DeviceManager.physicalDevice, deviceProps2);

            int groupCount = 3;
            int handleSize = rayTracingProperties.shaderGroupHandleSize();
            int handleAlignment = Math.max(1, rayTracingProperties.shaderGroupHandleAlignment());
            int baseAlignment = Math.max(1, rayTracingProperties.shaderGroupBaseAlignment());

            int alignedHandleSize = alignUp(handleSize, handleAlignment);
            this.sbtStride = alignUp(alignedHandleSize, baseAlignment);
            int sbtSize = this.sbtStride * groupCount;

            ByteBuffer handles = MemoryUtil.memAlloc(handleSize * groupCount);
            if (vkGetRayTracingShaderGroupHandlesKHR(DeviceManager.vkDevice, pipelineHandle, 0, groupCount, handles) != VK_SUCCESS) {
                MemoryUtil.memFree(handles);
                throw new RuntimeException("Failed to get ray tracing shader group handles");
            }

            ByteBuffer sbtData = MemoryUtil.memAlloc(sbtSize);
            MemoryUtil.memSet(MemoryUtil.memAddress(sbtData), (byte) 0, sbtSize);
            for (int i = 0; i < groupCount; i++) {
                long src = MemoryUtil.memAddress(handles) + (long) i * handleSize;
                long dst = MemoryUtil.memAddress(sbtData) + (long) i * this.sbtStride;
                MemoryUtil.memCopy(src, dst, handleSize);
            }
            MemoryUtil.memFree(handles);

            this.shaderBindingTable = new Buffer(VK_BUFFER_USAGE_SHADER_BINDING_TABLE_BIT_KHR | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, MemoryTypes.HOST_MEM);
            this.shaderBindingTable.createBuffer(sbtSize);
            this.shaderBindingTable.copyBuffer(sbtData, sbtSize);
            MemoryUtil.memFree(sbtData);

            VkBufferDeviceAddressInfo bufferDeviceAddressInfo = VkBufferDeviceAddressInfo.calloc(stack);
            bufferDeviceAddressInfo.sType$Default();
            bufferDeviceAddressInfo.buffer(this.shaderBindingTable.getId());
            this.sbtBufferAddress = vkGetBufferDeviceAddress(DeviceManager.vkDevice, bufferDeviceAddressInfo);
        }
    }

    public long getHandle() {
        return this.pipelineHandle;
    }

    public long getSbtBufferAddress() {
        return sbtBufferAddress;
    }

    public int getSbtStride() {
        return sbtStride;
    }

    @Override
    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.vkDevice, raygenShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, missShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, chitShaderModule, null);

        destroyDescriptorSets();
        vkDestroyPipeline(DeviceManager.vkDevice, pipelineHandle, null);
        vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DeviceManager.vkDevice, pipelineLayout, null);

        this.shaderBindingTable.scheduleFree();

        PIPELINES.remove(this);
        //Renderer.getInstance().removeUsedPipeline(this);
    }

    public static class Builder extends Pipeline.Builder {
        SPIRVUtils.SPIRV raygenShaderSPIRV, missShaderSPIRV, chitShaderSPIRV;

        public Builder(String shaderPath) {
            super(null, shaderPath);
        }

        public Builder setRaygenShaderSPIRV(SPIRVUtils.SPIRV spirv) {
            this.raygenShaderSPIRV = spirv;
            return this;
        }

        public Builder setMissShaderSPIRV(SPIRVUtils.SPIRV spirv) {
            this.missShaderSPIRV = spirv;
            return this;
        }

        public Builder setChitShaderSPIRV(SPIRVUtils.SPIRV spirv) {
            this.chitShaderSPIRV = spirv;
            return this;
        }

        public RayTracingPipeline createRayTracingPipeline() {
            return new RayTracingPipeline(this);
        }
    }
}
