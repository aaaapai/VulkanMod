package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.MemoryType;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.VertexBuffer;
import net.vulkanmod.vulkan.memory.buffer.index.AutoIndexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Environment(EnvType.CLIENT)
public class VBO {
    private final MemoryType memoryType;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;

    private VertexFormat.Mode mode;
    private boolean autoIndexed = false;
    private int indexCount;
    private int vertexCount;

    public VBO() {
        this(MemoryTypes.GPU_MEM);
    }

    public VBO(MemoryType memoryType) {
        this.memoryType = memoryType;
    }

    private static Matrix4f snapshotMatrix(ByteBuffer buffer) {
        FloatBuffer floatBuffer = buffer.asFloatBuffer().duplicate();
        floatBuffer.rewind();
        return new Matrix4f(floatBuffer);
    }

    private static int toGlMode(VertexFormat.Mode mode) {
        return switch (mode) {
            case LINES, DEBUG_LINES -> GL11.GL_LINES;
            case LINE_STRIP, DEBUG_LINE_STRIP -> GL11.GL_LINE_STRIP;
            case TRIANGLES -> GL11.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL11.GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> GL11.GL_TRIANGLE_FAN;
            case QUADS -> GL11.GL_QUADS;
        };
    }

    public void upload(MeshData meshData) {
        MeshData.DrawState parameters = meshData.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.mode = parameters.mode();

        this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
        this.uploadIndexBuffer(meshData.indexBuffer());

        meshData.close();
    }

    private void uploadVertexBuffer(MeshData.DrawState parameters, ByteBuffer data) {
        if (data != null) {
            if (this.vertexBuffer != null)
                this.vertexBuffer.scheduleFree();

            int size = parameters.format().getVertexSize() * parameters.vertexCount();
            this.vertexBuffer = new VertexBuffer(size, this.memoryType);
            this.vertexBuffer.copyBuffer(data, size);
        }
    }

    public void uploadIndexBuffer(ByteBuffer data) {
        if (data == null) {

            AutoIndexBuffer autoIndexBuffer;
            switch (this.mode) {
                case TRIANGLE_FAN -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case TRIANGLE_STRIP, LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case QUADS -> {
                    autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
                }
                case LINES -> {
                    autoIndexBuffer = Renderer.getDrawer().getLinesIndexBuffer();
                }
                case DEBUG_LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getDebugLineStripIndexBuffer();
                }
                case TRIANGLES, DEBUG_LINES -> {
                    autoIndexBuffer = null;
                }
                default -> throw new IllegalStateException("Unexpected draw mode: %s".formatted(this.mode));
            }

            if (this.indexBuffer != null && !this.autoIndexed) {
                this.indexBuffer.scheduleFree();
            }

            if (autoIndexBuffer != null) {
                autoIndexBuffer.checkCapacity(this.vertexCount);
                this.indexBuffer = autoIndexBuffer.getIndexBuffer();
            }

            this.autoIndexed = true;
        } else {
            if (this.indexBuffer != null && !this.autoIndexed) {
                this.indexBuffer.scheduleFree();
            }

            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            this.indexBuffer.copyBuffer(data, data.remaining());
        }
    }

    public void drawWithShader(Matrix4f modelView, Matrix4f projection, GraphicsPipeline pipeline) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            Matrix4f previousModelView = snapshotMatrix(VRenderSystem.getModelViewMatrix().buffer);
            Matrix4f previousProjection = snapshotMatrix(VRenderSystem.getProjectionMatrix().buffer);

            VRenderSystem.applyMVP(modelView, projection);
            VRenderSystem.setPrimitiveTopologyGL(toGlMode(this.mode));

            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);

            if (this.indexBuffer != null) {
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
            } else {
                Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
            }

            VRenderSystem.applyMVP(previousModelView, previousProjection);
        }
    }

    public void draw() {
        if (this.indexCount != 0) {
            if (this.indexBuffer != null) {
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
            } else {
                Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
            }
        }
    }

    public void close() {
        if (this.vertexCount <= 0)
            return;

        this.vertexBuffer.scheduleFree();
        this.vertexBuffer = null;

        if (!this.autoIndexed) {
            this.indexBuffer.scheduleFree();
            this.indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
    }

}
