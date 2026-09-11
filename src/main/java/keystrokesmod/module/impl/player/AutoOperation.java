package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class AutoOperation extends Module {
    private final SliderSetting operationDelay = new SliderSetting("Delay", " tick", 1.0, 0.0, 20.0, 0.1);
    private ItemStack operationStack;
    private Container lastContainer;
    private int lastWindowId = -1;
    private int operationButton;
    private int playerItemCount;
    private double pendingTicks;
    private boolean awaitingRefresh;

    public AutoOperation() {
        super("Auto Operation", category.player);
        this.registerSetting(this.operationDelay);
    }

    @Override
    public void onDisable() {
        this.clearOperation();
    }

    @Override
    public void onUpdate() {
        if (this.operationStack == null) {
            return;
        }
        if (!Utils.nullCheck()) {
            this.clearOperation();
            return;
        }

        Container container = mc.thePlayer.openContainer;
        if (this.awaitingRefresh) {
            if (container == null || container == mc.thePlayer.inventoryContainer || (container == this.lastContainer && container.windowId == this.lastWindowId)) {
                return;
            }

            int currentPlayerItemCount = this.getPlayerItemCount(container);
            if ((this.operationButton == 0 && currentPlayerItemCount <= this.playerItemCount) || (this.operationButton == 1 && currentPlayerItemCount >= this.playerItemCount)) {
                this.clearOperation();
                return;
            }

            this.playerItemCount = currentPlayerItemCount;
            this.awaitingRefresh = false;
            this.pendingTicks = this.operationDelay.getInput();
        }
        if (container == null || container == mc.thePlayer.inventoryContainer) {
            this.clearOperation();
            return;
        }
        if (this.pendingTicks > 0.0) {
            this.pendingTicks--;
            if (this.pendingTicks > 0.0) {
                return;
            }
        }

        if (this.operationButton == 1 && this.playerItemCount == 0) {
            this.clearOperation();
            return;
        }

        Slot slot = this.findOperationSlot(container);
        if (slot == null) {
            this.clearOperation();
            return;
        }

        this.clickOperationSlot(container, slot);
    }

    public boolean startOperation(Slot clickedSlot, int clickedButton, int clickType) {
        if (!this.isEnabled() || clickType != 1 || !net.minecraft.client.gui.GuiScreen.isCtrlKeyDown() || !Utils.nullCheck() || clickedSlot == null || clickedSlot.inventory == mc.thePlayer.inventory || clickedButton < 0 || clickedButton > 1) {
            return false;
        }

        ItemStack clickedStack = clickedSlot.getStack();
        Container container = mc.thePlayer.openContainer;
        if (clickedStack == null || container == null || container == mc.thePlayer.inventoryContainer) {
            return false;
        }

        this.clearOperation();
        this.operationStack = clickedStack.copy();
        this.operationButton = clickedButton;
        this.playerItemCount = this.getPlayerItemCount(container);
        this.clickOperationSlot(container, clickedSlot);
        return true;
    }

    private Slot findOperationSlot(Container container) {
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != mc.thePlayer.inventory && this.isSameOperationItem(slot.getStack())) {
                return slot;
            }
        }
        return null;
    }

    private int getPlayerItemCount(Container container) {
        int itemCount = 0;
        for (Slot slot : container.inventorySlots) {
            ItemStack slotStack = slot.getStack();
            if (slot.inventory == mc.thePlayer.inventory && this.isSameOperationItem(slotStack)) {
                itemCount += slotStack.stackSize;
            }
        }
        return itemCount;
    }

    private boolean isSameOperationItem(ItemStack itemStack) {
        return itemStack != null
                && itemStack.getItem() == this.operationStack.getItem()
                && itemStack.getMetadata() == this.operationStack.getMetadata()
                && itemStack.getDisplayName().equals(this.operationStack.getDisplayName());
    }

    private void clickOperationSlot(Container container, Slot slot) {
        this.lastContainer = container;
        this.lastWindowId = container.windowId;
        this.awaitingRefresh = true;
        mc.playerController.windowClick(this.lastWindowId, slot.slotNumber, this.operationButton, 1, mc.thePlayer);
    }

    private void clearOperation() {
        this.operationStack = null;
        this.lastContainer = null;
        this.lastWindowId = -1;
        this.operationButton = 0;
        this.playerItemCount = 0;
        this.pendingTicks = 0.0;
        this.awaitingRefresh = false;
    }
}
