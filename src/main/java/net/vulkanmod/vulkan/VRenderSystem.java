package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.vulkan.VK10.*;

public abstract class VRenderSystem {
    private static final float DEFAULT_DEPTH_VALUE = 1.0f;
    private static final float[] shaderColorArray = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] shaderFogColorArray = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;
    public static int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    public static int polygonMode = VK_POLYGON_MODE_FILL;
    public static boolean canSetLineWidth = false;
    public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
    public static boolean cull = true;
    public static boolean logicOp = false;
    public static int logicOpFun = 0;
    public static float clearDepthValue = DEFAULT_DEPTH_VALUE;
    public static FloatBuffer clearColor = MemoryUtil.memCallocFloat(4);
    public static MappedBuffer modelViewMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer projectionMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer TextureMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer MVP = new MappedBuffer(16 * 4);
    public static MappedBuffer modelOffset = new MappedBuffer(3 * 4);
    public static MappedBuffer lightDirection0 = new MappedBuffer(3 * 4);
    public static MappedBuffer lightDirection1 = new MappedBuffer(3 * 4);
    public static MappedBuffer shaderColor = new MappedBuffer(4 * 4);
    public static MappedBuffer shaderFogColor = new MappedBuffer(4 * 4);
    public static MappedBuffer screenSize = new MappedBuffer(2 * 4);
    public static float alphaCutout = 0.0f;
    private static long window;
    private static float fogEnvironmentalStart;
    private static float fogRenderStart;
    private static float fogEnvironmentalEnd;
    private static float fogRenderEnd;
    private static float fogSkyEnd;
    private static float fogCloudEnd;
    private static int fogShapeIndex;
    private static float shaderGameTime;
    private static float glintAlpha = 1.0f;
    private static boolean depthBiasEnabled = false;
    private static float depthBiasConstant = 0.0f;
    private static float depthBiasSlope = 0.0f;

    public static void initRenderer() {
        Vulkan.initVulkan(window);
    }

    public static MappedBuffer getScreenSize() {
        updateScreenSize();
        return screenSize;
    }

    public static void updateScreenSize() {
        Window window = Minecraft.getInstance().getWindow();

        screenSize.putFloat(0, (float) window.getWidth());
        screenSize.putFloat(4, (float) window.getHeight());
    }

    public static void setWindow(long window) {
        VRenderSystem.window = window;
    }

    public static ByteBuffer getModelOffset() {
        return modelOffset.buffer;
    }

    public static int maxSupportedTextureSize() {
        return DeviceManager.deviceProperties.limits().maxImageDimension2D();
    }

    public static void applyMVP(Matrix4f MV, Matrix4f P) {
        applyModelViewMatrix(MV);
        applyProjectionMatrix(P);
        calculateMVP();
    }

    public static void applyModelViewMatrix(Matrix4f mat) {
        mat.get(modelViewMatrix.buffer.asFloatBuffer());
    }

    public static void applyProjectionMatrix(Matrix4f mat) {
        mat.get(projectionMatrix.buffer.asFloatBuffer());
    }

    public static void calculateMVP() {
        org.joml.Matrix4f MV = new org.joml.Matrix4f(modelViewMatrix.buffer.asFloatBuffer());
        org.joml.Matrix4f P = new org.joml.Matrix4f(projectionMatrix.buffer.asFloatBuffer());

        P.mul(MV).get(MVP.buffer);
    }

    public static MappedBuffer getTextureMatrix() {
        return TextureMatrix;
    }

    public static void setTextureMatrix(Matrix4f mat) {
        mat.get(TextureMatrix.buffer.asFloatBuffer());
    }

    public static MappedBuffer getModelViewMatrix() {
        return modelViewMatrix;
    }

    public static MappedBuffer getProjectionMatrix() {
        return projectionMatrix;
    }

    public static MappedBuffer getMVP() {
        return MVP;
    }

    public static void setModelOffset(float x, float y, float z) {
        long ptr = modelOffset.ptr;
        VUtil.UNSAFE.putFloat(ptr, x);
        VUtil.UNSAFE.putFloat(ptr + 4, y);
        VUtil.UNSAFE.putFloat(ptr + 8, z);
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderColor, f1, f2, f3, f4);
        shaderColorArray[0] = f1;
        shaderColorArray[1] = f2;
        shaderColorArray[2] = f3;
        shaderColorArray[3] = f4;
    }

    public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderFogColor, f1, f2, f3, f4);
        shaderFogColorArray[0] = f1;
        shaderFogColorArray[1] = f2;
        shaderFogColorArray[2] = f3;
        shaderFogColorArray[3] = f4;
    }

    public static void setShaderLights(float lx0, float ly0, float lz0, float lx1, float ly1, float lz1) {
        lightDirection0.buffer.putFloat(0, lx0);
        lightDirection0.buffer.putFloat(4, ly0);
        lightDirection0.buffer.putFloat(8, lz0);

        lightDirection1.buffer.putFloat(0, lx1);
        lightDirection1.buffer.putFloat(4, ly1);
        lightDirection1.buffer.putFloat(8, lz1);
    }

    public static void setFogParameters(float environmentalStart, float renderStart, float environmentalEnd, float renderEnd, float skyEnd, float cloudEnd) {
        fogEnvironmentalStart = environmentalStart;
        fogRenderStart = renderStart;
        fogEnvironmentalEnd = environmentalEnd;
        fogRenderEnd = renderEnd;
        fogSkyEnd = skyEnd;
        fogCloudEnd = cloudEnd;
    }

    public static float[] getShaderColorArray() {
        return shaderColorArray;
    }

    public static float[] getShaderFogColorArray() {
        return shaderFogColorArray;
    }

    public static float getFogStart() {
        return fogRenderStart;
    }

    public static float getFogEnd() {
        return fogRenderEnd;
    }

    public static int getFogShapeIndex() {
        return fogShapeIndex;
    }

    public static void setFogShapeIndex(int shapeIndex) {
        fogShapeIndex = shapeIndex;
    }

    public static float getShaderGameTime() {
        return shaderGameTime;
    }

    public static void setShaderGameTime(float gameTime) {
        shaderGameTime = gameTime;
    }

    public static float getGlintAlpha() {
        return glintAlpha;
    }

    public static void setGlintAlpha(float alpha) {
        glintAlpha = alpha;
    }

    public static MappedBuffer getShaderColor() {
        return shaderColor;
    }

    public static MappedBuffer getShaderFogColor() {
        return shaderFogColor;
    }

    public static void setClearColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(clearColor, f1, f2, f3, f4);
    }

    public static void clear(int mask) {
        Renderer.clearAttachments(mask);
    }

    public static void clearDepth(double depth) {
        clearDepthValue = (float) depth;
    }

    // Pipeline state

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static void setPrimitiveTopologyGL(final int mode) {
        VRenderSystem.topology = switch (mode) {
            case GL11.GL_LINES, GL11.GL_LINE_STRIP -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case GL11.GL_TRIANGLE_FAN, GL11.GL_TRIANGLES, GL11.GL_TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            default -> throw new RuntimeException(String.format("Unknown GL primitive topology: %s", mode));
        };
    }

    public static void setPolygonModeGL(final int mode) {
        VRenderSystem.polygonMode = switch (mode) {
            case GL11.GL_POINT -> VK_POLYGON_MODE_POINT;
            case GL11.GL_LINE -> VK_POLYGON_MODE_LINE;
            case GL11.GL_FILL -> VK_POLYGON_MODE_FILL;
            default -> throw new RuntimeException(String.format("Unknown GL polygon mode: %s", mode));
        };
    }

    public static void setLineWidth(final float width) {
        if (canSetLineWidth) {
            Renderer.setLineWidth(width);
        }
    }

    public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
        colorMask = PipelineState.ColorMask.getColorMask(b, b1, b2, b3);
    }

    public static int getColorMask() {
        return colorMask;
    }

    public static void enableDepthTest() {
        depthTest = true;
    }

    public static void enableCull() {
        cull = true;
    }

    public static void disableCull() {
        cull = false;
    }

    public static void depthFunc(int depthFun) {
        VRenderSystem.depthFun = depthFun;
    }

    public static void enableBlend() {
        PipelineState.blendInfo.enabled = true;
    }

    public static void disableBlend() {
        PipelineState.blendInfo.enabled = false;
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        PipelineState.blendInfo.setBlendFunction(srcFactor, dstFactor);
    }

    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        PipelineState.blendInfo.setBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    public static void blendOp(int op) {
        PipelineState.blendInfo.setBlendOp(op);
    }

    public static void enableColorLogicOp() {
        logicOp = true;
    }

    public static void disableColorLogicOp() {
        logicOp = false;
    }

    public static void logicOp(int glLogicOp) {
        logicOpFun = glLogicOp;
    }

    public static void polygonOffset(float slope, float biasConstant) {
        if (depthBiasConstant != biasConstant || depthBiasSlope != slope) {
            depthBiasConstant = biasConstant;
            depthBiasSlope = slope;

            Renderer.setDepthBias(depthBiasConstant, depthBiasSlope);
        }
    }

    public static void enablePolygonOffset() {
        if (!depthBiasEnabled) {
            Renderer.setDepthBias(depthBiasConstant, depthBiasSlope);
            depthBiasEnabled = true;
        }
    }

    public static void disablePolygonOffset() {
        if (depthBiasEnabled) {
            Renderer.setDepthBias(0.0F, 0.0F);
            depthBiasEnabled = false;
        }
    }

}
