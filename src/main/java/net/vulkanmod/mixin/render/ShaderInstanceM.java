package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(ShaderInstance.class)
public class ShaderInstanceM implements ShaderMixed {

    @Shadow @Final private Map<String, com.mojang.blaze3d.shaders.Uniform> uniformMap;
    @Shadow @Final private String name;

    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform MODEL_VIEW_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform PROJECTION_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform COLOR_MODULATOR;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform LINE_WIDTH;

    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform GLINT_ALPHA;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_START;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_END;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_COLOR;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_SHAPE;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform TEXTURE_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform GAME_TIME;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform SCREEN_SIZE;

    private String vsPath;
    private String fsName;

    private GraphicsPipeline pipeline;
    boolean isLegacy = false;


    public GraphicsPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(GraphicsPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void create(ResourceProvider resourceProvider, String name, VertexFormat format, CallbackInfo ci) {
        // Skip VulkanMod's pipeline creation for mod subclasses (e.g. Iris ExtendedShader)
        // These mods handle their own Vulkan pipeline creation
        if (((Object) this).getClass() != ShaderInstance.class) {
            return;
        }

        try {
            String builtInJsonPath = String.format("/assets/vulkanmod/shaders/minecraft/core/%s/%s.json", name, name);

            if (Pipeline.class.getResourceAsStream(builtInJsonPath) == null) {
                // No built-in VulkanMod shader — must use legacy (resource pack) path
                createLegacyShader(resourceProvider, format);
                return;
            }

            // Built-in exists — check if a resource pack overrides this shader
            if (hasResourcePackShaderOverride(resourceProvider)) {
                Initializer.LOGGER.info("Resource pack shader override detected for '{}', using legacy conversion path", name);
                createLegacyShader(resourceProvider, format);
                return;
            }

            // Use VulkanMod's built-in optimized shader
            String path = String.format("minecraft/core/%s/%s", name, name);
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            this.pipeline = pipelineBuilder.createGraphicsPipeline();
        } catch (Exception e) {
            System.out.printf("Error on shader %s creation\n", name);
            e.printStackTrace();
            throw e;
        }
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;"), require = 0)
    private Program loadNames(ResourceProvider resourceProvider, Program.Type type, String name) {
        String path;
        if (this.name.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            ResourceLocation location = ResourceLocation.tryParse(name);
            path = location.withPath("shaders/core/%s".formatted(location.getPath())).toString();
        } else {
            path = "shaders/core/%s".formatted(name);
        }

        switch (type) {
            case VERTEX -> this.vsPath = path;
            case FRAGMENT -> this.fsName = path;
        }

        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"), require = 0)
    private void bindAttr(int program, int index, CharSequence name) {}

    /**
     * @author
     */
    @Overwrite
    public void close() {
        if (this.pipeline != null)
            this.pipeline.cleanUp();
    }

    /**
     * @author
     */
    @Overwrite
    public void apply() {
        if (!this.isLegacy)
            return;

        if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }

        if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        if (this.FOG_START != null) {
            this.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (this.FOG_END != null) {
            this.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (this.GAME_TIME != null) {
            this.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (this.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            this.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }

        if (this.LINE_WIDTH != null) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void clear() {}

    private void setUniformSuppliers(UBO ubo) {

        for (Uniform vUniform : ubo.getUniforms()) {
            com.mojang.blaze3d.shaders.Uniform uniform = this.uniformMap.get(vUniform.getName());

            if (uniform == null) {
                Initializer.LOGGER.error(String.format("Error: field %s not present in uniform map", vUniform.getName()));
                continue;
            }

            Supplier<MappedBuffer> supplier;
            ByteBuffer byteBuffer;

            if (uniform.getType() <= 3) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
            } else if (uniform.getType() <= 10) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
            } else {
                throw new RuntimeException("out of bounds value for uniform " + uniform);
            }


            MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
            supplier = () -> mappedBuffer;

            vUniform.setSupplier(supplier);
        }

    }

    /**
     * Detects whether a resource pack overrides the shaders for the current shader program.
     * Uses two methods:
     * 1. ResourceManager.getResourceStack() to check if multiple packs provide the same shader
     * 2. Content analysis for custom #moj_import directives or uniforms not in VulkanMod's built-in
     */
    private boolean hasResourcePackShaderOverride(ResourceProvider resourceProvider) {
        try {
            ResourceLocation vertLoc = ResourceLocation.tryParse(this.vsPath + ".vsh");
            ResourceLocation fragLoc = ResourceLocation.tryParse(this.fsName + ".fsh");

            // Method 1: Check if multiple resource packs provide this shader
            if (resourceProvider instanceof ResourceManager rm) {
                List<Resource> vertStack = rm.getResourceStack(vertLoc);
                if (vertStack.size() > 1) return true;

                List<Resource> fragStack = rm.getResourceStack(fragLoc);
                if (fragStack.size() > 1) return true;
            }

            // Method 2: Content analysis — check for features not in vanilla shaders
            String vshSrc = loadShaderSource(resourceProvider, vertLoc);
            String fshSrc = loadShaderSource(resourceProvider, fragLoc);

            // Check for custom #moj_import (files not in VulkanMod's include path)
            if (hasNonStandardImports(vshSrc) || hasNonStandardImports(fshSrc)) return true;

            // Check for precision qualifiers (resource pack shaders targeting GLES/WebGL)
            if (vshSrc.contains("precision highp") || vshSrc.contains("precision mediump")) return true;

            // Check if shader adds GameTime but VulkanMod's built-in doesn't have it
            if (vshSrc.contains("uniform float GameTime") || fshSrc.contains("uniform float GameTime")) {
                String builtInJsonPath = String.format("/assets/vulkanmod/shaders/minecraft/core/%s/%s.json", this.name, this.name);
                try (InputStream jsonStream = Pipeline.class.getResourceAsStream(builtInJsonPath)) {
                    if (jsonStream != null) {
                        String jsonContent = IOUtils.toString(jsonStream, StandardCharsets.UTF_8);
                        if (!jsonContent.contains("GameTime")) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String loadShaderSource(ResourceProvider resourceProvider, ResourceLocation loc) throws Exception {
        Resource resource = resourceProvider.getResourceOrThrow(loc);
        try (InputStream is = resource.open()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private boolean hasNonStandardImports(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("#moj_import")) {
                String filename = line.substring("#moj_import".length()).trim()
                    .replace("<", "").replace(">", "").replace("\"", "").trim();
                // If this include doesn't exist in VulkanMod's built-in include path, it's custom
                if (Pipeline.class.getResourceAsStream("/assets/vulkanmod/shaders/include/" + filename) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolves custom #moj_import directives by inlining file contents from the resource provider.
     * Standard imports (files that exist in VulkanMod's include path) are left as-is for
     * GlslConverter to convert to #include directives and resolve via ShaderIncluder.
     */
    private String resolveCustomImports(String source, ResourceProvider resourceProvider) {
        StringBuilder result = new StringBuilder();
        for (String line : source.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#moj_import")) {
                String filename = trimmed.substring("#moj_import".length()).trim()
                    .replace("<", "").replace(">", "").replace("\"", "").trim();

                // Only inline if NOT in VulkanMod's built-in include path
                if (Pipeline.class.getResourceAsStream("/assets/vulkanmod/shaders/include/" + filename) == null) {
                    try {
                        ResourceLocation includeLoc = ResourceLocation.tryParse("minecraft:shaders/include/" + filename);
                        Resource includeRes = resourceProvider.getResourceOrThrow(includeLoc);
                        String includeContent;
                        try (InputStream is = includeRes.open()) {
                            includeContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                        }
                        // Recursively resolve custom imports in included content
                        includeContent = resolveCustomImports(includeContent, resourceProvider);
                        result.append(includeContent).append("\n");
                        continue;
                    } catch (Exception e) {
                        Initializer.LOGGER.warn("Failed to resolve custom shader import '{}', leaving as #moj_import", filename);
                    }
                }
            }
            result.append(line).append("\n");
        }
        return result.toString();
    }

    private void createLegacyShader(ResourceProvider resourceProvider, VertexFormat format) {
        try {
            ResourceLocation vertLoc = ResourceLocation.tryParse(this.vsPath + ".vsh");
            String vshSrc = loadShaderSource(resourceProvider, vertLoc);

            ResourceLocation fragLoc = ResourceLocation.tryParse(this.fsName + ".fsh");
            String fshSrc = loadShaderSource(resourceProvider, fragLoc);

            // Resolve custom #moj_import directives by inlining from resource packs.
            // Standard vanilla imports (fog.glsl, light.glsl, etc.) are left for
            // GlslConverter → ShaderIncluder to handle using VulkanMod's Vulkan 450 versions.
            vshSrc = resolveCustomImports(vshSrc, resourceProvider);
            fshSrc = resolveCustomImports(fshSrc, resourceProvider);

            GlslConverter converter = new GlslConverter();
            Pipeline.Builder builder = new Pipeline.Builder(format, this.name);

            converter.process(vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(this.name, converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createGraphicsPipeline();
            this.isLegacy = true;

        } catch (Exception e) {
            Initializer.LOGGER.error("Error on shader {} resource pack conversion/compilation, falling back to built-in", this.name);
            e.printStackTrace();

            // Fall back to VulkanMod's built-in shader so text/GUI still renders
            try {
                String builtInPath = String.format("minecraft/core/%s/%s", this.name, this.name);
                if (Pipeline.class.getResourceAsStream(
                        String.format("/assets/vulkanmod/shaders/%s.json", builtInPath)) != null) {
                    Pipeline.Builder fallbackBuilder = new Pipeline.Builder(format, builtInPath);
                    fallbackBuilder.parseBindingsJSON();
                    fallbackBuilder.compileShaders();
                    this.pipeline = fallbackBuilder.createGraphicsPipeline();
                    this.isLegacy = false;
                    Initializer.LOGGER.info("Fell back to built-in shader for '{}'", this.name);
                }
            } catch (Exception fallbackEx) {
                Initializer.LOGGER.error("Built-in fallback also failed for '{}'", this.name);
                fallbackEx.printStackTrace();
            }
        }
    }
}

