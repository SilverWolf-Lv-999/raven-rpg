package keystrokesmod.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;

public final class RPGUIUtility {
    public static void drawChestBackground(GuiChest guiChest) {
        drawPanel(guiChest.guiLeft, guiChest.guiTop, guiChest.xSize, guiChest.ySize);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + 17, 9, guiChest.inventoryRows);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + guiChest.ySize - 82, 9, 3);
        drawSlotGrid(guiChest.guiLeft + 7, guiChest.guiTop + guiChest.ySize - 24, 9, 1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawChestForeground(GuiChest guiChest) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        drawLabel(fontRenderer, guiChest.lowerChestInventory.getDisplayName(), 8, 6, 0xFFFFFFFF);
        drawLabel(fontRenderer, guiChest.upperChestInventory.getDisplayName(), 8, guiChest.ySize - 96 + 2, 0xFF3F3F42);
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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawMerchantForeground(GuiMerchant guiMerchant) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        String title = guiMerchant.chatComponent == null ? "" : guiMerchant.chatComponent.getFormattedText();
        fontRenderer.drawString(title, guiMerchant.xSize / 2 - fontRenderer.getStringWidth(title) / 2, 6, 0xFFFFFFFF);
        drawLabel(fontRenderer, Minecraft.getMinecraft().thePlayer.inventory.getDisplayName(), 8, guiMerchant.ySize - 96 + 2, 0xFF3F3F42);
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