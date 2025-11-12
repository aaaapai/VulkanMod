package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexFormat.IndexType.class)
public class IndexTypeMixin {

    /**
     * @author
     * @reason Our chunk renderer only uploads 16-bit indices, so force Minecraft to pick the SHORT variant.
     */
    @Overwrite
    public static VertexFormat.IndexType least(int number) {
        return VertexFormat.IndexType.SHORT;
    }
}
