package net.vulkanmod.render.chunk.build.frapi.accessor;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.MeshItemCommand;

import java.util.List;

public interface AccessBatchingRenderCommandQueue {
	List<MeshItemCommand> fabric_getMeshItemCommands();
}
