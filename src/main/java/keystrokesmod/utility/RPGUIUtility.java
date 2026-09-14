package keystrokesmod.utility;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.IChatComponent;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

public final class RPGUIUtility {
    private static final int MERCHANT_TRADE_MAX_ROWS = 8;
    private static final int MERCHANT_TRADE_COLUMN_WIDTH = 92;
    private static final int MERCHANT_TRADE_ROW_HEIGHT = 19;

    public static void drawChestBackground(net.minecraft.client.gui.inventory.GuiChest guiChest) {
        drawPanel(guiChest.guiLeft, guiChest.guiTop, guiChest.xSize, guiChest.ySize);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + 17, 9, guiChest.inventoryRows);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + guiChest.ySize - 82, 9, 3);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + guiChest.ySize - 24, 9, 1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawChestForeground(net.minecraft.client.gui.inventory.GuiChest guiChest) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        drawLabel(fontRenderer, guiChest.lowerChestInventory.getDisplayName(), 8, 6, 0xFFFFFFFF);
        drawLabel(fontRenderer, guiChest.upperChestInventory.getDisplayName(), 8, guiChest.ySize - 96 + 2, 0xFF3F3F42);
    }

    public static void updateMerchantGuiSize(GuiMerchant guiMerchant) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        int maxColumns = Math.max(1, (guiMerchant.width - 192) / MERCHANT_TRADE_COLUMN_WIDTH);
        int columns = merchantRecipeList == null
            ? 1
            : Math.max(1, Math.min((merchantRecipeList.size() + MERCHANT_TRADE_MAX_ROWS - 1) / MERCHANT_TRADE_MAX_ROWS, maxColumns));
        int width = 184 + columns * MERCHANT_TRADE_COLUMN_WIDTH;
        if (guiMerchant.xSize != width) {
            guiMerchant.xSize = width;
            guiMerchant.guiLeft = (guiMerchant.width - width) / 2;
        }
    }

    public static int clampMerchantTradeScroll(GuiMerchant guiMerchant, int scrollRow) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList == null || merchantRecipeList.isEmpty()) {
            return 0;
        }

        int columns = Math.max(1, (merchantRecipeList.size() + MERCHANT_TRADE_MAX_ROWS - 1) / MERCHANT_TRADE_MAX_ROWS);
        int maxColumns = Math.max(1, (guiMerchant.width - 192) / MERCHANT_TRADE_COLUMN_WIDTH);
        columns = Math.min(columns, maxColumns);
        int rows = (merchantRecipeList.size() + columns - 1) / columns;
        int visibleRows = Math.max(1, (guiMerchant.ySize - 16 + MERCHANT_TRADE_ROW_HEIGHT - 1) / MERCHANT_TRADE_ROW_HEIGHT);
        return Math.max(0, Math.min(scrollRow, Math.max(0, rows - visibleRows)));
    }

    public static boolean isMerchantTradeListHovered(GuiMerchant guiMerchant, int mouseX, int mouseY) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        return merchantRecipeList != null && !merchantRecipeList.isEmpty()
            && mouseX >= guiMerchant.guiLeft + 182
            && mouseX < guiMerchant.guiLeft + guiMerchant.xSize - 4
            && mouseY >= guiMerchant.guiTop + 9
            && mouseY < guiMerchant.guiTop + guiMerchant.ySize - 8;
    }

    public static void drawMerchantBackground(GuiMerchant guiMerchant, int scrollRow) {
        drawPanel(guiMerchant.guiLeft, guiMerchant.guiTop, guiMerchant.xSize, guiMerchant.ySize);
        drawArrow(guiMerchant.guiLeft + 83, guiMerchant.guiTop + 21);
        drawSlot(guiMerchant.guiLeft + 36, guiMerchant.guiTop + 51);
        drawSlot(guiMerchant.guiLeft + 62, guiMerchant.guiTop + 51);
        drawSlot(guiMerchant.guiLeft + 120, guiMerchant.guiTop + 51);
        drawArrow(guiMerchant.guiLeft + 83, guiMerchant.guiTop + 48);
        drawSlotGrid(guiMerchant.guiLeft + 7, guiMerchant.guiTop + guiMerchant.ySize - 82, 9, 3);
        drawSlotGrid(guiMerchant.guiLeft + 7, guiMerchant.guiTop + guiMerchant.ySize - 24, 9, 1);

        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList != null && !merchantRecipeList.isEmpty()) {
            int columns = Math.max(1, (merchantRecipeList.size() + MERCHANT_TRADE_MAX_ROWS - 1) / MERCHANT_TRADE_MAX_ROWS);
            int maxColumns = Math.max(1, (guiMerchant.width - 192) / MERCHANT_TRADE_COLUMN_WIDTH);
            columns = Math.min(columns, maxColumns);
            int rows = (merchantRecipeList.size() + columns - 1) / columns;
            int sidebarLeft = guiMerchant.guiLeft + 180;
            int sidebarTop = guiMerchant.guiTop + 7;
            Gui.drawRect(sidebarLeft, sidebarTop, guiMerchant.guiLeft + guiMerchant.xSize - 4, guiMerchant.guiTop + guiMerchant.ySize - 6, 0xFF9B9CA0);
            Gui.drawRect(sidebarLeft + 2, sidebarTop + 2, guiMerchant.guiLeft + guiMerchant.xSize - 6, guiMerchant.guiTop + guiMerchant.ySize - 8, 0xFFD5D5D7);
            int visibleRows = Math.max(1, (guiMerchant.ySize - 16 + MERCHANT_TRADE_ROW_HEIGHT - 1) / MERCHANT_TRADE_ROW_HEIGHT);
            int firstRow = clampMerchantTradeScroll(guiMerchant, scrollRow);
            int lastRow = Math.min(rows, firstRow + visibleRows);
            RenderUtils.scissorPushGui(guiMerchant.guiLeft + 182, guiMerchant.guiTop + 9,
                guiMerchant.xSize - 186, guiMerchant.ySize - 15);
            for (int column = 0; column < columns; column++) {
                for (int row = firstRow; row < lastRow; row++) {
                    int index = column * rows + row;
                    if (index >= merchantRecipeList.size()) {
                        continue;
                    }
                    int left = guiMerchant.guiLeft + 184 + column * MERCHANT_TRADE_COLUMN_WIDTH;
                    int top = guiMerchant.guiTop + 10 + (row - firstRow) * MERCHANT_TRADE_ROW_HEIGHT;
                    drawSlot(left, top);
                    drawSlot(left + 19, top);
                    drawSlot(left + 38, top);
                }
            }
            RenderUtils.scissorPop();
        } else {
            Gui.drawRect(guiMerchant.guiLeft + 180, guiMerchant.guiTop + 7, guiMerchant.guiLeft + guiMerchant.xSize - 4, guiMerchant.guiTop + guiMerchant.ySize - 6, 0xFF9B9CA0);
            Gui.drawRect(guiMerchant.guiLeft + 182, guiMerchant.guiTop + 9, guiMerchant.guiLeft + guiMerchant.xSize - 6, guiMerchant.guiTop + guiMerchant.ySize - 8, 0xFFD5D5D7);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawMerchantForeground(GuiMerchant guiMerchant) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        String title = guiMerchant.chatComponent == null ? "" : guiMerchant.chatComponent.getFormattedText();
        fontRenderer.drawString(title, 88 - fontRenderer.getStringWidth(title) / 2, 6, 0xFFFFFFFF);
        fontRenderer.drawString("交易", 188, 6, 0xFFFFFFFF);
        drawLabel(fontRenderer, Minecraft.getMinecraft().thePlayer.inventory.getDisplayName(), 8, guiMerchant.ySize - 96 + 2, 0xFF3F3F42);
    }

    public static ItemStack drawMerchantTradeList(GuiMerchant guiMerchant, int selectedMerchantRecipe, int mouseX, int mouseY, int scrollRow) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList == null || merchantRecipeList.isEmpty()) {
            return null;
        }

        int columns = Math.max(1, (merchantRecipeList.size() + MERCHANT_TRADE_MAX_ROWS - 1) / MERCHANT_TRADE_MAX_ROWS);
        int maxColumns = Math.max(1, (guiMerchant.width - 192) / MERCHANT_TRADE_COLUMN_WIDTH);
        columns = Math.min(columns, maxColumns);
        int rows = (merchantRecipeList.size() + columns - 1) / columns;
        int visibleRows = Math.max(1, (guiMerchant.ySize - 16 + MERCHANT_TRADE_ROW_HEIGHT - 1) / MERCHANT_TRADE_ROW_HEIGHT);
        int firstRow = clampMerchantTradeScroll(guiMerchant, scrollRow);
        int lastRow = Math.min(rows, firstRow + visibleRows);
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        ItemStack hoveredStack = null;
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderUtils.scissorPushGui(guiMerchant.guiLeft + 182, guiMerchant.guiTop + 9,
            guiMerchant.xSize - 186, guiMerchant.ySize - 15);
        for (int column = 0; column < columns; column++) {
            for (int row = firstRow; row < lastRow; row++) {
                int index = column * rows + row;
                if (index >= merchantRecipeList.size()) {
                    continue;
                }
                MerchantRecipe merchantRecipe = merchantRecipeList.get(index);
                int left = guiMerchant.guiLeft + 184 + column * MERCHANT_TRADE_COLUMN_WIDTH;
                int top = guiMerchant.guiTop + 10 + (row - firstRow) * MERCHANT_TRADE_ROW_HEIGHT;
                boolean hovered = mouseX >= left - 2 && mouseX < left + 56 && mouseY >= top - 1 && mouseY < top + 18;
                if (index == selectedMerchantRecipe) {
                    Gui.drawRect(left - 2, top - 1, left + 56, top + 18, 0x886B8ED6);
                } else if (hovered) {
                    Gui.drawRect(left - 2, top - 1, left + 56, top + 18, 0x443F3F42);
                }
                if (merchantRecipe.isRecipeDisabled()) {
                    Gui.drawRect(left - 2, top - 1, left + 56, top + 18, 0x663F3F42);
                }

                ItemStack itemToBuy = merchantRecipe.getItemToBuy();
                ItemStack secondItemToBuy = merchantRecipe.getSecondItemToBuy();
                ItemStack itemToSell = merchantRecipe.getItemToSell();
                if (itemToBuy != null) {
                    renderItem.renderItemAndEffectIntoGUI(itemToBuy, left + 1, top + 1);
                    renderItem.renderItemOverlays(fontRenderer, itemToBuy, left + 1, top + 1);
                }
                if (secondItemToBuy != null) {
                    renderItem.renderItemAndEffectIntoGUI(secondItemToBuy, left + 20, top + 1);
                    renderItem.renderItemOverlays(fontRenderer, secondItemToBuy, left + 20, top + 1);
                }
                if (itemToSell != null) {
                    renderItem.renderItemAndEffectIntoGUI(itemToSell, left + 39, top + 1);
                    renderItem.renderItemOverlays(fontRenderer, itemToSell, left + 39, top + 1);
                }
                if (hovered) {
                    int itemOffset = mouseX - left;
                    if (itemOffset < 18) {
                        hoveredStack = itemToBuy;
                    } else if (itemOffset < 37) {
                        hoveredStack = secondItemToBuy;
                    } else {
                        hoveredStack = itemToSell;
                    }
                }
            }
        }
        RenderUtils.scissorPop();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        return hoveredStack;
    }

    public static int getMerchantTradeIndex(GuiMerchant guiMerchant, int mouseX, int mouseY, int scrollRow) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList == null || merchantRecipeList.isEmpty()) {
            return -1;
        }

        int columns = Math.max(1, (merchantRecipeList.size() + MERCHANT_TRADE_MAX_ROWS - 1) / MERCHANT_TRADE_MAX_ROWS);
        int maxColumns = Math.max(1, (guiMerchant.width - 192) / MERCHANT_TRADE_COLUMN_WIDTH);
        columns = Math.min(columns, maxColumns);
        int rows = (merchantRecipeList.size() + columns - 1) / columns;
        int relativeX = mouseX - guiMerchant.guiLeft - 182;
        int relativeY = mouseY - guiMerchant.guiTop - 9;
        int visibleRows = Math.max(1, (guiMerchant.ySize - 16 + MERCHANT_TRADE_ROW_HEIGHT - 1) / MERCHANT_TRADE_ROW_HEIGHT);
        if (relativeX < 0 || relativeY < 0 || relativeX >= columns * MERCHANT_TRADE_COLUMN_WIDTH
            || relativeY >= visibleRows * MERCHANT_TRADE_ROW_HEIGHT) {
            return -1;
        }
        int column = relativeX / MERCHANT_TRADE_COLUMN_WIDTH;
        int row = relativeY / MERCHANT_TRADE_ROW_HEIGHT + clampMerchantTradeScroll(guiMerchant, scrollRow);
        if (column >= columns || row >= rows) {
            return -1;
        }
        int index = column * rows + row;
        return index < merchantRecipeList.size() ? index : -1;
    }

    public static void selectAndFillMerchantTrade(GuiMerchant guiMerchant, int selectedMerchantRecipe, boolean completeTrade) {
        ContainerMerchant containerMerchant = (ContainerMerchant) guiMerchant.inventorySlots;
        guiMerchant.selectedMerchantRecipe = selectedMerchantRecipe;
        containerMerchant.setCurrentRecipeIndex(selectedMerchantRecipe);
        PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer());
        packetBuffer.writeInt(selectedMerchantRecipe);
        PacketUtils.sendPacketNoEvent(new C17PacketCustomPayload("MC|TrSel", packetBuffer));
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList != null && selectedMerchantRecipe >= 0 && selectedMerchantRecipe < merchantRecipeList.size()) {
            MerchantRecipe merchantRecipe = merchantRecipeList.get(selectedMerchantRecipe);
            if (!merchantRecipe.isRecipeDisabled()) {
                if (completeTrade) {
                    completeMerchantTrade(guiMerchant, merchantRecipe);
                } else {
                    fillMerchantTrade(guiMerchant, merchantRecipe);
                }
            }
        }
    }

    private static void completeMerchantTrade(GuiMerchant guiMerchant, MerchantRecipe merchantRecipe) {
        ContainerMerchant containerMerchant = (ContainerMerchant) guiMerchant.inventorySlots;
        MerchantClickState state = new MerchantClickState(containerMerchant, Minecraft.getMinecraft().thePlayer);
        if (state.cursorStack != null) {
            return;
        }
        for (int index = 0; index < 2; index++) {
            if (state.slotStacks[index] != null) {
                if (!appendMerchantClick(state, index, 0, 1) || state.slotStacks[index] != null) {
                    return;
                }
            }
        }
        state.slotStacks[2] = null;
        if (!moveMerchantItemToSlot(state, merchantRecipe.getItemToBuy(), merchantRecipe.getItemToBuy().stackSize, 0)) {
            return;
        }
        ItemStack secondItemToBuy = merchantRecipe.getSecondItemToBuy();
        if (secondItemToBuy != null && !moveMerchantItemToSlot(state, secondItemToBuy, secondItemToBuy.stackSize, 1)) {
            return;
        }
        state.slotStacks[2] = merchantRecipe.getItemToSell().copy();
        if (!appendMerchantClick(state, 2, 0, 1)) {
            return;
        }
        for (C0EPacketClickWindow packet : state.packets) {
            PacketUtils.sendPacketNoEvent(packet);
        }
    }

    public static void fillMerchantTrade(GuiMerchant guiMerchant, MerchantRecipe merchantRecipe) {
        ContainerMerchant containerMerchant = (ContainerMerchant) guiMerchant.inventorySlots;
        MerchantClickState state = new MerchantClickState(containerMerchant, Minecraft.getMinecraft().thePlayer);
        if (state.cursorStack != null) {
            return;
        }
        for (int index = 0; index < 2; index++) {
            if (state.slotStacks[index] != null) {
                if (!appendMerchantClick(state, index, 0, 1) || state.slotStacks[index] != null) {
                    return;
                }
            }
        }
        state.slotStacks[2] = null;
        if (!moveMerchantItemToSlot(state, merchantRecipe.getItemToBuy(), merchantRecipe.getItemToBuy().stackSize, 0)) {
            return;
        }
        ItemStack secondItemToBuy = merchantRecipe.getSecondItemToBuy();
        if (secondItemToBuy != null && !moveMerchantItemToSlot(state, secondItemToBuy, secondItemToBuy.stackSize, 1)) {
            return;
        }
        for (C0EPacketClickWindow packet : state.packets) {
            PacketUtils.sendPacketNoEvent(packet);
        }
    }

    private static boolean moveMerchantItemToSlot(MerchantClickState state, ItemStack requiredItem, int amount, int targetSlot) {
        if (requiredItem == null) {
            return true;
        }
        int remaining = amount;
        for (int index = 3; index < 39 && index < state.slotStacks.length && remaining > 0; index++) {
            ItemStack sourceStack = state.slotStacks[index];
            if (!matchesMerchantItem(sourceStack, requiredItem)) {
                continue;
            }
            int sourceAmount = sourceStack.stackSize;
            int moveAmount = Math.min(remaining, sourceAmount);
            if (!appendMerchantClick(state, index, 0, 0)) {
                return false;
            }
            if (moveAmount == sourceAmount) {
                if (!appendMerchantClick(state, targetSlot, 0, 0)) {
                    return false;
                }
            } else {
                for (int count = 0; count < moveAmount; count++) {
                    if (!appendMerchantClick(state, targetSlot, 1, 0)) {
                        return false;
                    }
                }
                if (!appendMerchantClick(state, index, 0, 0)) {
                    return false;
                }
            }
            remaining -= moveAmount;
        }
        return remaining == 0 && state.cursorStack == null;
    }

    private static boolean appendMerchantClick(MerchantClickState state, int slotId, int clickedButton, int mode) {
        if (slotId < 0 || slotId >= state.slotStacks.length || (mode != 0 && mode != 1)
            || clickedButton < 0 || clickedButton > 1) {
            return false;
        }
        Slot slot = state.containerMerchant.inventorySlots.get(slotId);
        ItemStack slotStack = state.slotStacks[slotId];
        ItemStack clickedItem = slotStack == null ? null : slotStack.copy();
        boolean changed = false;
        if (mode == 1) {
            if (slotStack == null || !slot.canTakeStack(state.player)) {
                return false;
            }
            ItemStack originalStack = slotStack.copy();
            if (!mergeMerchantStack(state, slotStack, 3, 39, slotId == 2)) {
                return false;
            }
            if (slotStack.stackSize == 0) {
                state.slotStacks[slotId] = null;
            }
            clickedItem = originalStack;
            changed = true;
        } else if (slotStack == null) {
            if (state.cursorStack == null || !slot.isItemValid(state.cursorStack)) {
                return false;
            }
            int amount = clickedButton == 0 ? state.cursorStack.stackSize : 1;
            amount = Math.min(amount, slot.getItemStackLimit(state.cursorStack));
            amount = Math.min(amount, state.cursorStack.getMaxStackSize());
            if (amount <= 0) {
                return false;
            }
            ItemStack placedStack = state.cursorStack.copy();
            placedStack.stackSize = amount;
            state.slotStacks[slotId] = placedStack;
            state.cursorStack.stackSize -= amount;
            if (state.cursorStack.stackSize <= 0) {
                state.cursorStack = null;
            }
            changed = true;
        } else if (state.cursorStack == null) {
            if (!slot.canTakeStack(state.player)) {
                return false;
            }
            int amount = clickedButton == 0 ? slotStack.stackSize : (slotStack.stackSize + 1) / 2;
            if (amount <= 0) {
                return false;
            }
            state.cursorStack = slotStack.copy();
            state.cursorStack.stackSize = amount;
            slotStack.stackSize -= amount;
            if (slotStack.stackSize <= 0) {
                state.slotStacks[slotId] = null;
            }
            changed = true;
        } else if (slot.canTakeStack(state.player) && slot.isItemValid(state.cursorStack)
            && ItemStack.areItemsEqual(slotStack, state.cursorStack)
            && ItemStack.areItemStackTagsEqual(slotStack, state.cursorStack)) {
            int amount = clickedButton == 0 ? state.cursorStack.stackSize : 1;
            amount = Math.min(amount, slot.getItemStackLimit(state.cursorStack) - slotStack.stackSize);
            amount = Math.min(amount, state.cursorStack.getMaxStackSize() - slotStack.stackSize);
            if (amount > 0) {
                slotStack.stackSize += amount;
                state.cursorStack.stackSize -= amount;
                if (state.cursorStack.stackSize <= 0) {
                    state.cursorStack = null;
                }
                changed = true;
            }
        } else if (slot.canTakeStack(state.player) && slot.isItemValid(state.cursorStack)
            && state.cursorStack.stackSize <= slot.getItemStackLimit(state.cursorStack)) {
            state.slotStacks[slotId] = state.cursorStack;
            state.cursorStack = slotStack;
            changed = true;
        }
        if (!changed) {
            return false;
        }
        state.packets.add(new C0EPacketClickWindow(state.containerMerchant.windowId, slotId, clickedButton, mode,
            clickedItem, state.containerMerchant.getNextTransactionID(state.player.inventory)));
        return true;
    }

    private static boolean mergeMerchantStack(MerchantClickState state, ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean merged = false;
        int index = reverseDirection ? endIndex - 1 : startIndex;
        if (stack.isStackable()) {
            while (stack.stackSize > 0 && ((!reverseDirection && index < endIndex) || (reverseDirection && index >= startIndex))) {
                ItemStack targetStack = state.slotStacks[index];
                if (targetStack != null && sameMerchantStack(stack, targetStack)) {
                    int combinedSize = targetStack.stackSize + stack.stackSize;
                    if (combinedSize <= stack.getMaxStackSize()) {
                        stack.stackSize = 0;
                        targetStack.stackSize = combinedSize;
                        merged = true;
                    } else if (targetStack.stackSize < stack.getMaxStackSize()) {
                        stack.stackSize -= stack.getMaxStackSize() - targetStack.stackSize;
                        targetStack.stackSize = stack.getMaxStackSize();
                        merged = true;
                    }
                }
                index += reverseDirection ? -1 : 1;
            }
        }
        index = reverseDirection ? endIndex - 1 : startIndex;
        while (stack.stackSize > 0 && ((!reverseDirection && index < endIndex) || (reverseDirection && index >= startIndex))) {
            Slot slot = state.containerMerchant.inventorySlots.get(index);
            if (state.slotStacks[index] == null && slot.isItemValid(stack)) {
                state.slotStacks[index] = stack.copy();
                stack.stackSize = 0;
                merged = true;
                break;
            }
            index += reverseDirection ? -1 : 1;
        }
        return merged;
    }

    private static boolean sameMerchantStack(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null && secondStack != null && firstStack.getItem() == secondStack.getItem()
            && (!firstStack.getHasSubtypes() || firstStack.getMetadata() == secondStack.getMetadata())
            && ItemStack.areItemStackTagsEqual(firstStack, secondStack);
    }

    private static boolean matchesMerchantItem(ItemStack itemStack, ItemStack requiredItem) {
        return itemStack != null && requiredItem != null && ItemStack.areItemsEqual(itemStack, requiredItem)
            && (!requiredItem.hasTagCompound() || ItemStack.areItemStackTagsEqual(requiredItem, itemStack));
    }

    private static final class MerchantClickState {
        private final ContainerMerchant containerMerchant;
        private final EntityPlayer player;
        private final ItemStack[] slotStacks;
        private final List<C0EPacketClickWindow> packets = new ArrayList<>();
        private ItemStack cursorStack;

        private MerchantClickState(ContainerMerchant containerMerchant, EntityPlayer player) {
            this.containerMerchant = containerMerchant;
            this.player = player;
            this.slotStacks = new ItemStack[containerMerchant.inventorySlots.size()];
            for (int index = 0; index < this.slotStacks.length; index++) {
                ItemStack stack = containerMerchant.inventorySlots.get(index).getStack();
                this.slotStacks[index] = stack == null ? null : stack.copy();
            }
            ItemStack cursorStack = player.inventory.getItemStack();
            this.cursorStack = cursorStack == null ? null : cursorStack.copy();
        }
    }

    private static void drawPanel(int left, int top, int width, int height) {
        Gui.drawRect(left - 2, top - 2, left + width + 2, top + height + 2, 0xFF15151A);
        Gui.drawRect(left - 1, top - 1, left + width + 1, top + height + 1, 0xFF5B5C63);
        Gui.drawRect(left, top, left + width, top + height, 0xFFC9C9CC);
        Gui.drawRect(left + 2, top + 2, left + width - 2, top + 4, 0xFFF3F3F4);
        Gui.drawRect(left + 2, top + 2, left + 4, top + height - 2, 0xFFF3F3F4);
        Gui.drawRect(left + 2, top + height - 4, left + width - 2, top + height - 2, 0xFF9A9B9F);
        Gui.drawRect(left + width - 4, top + 2, left + width - 2, top + height - 2, 0xFF9A9B9F);
    }

    private static void drawSlotGrid(int left, int top, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                drawSlot(left + column * 18, top + row * 18);
            }
        }
    }

    private static void drawSlot(int left, int top) {
        Gui.drawRect(left, top, left + 18, top + 18, 0xFF34353A);
        Gui.drawRect(left + 1, top + 1, left + 17, top + 17, 0xFF929398);
        Gui.drawRect(left + 1, top + 1, left + 17, top + 2, 0xFFEFEFF0);
        Gui.drawRect(left + 1, top + 2, left + 2, top + 17, 0xFFEFEFF0);
        Gui.drawRect(left + 2, top + 16, left + 17, top + 17, 0xFF68696E);
        Gui.drawRect(left + 16, top + 2, left + 17, top + 16, 0xFF68696E);
    }

    private static void drawArrow(int left, int top) {
        Gui.drawRect(left, top + 8, left + 21, top + 13, 0xFFA5A6A9);
        Gui.drawRect(left + 19, top + 5, left + 24, top + 16, 0xFFA5A6A9);
        Gui.drawRect(left + 22, top + 2, left + 28, top + 19, 0xFFA5A6A9);
    }

    private static void drawLabel(FontRenderer fontRenderer, IChatComponent text, int x, int y, int color) {
        if (text != null) {
            fontRenderer.drawString(text.getFormattedText(), x, y, color);
        }
    }
}
