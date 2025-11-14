/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vulkanmod.mixin.render.frapi;

import net.vulkanmod.render.chunk.build.frapi.render.BlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.renderer.block.ModelBlockRenderer;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererM {

    // TODO ThreadLocal look ups are slow, same goes for ItemRendererMixin
    @Unique
    private final ThreadLocal<BlockRenderContext> fabric_contexts = ThreadLocal.withInitial(BlockRenderContext::new);

    // TODO: frapi
//    @Inject(at = @At("HEAD"), method = "tesselateBlock", cancellable = true)
//    private void hookRender(BlockAndTintGetter blockAndTintGetter, List<BlockModelPart> list, BlockState blockState,
//                            BlockPos blockPos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, int i,
//                            CallbackInfo ci) {
//        if (!model.isVanillaAdapter()) {
//            BlockRenderContext context = fabric_contexts.get();
//            context.render(blockView, model, state, pos, matrix, buffer, cull, rand, seed, overlay);
//            ci.cancel();
//        }
//    }

}
