package net.vulkanmod.render.texture;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.queue.TransferQueue;

public class ImageUploadHelper {

    public static final ImageUploadHelper INSTANCE = new ImageUploadHelper();

    private final Queue queue;
    private CommandPool.CommandBuffer currentCmdBuffer;

    public ImageUploadHelper() {

        //False on AMDVlk Drivers, true on Mesa, RADV, Nvidia and most other Vendors
        boolean useDMAQueue = Queue.getQueueFamilies().permitDMAQueue;
        queue = useDMAQueue ? DeviceManager.getTransferQueue() : DeviceManager.getGraphicsQueue();
    }

    public boolean DMAMode() {
        return queue instanceof TransferQueue;
    }

    public void submitCommands() {
        if (this.currentCmdBuffer == null) {
            return;
        }

        queue.submitCommands(this.currentCmdBuffer);
        Synchronization.INSTANCE.addCommandBuffer(this.currentCmdBuffer);

        this.currentCmdBuffer = null;
    }

    public CommandPool.CommandBuffer getOrStartCommandBuffer() {
        if (this.currentCmdBuffer == null) {
            this.currentCmdBuffer = this.queue.beginCommands();
        }

        return this.currentCmdBuffer;
    }

    public CommandPool.CommandBuffer getCommandBuffer() {
        return this.currentCmdBuffer;
    }
}
