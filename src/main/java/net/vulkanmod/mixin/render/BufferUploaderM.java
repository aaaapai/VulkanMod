package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(BufferUploader.class)
public class BufferUploaderM {

    /**
     * @author
     */
    @Overwrite
    public static void reset() {}

    /**
     * @author
     */
    @Overwrite
    public static void drawWithShader(MeshData meshData) {
        RenderSystem.assertOnRenderThread();

        MeshData.DrawState parameters = meshData.drawState();

        Renderer renderer = Renderer.getInstance();

        if (parameters.vertexCount() > 0) {
            ShaderInstance shaderInstance = RenderSystem.getShader();

            boolean isIrisShader = shaderInstance.getClass().getName().contains("ExtendedShader");

            // Prevent drawing if formats don't match to avoid visual bugs
            if (shaderInstance.getVertexFormat() != parameters.format()) {
                meshData.close();
                return;
            }

            // Used to update legacy shader uniforms
            shaderInstance.apply();

            GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();

            if (pipeline == null) {
                meshData.close();
                return;
            }

            // Ensure a render pass is active before drawing
            // (Iris's ExtendedShader.apply() starts a render pass via GlFramebuffer.bind() -
            //  if that failed, we must not attempt to draw)
            if (renderer.getBoundRenderPass() == null) {
                meshData.close();
                return;
            }

            VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);

            renderer.bindGraphicsPipeline(pipeline);

            // For Iris ExtendedShader: skip VTextureSelector.bindShaderTextures() because
            // ExtendedShader.apply() already handles all texture binding via ProgramSamplers.
            // VTextureSelector reads from RenderSystem.getShaderTexture() which doesn't have
            // Iris's custom samplers (shadow maps, gbuffer textures, PBR textures), so calling
            // it would overwrite correct bindings with wrong ones for indices 0-11.
            if (!isIrisShader) {
                VTextureSelector.bindShaderTextures(pipeline);
            }

            renderer.uploadAndBindUBOs(pipeline);

            // For Iris shaders with LINES mode: use non-indexed drawing.
            // VulkanMod's Drawer groups every 4 vertices into quads for LINES mode,
            // creating cross-connections between adjacent line segments via the
            // linesIndexBuffer. With LINE_LIST topology, these cross-lines become
            // screen-spanning artifacts. DEBUG_LINES mode draws each sequential
            // vertex pair as an independent line.
            VertexFormat.Mode drawMode = parameters.mode();
            if (isIrisShader && drawMode == VertexFormat.Mode.LINES) {
                drawMode = VertexFormat.Mode.DEBUG_LINES;
            }
            Renderer.getDrawer().draw(meshData.vertexBuffer(), drawMode, parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    /**
     * @author
     */
    @Overwrite
    public static void draw(MeshData meshData) {
        MeshData.DrawState parameters = meshData.drawState();

        if (parameters.vertexCount() > 0) {
            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();
            renderer.uploadAndBindUBOs(pipeline);

            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

}
