package keystrokesmod.module.impl.player;

import keystrokesmod.event.GameTickEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoShell extends Module {
    private ItemStack pendingStack;
    private int pendingTicks;
    private int pendingStackSize;
    private boolean commandSent;
    public final SliderSetting delay = new SliderSetting("send cmd delay", 0.0, 0.0, 20.0, 1.0);

    public AutoShell() {
        super("Auto Shell", category.player);
        this.registerSetting(delay);
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
            ItemStack heldStack = mc.thePlayer.getHeldItem();
            if (!commandSent) {
                if (!isSameStack(heldStack, pendingStack)) {
                    return;
                }

                if (++pendingTicks < (int) delay.getInput()) {
                    return;
                }

                pendingStackSize = heldStack.stackSize;
                mc.thePlayer.sendChatMessage("/ils shell");
                commandSent = true;
                pendingTicks = 0;
                return;
            }

            if (heldStack != null && isSameStack(heldStack, pendingStack) && heldStack.stackSize >= pendingStackSize) {
                return;
            }

            resetState();
        }

        for (int inventoryIndex = 0; inventoryIndex < mc.thePlayer.inventory.mainInventory.length; inventoryIndex++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[inventoryIndex];
            if (stack == null || !containsSalePrice(stack)) {
                continue;
            }

            ItemStack targetStack = stack.copy();
            if (!moveToMainHand(inventoryIndex)) {
                continue;
            }

            pendingStack = targetStack;
            pendingTicks = 0;
            pendingStackSize = targetStack.stackSize;
            commandSent = false;
            return;
        }

        this.disable();
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

    private boolean isSameStack(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null
                && secondStack != null
                && firstStack.getItem() == secondStack.getItem()
                && firstStack.getMetadata() == secondStack.getMetadata()
                && ItemStack.areItemStackTagsEqual(firstStack, secondStack);
    }

    private void resetState() {
        pendingStack = null;
        pendingTicks = 0;
        pendingStackSize = 0;
        commandSent = false;
    }
}
