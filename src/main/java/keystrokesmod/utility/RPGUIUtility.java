package keystrokesmod.utility;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.IChatComponent;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

public final class RPGUIUtility {
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
        int columns = merchantRecipeList == null ? 1 : Math.max(1, (merchantRecipeList.size() + 7) / 8);
        int width = 184 + columns * 92;
        if (guiMerchant.xSize != width) {
            guiMerchant.xSize = width;
            guiMerchant.guiLeft = (guiMerchant.width - width) / 2;
        }
    }

    public static void drawMerchantBackground(GuiMerchant guiMerchant) {
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
            int columns = Math.max(1, (merchantRecipeList.size() + 7) / 8);
            int rows = (merchantRecipeList.size() + columns - 1) / columns;
            int sidebarLeft = guiMerchant.guiLeft + 180;
            int sidebarTop = guiMerchant.guiTop + 7;
            Gui.drawRect(sidebarLeft, sidebarTop, guiMerchant.guiLeft + guiMerchant.xSize - 4, guiMerchant.guiTop + guiMerchant.ySize - 6, 0xFF9B9CA0);
            Gui.drawRect(sidebarLeft + 2, sidebarTop + 2, guiMerchant.guiLeft + guiMerchant.xSize - 6, guiMerchant.guiTop + guiMerchant.ySize - 8, 0xFFD5D5D7);
            for (int index = 0; index < merchantRecipeList.size(); index++) {
                int column = index / rows;
                int row = index % rows;
                int left = guiMerchant.guiLeft + 184 + column * 92;
                int top = guiMerchant.guiTop + 10 + row * 19;
                drawSlot(left, top);
                drawSlot(left + 19, top);
                drawSlot(left + 38, top);
            }
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

    public static ItemStack drawMerchantTradeList(GuiMerchant guiMerchant, int selectedMerchantRecipe, int mouseX, int mouseY) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList == null || merchantRecipeList.isEmpty()) {
            return null;
        }

        int columns = Math.max(1, (merchantRecipeList.size() + 7) / 8);
        int rows = (merchantRecipeList.size() + columns - 1) / columns;
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        ItemStack hoveredStack = null;
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        for (int index = 0; index < merchantRecipeList.size(); index++) {
            MerchantRecipe merchantRecipe = merchantRecipeList.get(index);
            int column = index / rows;
            int row = index % rows;
            int left = guiMerchant.guiLeft + 184 + column * 92;
            int top = guiMerchant.guiTop + 10 + row * 19;
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
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        return hoveredStack;
    }

    public static int getMerchantTradeIndex(GuiMerchant guiMerchant, int mouseX, int mouseY) {
        MerchantRecipeList merchantRecipeList = guiMerchant.getMerchant().getRecipes(Minecraft.getMinecraft().thePlayer);
        if (merchantRecipeList == null || merchantRecipeList.isEmpty()) {
            return -1;
        }

        int columns = Math.max(1, (merchantRecipeList.size() + 7) / 8);
        int rows = (merchantRecipeList.size() + columns - 1) / columns;
        int relativeX = mouseX - guiMerchant.guiLeft - 182;
        int relativeY = mouseY - guiMerchant.guiTop - 9;
        if (relativeX < 0 || relativeY < 0) {
            return -1;
        }
        int column = relativeX / 92;
        int row = relativeY / 19;
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
        Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C17PacketCustomPayload("MC|TrSel", packetBuffer));
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
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        ContainerMerchant containerMerchant = (ContainerMerchant) guiMerchant.inventorySlots;
        ItemStack itemToBuy = merchantRecipe.getItemToBuy();
        ItemStack secondItemToBuy = merchantRecipe.getSecondItemToBuy();
        int itemToBuyCount = 0;
        int secondItemToBuyCount = 0;
        for (int index = 3; index < 39 && index < containerMerchant.inventorySlots.size(); index++) {
            ItemStack itemStack = containerMerchant.inventorySlots.get(index).getStack();
            if (matchesMerchantItem(itemStack, itemToBuy)) {
                itemToBuyCount += itemStack.stackSize;
            }
            if (secondItemToBuy != null && matchesMerchantItem(itemStack, secondItemToBuy)) {
                secondItemToBuyCount += itemStack.stackSize;
            }
        }
        boolean sameItem = secondItemToBuy != null
            && ItemStack.areItemsEqual(itemToBuy, secondItemToBuy)
            && ItemStack.areItemStackTagsEqual(itemToBuy, secondItemToBuy);
        if (sameItem ? itemToBuyCount < itemToBuy.stackSize + secondItemToBuy.stackSize
            : itemToBuyCount < itemToBuy.stackSize || secondItemToBuyCount < secondItemToBuy.stackSize) {
            return;
        }
        for (int index = 0; index < 2; index++) {
            if (containerMerchant.inventorySlots.get(index).getHasStack()) {
                minecraft.playerController.windowClick(containerMerchant.windowId, index, 0, 1, player);
            }
        }
        if (containerMerchant.inventorySlots.get(0).getHasStack() || containerMerchant.inventorySlots.get(1).getHasStack()
            || player.inventory.getItemStack() != null) {
            return;
        }
        if (!moveMerchantItemToSlot(guiMerchant, itemToBuy, itemToBuy.stackSize, 0)) {
            return;
        }
        if (secondItemToBuy != null && !moveMerchantItemToSlot(guiMerchant, secondItemToBuy, secondItemToBuy.stackSize, 1)) {
            return;
        }
        if (containerMerchant.inventorySlots.get(2).getHasStack()) {
            minecraft.playerController.windowClick(containerMerchant.windowId, 2, 0, 1, player);
        }
    }

    public static void fillMerchantTrade(GuiMerchant guiMerchant, MerchantRecipe merchantRecipe) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        ContainerMerchant containerMerchant = (ContainerMerchant)guiMerchant.inventorySlots;
        for (int index = 0; index < 2; index++) {
            if (containerMerchant.inventorySlots.get(index).getHasStack()) {
                minecraft.playerController.windowClick(containerMerchant.windowId, index, 0, 1, player);
            }
        }
        quickMoveMerchantItem(guiMerchant, merchantRecipe.getItemToBuy());
        quickMoveMerchantItem(guiMerchant, merchantRecipe.getSecondItemToBuy());
    }

    private static void quickMoveMerchantItem(GuiMerchant guiMerchant, ItemStack requiredItem) {
        if (requiredItem == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        for (int index = 3; index < 39 && index < guiMerchant.inventorySlots.inventorySlots.size(); index++) {
            Slot slot = guiMerchant.inventorySlots.inventorySlots.get(index);
            ItemStack itemStack = slot.getStack();
            if (matchesMerchantItem(itemStack, requiredItem)) {
                minecraft.playerController.windowClick(guiMerchant.inventorySlots.windowId, slot.slotNumber, 0, 1, player);
                return;
            }
        }
    }

    private static boolean moveMerchantItemToSlot(GuiMerchant guiMerchant, ItemStack requiredItem, int amount, int targetSlot) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        int remaining = amount;
        for (int index = 3; index < 39 && index < guiMerchant.inventorySlots.inventorySlots.size() && remaining > 0; index++) {
            Slot sourceSlot = guiMerchant.inventorySlots.inventorySlots.get(index);
            ItemStack sourceStack = sourceSlot.getStack();
            if (!matchesMerchantItem(sourceStack, requiredItem)) {
                continue;
            }
            int sourceAmount = sourceStack.stackSize;
            int moveAmount = Math.min(remaining, sourceAmount);
            minecraft.playerController.windowClick(guiMerchant.inventorySlots.windowId, sourceSlot.slotNumber, 0, 0, player);
            if (moveAmount == sourceAmount) {
                minecraft.playerController.windowClick(guiMerchant.inventorySlots.windowId, targetSlot, 0, 0, player);
            } else {
                for (int count = 0; count < moveAmount; count++) {
                    minecraft.playerController.windowClick(guiMerchant.inventorySlots.windowId, targetSlot, 1, 0, player);
                }
                minecraft.playerController.windowClick(guiMerchant.inventorySlots.windowId, sourceSlot.slotNumber, 0, 0, player);
            }
            remaining -= moveAmount;
        }
        return remaining == 0 && player.inventory.getItemStack() == null;
    }

    private static boolean matchesMerchantItem(ItemStack itemStack, ItemStack requiredItem) {
        return itemStack != null && requiredItem != null && ItemStack.areItemsEqual(itemStack, requiredItem)
            && (!requiredItem.hasTagCompound() || ItemStack.areItemStackTagsEqual(requiredItem, itemStack));
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
