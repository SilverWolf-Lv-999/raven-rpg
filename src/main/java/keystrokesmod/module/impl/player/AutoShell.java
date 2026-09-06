package keystrokesmod.module.impl.player;

import keystrokesmod.event.GameTickEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.utility.Utils;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoShell extends Module {
    private ItemStack pendingStack;
    private int pendingTicks;

    public AutoShell() {
        super("Auto Shell", category.player);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @SubscribeEvent
    public void onGameTick(GameTickEvent event) {
        if (!Utils.nullCheck()) {
            resetState();
            return;
        }

        if (pendingStack != null) {
            pendingTicks++;
            ItemStack heldStack = mc.thePlayer.getHeldItem();
            if (!isPendingStackProcessed(heldStack) && pendingTicks < 40) {
                return;
            }
            pendingStack = null;
            pendingTicks = 0;
        }

        for (int inventoryIndex = 0; inventoryIndex < mc.thePlayer.inventory.mainInventory.length; inventoryIndex++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[inventoryIndex];
            if (stack == null || !containsSalePrice(stack)) {
                continue;
            }

            if (!moveToMainHand(inventoryIndex)) {
                continue;
            }

            ItemStack heldStack = mc.thePlayer.getHeldItem();
            if (heldStack == null) {
                continue;
            }

            pendingStack = heldStack.copy();
            pendingTicks = 0;
            mc.thePlayer.sendChatMessage("/ils shell");
            return;
        }
    }

    private boolean containsSalePrice(ItemStack stack) {
        for (String tooltipLine : stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips)) {
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(tooltipLine);
            if (text == null) {
                continue;
            }

            int saleStart = text.indexOf("售价");
            int coinStart = text.indexOf("金币", saleStart + 2);
            if (saleStart < 0 || coinStart < 0) {
                continue;
            }

            String price = text.substring(saleStart + 2, coinStart)
                    .replace(":", "")
                    .replace("：", "")
                    .replace(",", "")
                    .replace("，", "")
                    .trim();
            if (price.isEmpty()) {
                continue;
            }

            boolean numeric = true;
            for (int index = 0; index < price.length(); index++) {
                if (!Character.isDigit(price.charAt(index))) {
                    numeric = false;
                    break;
                }
            }
            if (numeric) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToMainHand(int inventoryIndex) {
        int currentItem = mc.thePlayer.inventory.currentItem;
        if (inventoryIndex < 9) {
            if (inventoryIndex != currentItem) {
                mc.thePlayer.inventory.currentItem = inventoryIndex;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
            }
            return true;
        }

        Slot inventorySlot = null;
        for (Object slotObject : mc.thePlayer.openContainer.inventorySlots) {
            Slot slot = (Slot) slotObject;
            if (slot.inventory == mc.thePlayer.inventory && slot.getSlotIndex() == inventoryIndex) {
                inventorySlot = slot;
                break;
            }
        }
        if (inventorySlot == null) {
            return false;
        }

        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, inventorySlot.slotNumber, currentItem, 2, mc.thePlayer);
        return true;
    }

    private boolean isPendingStackProcessed(ItemStack heldStack) {
        if (heldStack == null || heldStack.getItem() != pendingStack.getItem() || heldStack.getMetadata() != pendingStack.getMetadata()) {
            return true;
        }
        if (!ItemStack.areItemStackTagsEqual(heldStack, pendingStack)) {
            return true;
        }
        return heldStack.stackSize < pendingStack.stackSize;
    }

    private void resetState() {
        pendingStack = null;
        pendingTicks = 0;
    }
}
