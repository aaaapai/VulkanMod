package net.vulkanmod.mixin.profiling;

import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.vulkanmod.render.profiling.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    @Unique
    private static final EnumMap<ChunkSectionLayerGroup, String> VK$GROUP_NAMES = new EnumMap<>(ChunkSectionLayerGroup.class);

    static {
        VK$GROUP_NAMES.put(ChunkSectionLayerGroup.OPAQUE, "Opaque_terrain");
        VK$GROUP_NAMES.put(ChunkSectionLayerGroup.TRANSLUCENT, "Translucent_terrain");
        VK$GROUP_NAMES.put(ChunkSectionLayerGroup.TRIPWIRE, "Tripwire");
    }

    @Inject(method = "renderGroup", at = @At("HEAD"))
    private void vk$pushRenderGroup(ChunkSectionLayerGroup group, CallbackInfo ci) {
        Profiler.getMainProfiler().push(VK$GROUP_NAMES.getOrDefault(group, group.label()));
    }

    @Inject(method = "renderGroup", at = @At("RETURN"))
    private void vk$popRenderGroup(ChunkSectionLayerGroup group, CallbackInfo ci) {
        Profiler.getMainProfiler().pop();
    }
}
