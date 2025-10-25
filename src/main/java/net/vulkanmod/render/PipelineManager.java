package net.vulkanmod.render;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.RayTracingPipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

public abstract class PipelineManager {
    public static VertexFormat terrainVertexFormat;

    public static void setTerrainVertexFormat(VertexFormat format) {
        terrainVertexFormat = format;
    }

    static GraphicsPipeline
            terrainShader, terrainShaderEarlyZ,
            fastBlitPipeline, cloudsPipeline;

    static RayTracingPipeline rayTracingPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    private static final Map<ResourceLocation, GraphicsPipeline> dynamicPipelines = new HashMap<>();

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(
                renderType -> renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : terrainShader);
    }

    private static void createBasicPipelines() {
        terrainShaderEarlyZ = createPipeline("terrain_earlyZ", terrainVertexFormat);
        terrainShader = createPipeline("terrain", terrainVertexFormat);
        fastBlitPipeline = createPipeline("blit", CustomVertexFormat.NONE);
        cloudsPipeline = createPipeline("clouds", DefaultVertexFormat.POSITION_COLOR);
        rayTracingPipeline = createRayTracingPipeline("raytracing");
    }

    private static GraphicsPipeline createPipeline(String configName, VertexFormat vertexFormat) {
        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, configName);

        JsonObject config = ShaderLoadUtil.getJsonConfig("basic", configName);
        if (config == null) {
            return null;
        }

        pipelineBuilder.parseBindings(config);

        ShaderLoadUtil.loadShaders(pipelineBuilder, config, configName, "basic");

        return pipelineBuilder.createGraphicsPipeline();
    }

    private static RayTracingPipeline createRayTracingPipeline(String configName) {
        RayTracingPipeline.Builder pipelineBuilder = new RayTracingPipeline.Builder(configName);

        JsonObject config = ShaderLoadUtil.getJsonConfig("raytracing", configName);
        pipelineBuilder.parseBindings(config);

        ShaderLoadUtil.loadShader(pipelineBuilder, configName, "raytracing", SPIRVUtils.ShaderKind.RAYGEN_SHADER);
        ShaderLoadUtil.loadShader(pipelineBuilder, configName, "raytracing", SPIRVUtils.ShaderKind.MISS_SHADER);
        ShaderLoadUtil.loadShader(pipelineBuilder, configName, "raytracing", SPIRVUtils.ShaderKind.CLOSEST_HIT_SHADER);

        return pipelineBuilder.createRayTracingPipeline();
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType) {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainShaderEarlyZ;
    }

    public static GraphicsPipeline getFastBlitPipeline() {
        return fastBlitPipeline;
    }

    public static GraphicsPipeline getCloudsPipeline() {
        return cloudsPipeline;
    }

    public static RayTracingPipeline getRayTracingPipeline() {
        return rayTracingPipeline;
    }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
        fastBlitPipeline.cleanUp();
        cloudsPipeline.cleanUp();
        rayTracingPipeline.cleanUp();
        dynamicPipelines.values().forEach(GraphicsPipeline::cleanUp);
        dynamicPipelines.clear();
    }

    public static GraphicsPipeline getPipeline(RenderPipeline pipeline, VertexFormat fallbackFormat) {
        ResourceLocation location = pipeline.getLocation();
        GraphicsPipeline cached = dynamicPipelines.get(location);
        if (cached != null) {
            return cached;
        }

        String configName = location.getPath();
        VertexFormat vertexFormat = pipeline.getVertexFormat();
        if (vertexFormat == null) {
            vertexFormat = fallbackFormat != null ? fallbackFormat : terrainVertexFormat;
        }

        GraphicsPipeline graphicsPipeline = createPipeline(configName, vertexFormat);
        if (graphicsPipeline == null) {
            return terrainShader;
        }

        dynamicPipelines.put(location, graphicsPipeline);
        return graphicsPipeline;
    }
}
