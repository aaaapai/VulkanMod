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

import net.fabricmc.fabric.api.renderer.v1.render.FabricLayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemStackRenderState.LayerRenderState.class)
abstract class LayerRenderStateMixin implements FabricLayerRenderState {
//abstract class LayerRenderStateMixin implements FabricLayerRenderState, AccessLayerRenderState {
//	@Unique
//	private final MutableMeshImpl mutableMesh = new MutableMeshImpl();
//
//	@Inject(method = "clear()V", at = @At("RETURN"))
//	private void onReturnClear(CallbackInfo ci) {
//		mutableMesh.clear();
//	}

	// TODO: frapi
//	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"))
//	private void renderItemProxy(net.minecraft.world.item.ItemDisplayContext itemDisplayContext, PoseStack poseStack,
//								 MultiBufferSource multiBufferSource, int i, int j, int[] is,
//								 List<net.minecraft.client.renderer.block.model.BakedQuad> list, RenderType renderType,
//								 ItemStackRenderState.FoilType foilType) {
//		if (mutableMesh.size() > 0) {
//			ItemRenderContext.POOL.get().renderItem(displayContext, matrices, vertexConsumers, light, overlay, tints, quads, mutableMesh, layer, glint);
//		} else {
//			ItemRenderer.renderItem(displayContext, matrices, vertexConsumers, light, overlay, tints, quads, layer, glint);
//		}
//	}

//	@Override
//	public MutableMeshImpl fabric_getMutableMesh() {
//		return mutableMesh;
//	}
}
