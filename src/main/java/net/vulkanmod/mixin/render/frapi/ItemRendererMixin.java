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

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;

@Mixin(ItemRenderer.class)
abstract class ItemRendererMixin {
//    @Unique
//    private static final ThreadLocal<ItemRenderContext> CONTEXTS = ThreadLocal.withInitial(ItemRenderContext::new);

    // TODO: frapi
//    @Inject(method = "renderItem", at = @At(value = "HEAD"), cancellable = true)
//    private static void hookRenderItem(ItemDisplayContext itemDisplayContext, PoseStack poseStack,
//                                       MultiBufferSource multiBufferSource, int i, int j, int[] is,
//                                       List<BakedQuad> list, RenderType renderType,
//                                       ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
//        if (!model.isVanillaAdapter()) {
//            CONTEXTS.get().renderModel(itemDisplayContext, poseStack, multiBufferSource, i, j, is, model, renderType, foilType);
//            ci.cancel();
//        }
//    }
}