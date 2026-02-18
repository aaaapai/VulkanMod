package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {
    private final Object2LongMap<PipelineState> graphicsPipelines = new Object2LongOpenHashMap<>();

    private final VertexFormat vertexFormat;
    private final VertexInputDescription vertexInputDescription;

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    GraphicsPipeline(Builder builder) {
        super(builder.shaderPath);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.imageDescriptors = builder.imageDescriptors;
        this.pushConstants = builder.pushConstants;
        this.vertexFormat = builder.vertexFormat;

        this.vertexInputDescription = new VertexInputDescription(this.vertexFormat);

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(builder.vertShaderSPIRV, builder.fragShaderSPIRV);

        if (builder.renderPass != null)
            graphicsPipelines.computeIfAbsent(PipelineState.DEFAULT,
                    this::createGraphicsPipeline);

        createDescriptorSets(Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    public long getHandle(PipelineState state) {
        long handle = graphicsPipelines.computeIfAbsent(state, this::createGraphicsPipeline);
        if (handle == VK_NULL_HANDLE) {
            // Remove the cached null handle so creation can be retried
            graphicsPipelines.removeLong(state);
        }
        return handle;
    }

    private long createGraphicsPipeline(PipelineState state) {
        // Validate handles before calling vkCreateGraphicsPipelines to prevent native driver crashes
        if (vertShaderModule == 0 || fragShaderModule == 0) {
            System.err.println("[VulkanMod] FATAL: Cannot create graphics pipeline '" + name + "' - shader modules are invalid"
                + " (vert=" + vertShaderModule + " frag=" + fragShaderModule + ")");
            return VK_NULL_HANDLE;
        }
        if (pipelineLayout == 0) {
            System.err.println("[VulkanMod] FATAL: Cannot create graphics pipeline '" + name + "' - pipeline layout is VK_NULL_HANDLE");
            return VK_NULL_HANDLE;
        }
        if (state.renderPass == null || state.renderPass.getId() == 0) {
            System.err.println("[VulkanMod] FATAL: Cannot create graphics pipeline '" + name + "' - render pass is null/invalid");
            return VK_NULL_HANDLE;
        }

        try (MemoryStack stack = stackPush()) {
            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(vertexInputDescription.bindingDescriptions);
            vertexInputInfo.pVertexAttributeDescriptions(vertexInputDescription.attributeDescriptions);

            // ===> ASSEMBLY STAGE <===

            final int topology = PipelineState.AssemblyRasterState.decodeTopology(state.assemblyRasterState);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // ===> RASTERIZATION STAGE <===

            final int polygonMode = PipelineState.AssemblyRasterState.decodePolygonMode(state.assemblyRasterState);
            final int cullMode = PipelineState.AssemblyRasterState.decodeCullMode(state.assemblyRasterState);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(polygonMode);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(cullMode);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(true);

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // ===> DEPTH TEST <===

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(PipelineState.DepthState.depthTest(state.depthState_i));
            depthStencil.depthWriteEnable(PipelineState.DepthState.depthMask(state.depthState_i));
            depthStencil.depthCompareOp(PipelineState.DepthState.decodeDepthFun(state.depthState_i));
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional
            depthStencil.stencilTestEnable(false);

            // ===> COLOR BLENDING <===

            // MRT: create one blend attachment state per color attachment in the render pass
            int colorAttCount = (state.renderPass != null) ? state.renderPass.getColorAttachmentCount() : 1;
            if (colorAttCount < 1) colorAttCount = 1;

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(colorAttCount, stack);
            for (int att = 0; att < colorAttCount; att++) {
                VkPipelineColorBlendAttachmentState blendAtt = colorBlendAttachment.get(att);
                blendAtt.colorWriteMask(state.colorMask_i);

                if (PipelineState.BlendState.enable(state.blendState_i)) {
                    blendAtt.blendEnable(true);
                    blendAtt.srcColorBlendFactor(PipelineState.BlendState.getSrcRgbFactor(state.blendState_i));
                    blendAtt.dstColorBlendFactor(PipelineState.BlendState.getDstRgbFactor(state.blendState_i));
                    blendAtt.colorBlendOp(VK_BLEND_OP_ADD);
                    blendAtt.srcAlphaBlendFactor(PipelineState.BlendState.getSrcAlphaFactor(state.blendState_i));
                    blendAtt.dstAlphaBlendFactor(PipelineState.BlendState.getDstAlphaFactor(state.blendState_i));
                    blendAtt.alphaBlendOp(VK_BLEND_OP_ADD);
                } else {
                    blendAtt.blendEnable(false);
                }
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(PipelineState.LogicOpState.enable(state.logicOp_i));
            colorBlending.logicOp(PipelineState.LogicOpState.decodeFun(state.logicOp_i));
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> DYNAMIC STATES <===

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            if (topology == VK_PRIMITIVE_TOPOLOGY_LINE_LIST || polygonMode == VK_POLYGON_MODE_LINE)
                dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_LINE_WIDTH));
            else
                dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicStates);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            if (!Vulkan.DYNAMIC_RENDERING) {
                pipelineInfo.renderPass(state.renderPass.getId());
                pipelineInfo.subpass(0);
            }
            else {
                //dyn-rendering
                VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(state.renderPass.getFramebuffer().getFormat()));
                renderingInfo.depthAttachmentFormat(state.renderPass.getFramebuffer().getDepthFormat());
                pipelineInfo.pNext(renderingInfo);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(DeviceManager.vkDevice, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }

    private void createShaderModules(SPIRVUtils.SPIRV vertSpirv, SPIRVUtils.SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.vkDevice, vertShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, fragShaderModule, null);

        vertexInputDescription.cleanUp();

        destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DeviceManager.vkDevice, pipelineLayout, null);

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }

    static class VertexInputDescription {
        final VkVertexInputAttributeDescription.Buffer attributeDescriptions;
        final VkVertexInputBindingDescription.Buffer bindingDescriptions;

        VertexInputDescription(VertexFormat vertexFormat) {
            this.bindingDescriptions = getBindingDescription(vertexFormat);
            this.attributeDescriptions = getAttributeDescriptions(vertexFormat);
        }

        void cleanUp() {
            MemoryUtil.memFree(this.bindingDescriptions);
            MemoryUtil.memFree(this.attributeDescriptions);
        }
    }

    private static VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat) {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1);

        bindingDescription.binding(0);
        bindingDescription.stride(vertexFormat.getVertexSize());
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(VertexFormat vertexFormat) {
        List<VertexFormatElement> elements = vertexFormat.getElements();

        int size = elements.size();

        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(size);

        int offset = 0;

        for (int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.usage();
            VertexFormatElement.Type type = formatElement.type();
            int elementCount = formatElement.count();

            switch (usage) {
                case POSITION -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 12;
                        }
                        case SHORT -> {
                            posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case BYTE -> {
                            posDescription.format(VK_FORMAT_R8G8B8A8_SINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                    }

                }

                case COLOR -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case UV -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case SHORT -> {
                            posDescription.offset(offset);
                            if (elementCount <= 2) {
                                posDescription.format(VK_FORMAT_R16G16_SINT);
                                offset += 4;
                            } else {
                                posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                                offset += elementCount * 2;
                            }
                        }
                        case USHORT -> {
                            posDescription.offset(offset);
                            if (elementCount <= 2) {
                                posDescription.format(VK_FORMAT_R16G16_UINT);
                                offset += 4;
                            } else {
                                // Use SINT to match shader's ivec3 (e.g. iris_Entity)
                                posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                                offset += elementCount * 2;
                            }
                        }
                    }
                }

                case NORMAL -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case GENERIC -> {
                    int fmt;
                    int sz;
                    if (type == VertexFormatElement.Type.FLOAT) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R32_SFLOAT; sz = 4; }
                            case 2 -> { fmt = VK_FORMAT_R32G32_SFLOAT; sz = 8; }
                            case 3 -> { fmt = VK_FORMAT_R32G32B32_SFLOAT; sz = 12; }
                            case 4 -> { fmt = VK_FORMAT_R32G32B32A32_SFLOAT; sz = 16; }
                            default -> throw new RuntimeException("Unsupported GENERIC FLOAT count: " + elementCount);
                        }
                    } else if (type == VertexFormatElement.Type.SHORT) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R16_SINT; sz = 2; }
                            case 2 -> { fmt = VK_FORMAT_R16G16_SINT; sz = 4; }
                            case 3 -> { fmt = VK_FORMAT_R16G16B16A16_SINT; sz = 8; }
                            case 4 -> { fmt = VK_FORMAT_R16G16B16A16_SINT; sz = 8; }
                            default -> throw new RuntimeException("Unsupported GENERIC SHORT count: " + elementCount);
                        }
                    } else if (type == VertexFormatElement.Type.BYTE) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R8_SNORM; sz = 1; }
                            case 2 -> { fmt = VK_FORMAT_R8G8_SNORM; sz = 2; }
                            case 3 -> { fmt = VK_FORMAT_R8G8B8A8_SNORM; sz = 4; }
                            case 4 -> { fmt = VK_FORMAT_R8G8B8A8_SNORM; sz = 4; }
                            default -> throw new RuntimeException("Unsupported GENERIC BYTE count: " + elementCount);
                        }
                    } else if (type == VertexFormatElement.Type.INT) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R32_SINT; sz = 4; }
                            case 2 -> { fmt = VK_FORMAT_R32G32_SINT; sz = 8; }
                            case 3 -> { fmt = VK_FORMAT_R32G32B32_SINT; sz = 12; }
                            case 4 -> { fmt = VK_FORMAT_R32G32B32A32_SINT; sz = 16; }
                            default -> throw new RuntimeException("Unsupported GENERIC INT count: " + elementCount);
                        }
                    } else if (type == VertexFormatElement.Type.USHORT) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R16_UINT; sz = 2; }
                            case 2 -> { fmt = VK_FORMAT_R16G16_UINT; sz = 4; }
                            case 3 -> { fmt = VK_FORMAT_R16G16B16A16_UINT; sz = 8; }
                            case 4 -> { fmt = VK_FORMAT_R16G16B16A16_UINT; sz = 8; }
                            default -> throw new RuntimeException("Unsupported GENERIC USHORT count: " + elementCount);
                        }
                    } else if (type == VertexFormatElement.Type.UBYTE) {
                        switch (elementCount) {
                            case 1 -> { fmt = VK_FORMAT_R8_UINT; sz = 1; }
                            case 2 -> { fmt = VK_FORMAT_R8G8_UINT; sz = 2; }
                            case 3 -> { fmt = VK_FORMAT_R8G8B8A8_UINT; sz = 4; }
                            case 4 -> { fmt = VK_FORMAT_R8G8B8A8_UINT; sz = 4; }
                            default -> throw new RuntimeException("Unsupported GENERIC UBYTE count: " + elementCount);
                        }
                    } else {
                        throw new RuntimeException(String.format("Unknown GENERIC type: %s count: %d", type, elementCount));
                    }
                    posDescription.format(fmt);
                    posDescription.offset(offset);
                    offset += sz;
                }

                default -> throw new RuntimeException(String.format("Unknown format: %s", usage));
            }

            posDescription.offset(((VertexFormatMixed) (vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }
}
