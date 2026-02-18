package net.vulkanmod.gl;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public abstract class GlUtil {

    public static SPIRVUtils.ShaderKind extToShaderKind(String ext) {
        return switch (ext) {
            case ".vsh" -> SPIRVUtils.ShaderKind.VERTEX_SHADER;
            case ".fsh" -> SPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            default -> throw new RuntimeException("unknown shader type: " + ext);
        };
    }

    //Created buffer will need to be freed
    public static ByteBuffer RGBtoRGBA_buffer(ByteBuffer in) {
        Validate.isTrue(in.remaining() % 3 == 0, "Unexpected buffer stride");

        int outSize = in.remaining() * 4 / 3;
        ByteBuffer out = MemoryUtil.memAlloc(outSize);

        int j = 0;
        for (int i = 0; i < outSize; i+=4, j+=3) {
            out.put(i, in.get(j));
            out.put(i + 1, in.get(j + 1));
            out.put(i + 2, in.get(j + 2));
            out.put(i + 3, (byte) 0xFF);
        }

        return out;
    }

    public static ByteBuffer BGRAtoRGBA_buffer(ByteBuffer in) {
        Validate.isTrue(in.remaining() % 4 == 0, "Unexpected buffer stride");

        int outSize = in.remaining();
        ByteBuffer out = MemoryUtil.memAlloc(outSize);

        long ptr = MemoryUtil.memAddress0(out);

        long srcPtr = MemoryUtil.memAddress0(in);

        // TODO write in place (don't free the returned buffer in that case)
        for (int i = 0; i < outSize ; i += 4) {
            int color = MemoryUtil.memGetInt(srcPtr + i);

            color = (color << 24) & 0xFF000000 | (color >> 8) & 0xFFFFFF;

            MemoryUtil.memPutInt(ptr + i, color);
        }

        return out;
    }

    public static int vulkanFormat(int glFormat, int type) {
        return switch (glFormat) {
            case GL11.GL_RGBA, GL30.GL_RGBA8 ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8G8B8A8_UNORM;
                        case GL11.GL_BYTE -> VK_FORMAT_R8G8B8A8_UNORM;
                        case GL30.GL_UNSIGNED_INT_8_8_8_8, GL30.GL_UNSIGNED_INT_8_8_8_8_REV -> VK_FORMAT_R8G8B8A8_UNORM;
                        default -> throw new IllegalStateException("Unexpected type: " + type);
                    };
            case GL30.GL_BGRA ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_B8G8R8A8_UNORM;
                        case GL11.GL_BYTE -> VK_FORMAT_B8G8R8A8_UNORM;
                        case GL30.GL_UNSIGNED_INT_8_8_8_8, GL30.GL_UNSIGNED_INT_8_8_8_8_REV -> VK_FORMAT_B8G8R8A8_UNORM;
                        default -> throw new IllegalStateException("Unexpected type: " + type);
                    };
            case GL30.GL_UNSIGNED_INT_8_8_8_8_REV ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8G8B8A8_UINT;
                        case GL11.GL_BYTE -> VK_FORMAT_R8G8B8A8_UNORM;
                        default -> throw new IllegalStateException("Unexpected type: " + type);
                    };
            case GL11.GL_RED ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8_UNORM;
                        default -> throw new IllegalStateException("Unexpected type: " + type);
                    };
            case GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT24 ->
                    Vulkan.getDefaultDepthFormat();

            // === Iris shader pack formats ===
            // HDR float formats
            case 0x8C3A /* GL_R11F_G11F_B10F */ -> VK_FORMAT_B10G11R11_UFLOAT_PACK32;
            case 0x881A /* GL_RGBA16F */ -> VK_FORMAT_R16G16B16A16_SFLOAT;
            case 0x8814 /* GL_RGBA32F */ -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case 0x881B /* GL_RGB16F */ -> VK_FORMAT_R16G16B16A16_SFLOAT; // Vulkan 3-component rarely supported
            case 0x8815 /* GL_RGB32F */ -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case 0x822D /* GL_R16F */ -> VK_FORMAT_R16_SFLOAT;
            case 0x822E /* GL_R32F */ -> VK_FORMAT_R32_SFLOAT;
            case 0x822F /* GL_RG16F */ -> VK_FORMAT_R16G16_SFLOAT;
            case 0x8230 /* GL_RG32F */ -> VK_FORMAT_R32G32_SFLOAT;
            // Normalized integer formats
            case 0x805B /* GL_RGBA16 */ -> VK_FORMAT_R16G16B16A16_UNORM;
            case 0x8229 /* GL_R8 */ -> VK_FORMAT_R8_UNORM;
            case 0x822B /* GL_RG8 */ -> VK_FORMAT_R8G8_UNORM;
            case 0x8051 /* GL_RGB8 */ -> VK_FORMAT_R8G8B8A8_UNORM; // No 3-component in Vulkan
            case 0x822A /* GL_R16 */ -> VK_FORMAT_R16_UNORM;
            case 0x822C /* GL_RG16 */ -> VK_FORMAT_R16G16_UNORM;
            // Depth/stencil formats
            case 0x81A5 /* GL_DEPTH_COMPONENT16 */ -> VK_FORMAT_D16_UNORM;
            case 0x81A7 /* GL_DEPTH_COMPONENT32 */ -> VK_FORMAT_D32_SFLOAT;
            case 0x88F0 /* GL_DEPTH24_STENCIL8 */ -> VK_FORMAT_D24_UNORM_S8_UINT;
            case 0x8CAD /* GL_DEPTH32F_STENCIL8 */ -> VK_FORMAT_D32_SFLOAT_S8_UINT;
            case 0x84F9 /* GL_DEPTH_STENCIL */ -> VK_FORMAT_D24_UNORM_S8_UINT;
            // Integer formats (unsigned)
            case 0x8D70 /* GL_RGBA32UI */ -> VK_FORMAT_R32G32B32A32_UINT;
            case 0x8D76 /* GL_RGBA16UI */ -> VK_FORMAT_R16G16B16A16_UINT;
            case 0x8D7C /* GL_RGBA8UI */ -> VK_FORMAT_R8G8B8A8_UINT;
            case 0x8236 /* GL_R32UI */ -> VK_FORMAT_R32_UINT;
            // Integer formats (signed)
            case 0x8D82 /* GL_RGBA32I */ -> VK_FORMAT_R32G32B32A32_SINT;
            case 0x8D88 /* GL_RGBA16I */ -> VK_FORMAT_R16G16B16A16_SINT;
            case 0x8D8E /* GL_RGBA8I */ -> VK_FORMAT_R8G8B8A8_SINT;
            // SNORM formats
            case 0x8F97 /* GL_RGBA8_SNORM */ -> VK_FORMAT_R8G8B8A8_SNORM;
            case 0x8F9B /* GL_RGBA16_SNORM */ -> VK_FORMAT_R16G16B16A16_SNORM;
            case 0x8F94 /* GL_R8_SNORM */ -> VK_FORMAT_R8_SNORM;
            case 0x8F98 /* GL_R16_SNORM */ -> VK_FORMAT_R16_SNORM;
            case 0x8F95 /* GL_RG8_SNORM */ -> VK_FORMAT_R8G8_SNORM;
            case 0x8F99 /* GL_RG16_SNORM */ -> VK_FORMAT_R16G16_SNORM;
            // RGB 3-component SNORM (no 3-component in Vulkan, use 4-component)
            case 0x8F96 /* GL_RGB8_SNORM */ -> VK_FORMAT_R8G8B8A8_SNORM;
            case 0x8F9A /* GL_RGB16_SNORM */ -> VK_FORMAT_R16G16B16A16_SNORM;
            // RGB 3-component integer (no 3-component in Vulkan, use 4-component)
            case 0x8D71 /* GL_RGB32UI */ -> VK_FORMAT_R32G32B32A32_UINT;
            case 0x8D77 /* GL_RGB16UI */ -> VK_FORMAT_R16G16B16A16_UINT;
            case 0x8D7D /* GL_RGB8UI */ -> VK_FORMAT_R8G8B8A8_UINT;
            case 0x8D83 /* GL_RGB32I */ -> VK_FORMAT_R32G32B32A32_SINT;
            case 0x8D89 /* GL_RGB16I */ -> VK_FORMAT_R16G16B16A16_SINT;
            case 0x8D8F /* GL_RGB8I */ -> VK_FORMAT_R8G8B8A8_SINT;
            // RGB normalized
            case 0x8054 /* GL_RGB16 */ -> VK_FORMAT_R16G16B16A16_UNORM;
            // RG integer
            case 0x8232 /* GL_R16UI */ -> VK_FORMAT_R16_UINT;
            case 0x8234 /* GL_RG16UI */ -> VK_FORMAT_R16G16_UINT;
            case 0x8235 /* GL_RG32UI */ -> VK_FORMAT_R32G32_UINT;
            case 0x8233 /* GL_R16I */ -> VK_FORMAT_R16_SINT;

            default -> throw new IllegalStateException("Unexpected format: " + glFormat);
        };
    }

    // TODO: refactor
    public static int vulkanFormat(int glInternalFormat) {
        return switch (glInternalFormat) {
            case GL30.GL_UNSIGNED_INT_8_8_8_8_REV -> VK_FORMAT_R8G8B8A8_UINT;
            case GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT24 ->
//                    switch (type) {
//                        case GL11.GL_FLOAT -> VK_FORMAT_D32_SFLOAT;
//                        default -> throw new IllegalStateException("Unexpected value: " + type);
//                    };
                    Vulkan.getDefaultDepthFormat();

            default -> throw new IllegalStateException("Unexpected value: " + glInternalFormat);
        };
    }

    public static int getGlFormat(int vFormat) {
        return switch (vFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM -> GL11.GL_RGBA;
            case VK_FORMAT_B8G8R8A8_UNORM -> GL30.GL_BGRA;
            case VK_FORMAT_R8G8_UNORM -> GL30.GL_RG;
            case VK_FORMAT_R8_UNORM -> GL11.GL_RED;
            default -> throw new IllegalStateException("Unexpected value: " + vFormat);
        };
    }
}
