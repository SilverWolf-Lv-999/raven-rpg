package keystrokesmod.ui.trade;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class VTScreen extends GuiScreen {
    private int selectedIndex = -1;

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 60, this.height - 28, 120, 20, "Clear trades"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        List<TradeEntry> trades = TradeManager.getTrades();
        int left = Math.max(12, this.width / 2 - 150);
        int top = 24;
        this.drawCenteredString(this.fontRendererObj, "Villager trades", this.width / 2, 8, 0xFFFFFF);
        RenderItem renderItem = this.mc.getRenderItem();
        RenderHelper.enableGUIStandardItemLighting();
        for (int index = 0; index < trades.size(); index++) {
            int rowTop = top + index * 28;
            int color = index == this.selectedIndex ? 0x665E76B8 : 0x66333333;
            Gui.drawRect(left, rowTop, left + 300, rowTop + 24, color);
            TradeEntry entry = trades.get(index);
            ItemStack first = entry.getItemToBuy();
            ItemStack second = entry.getSecondItemToBuy();
            ItemStack result = entry.getItemToSell();
            if (first != null) {
                renderItem.renderItemAndEffectIntoGUI(first, left + 5, rowTop + 3);
                renderItem.renderItemOverlays(this.fontRendererObj, first, left + 5, rowTop + 3);
            }
            if (second != null) {
                renderItem.renderItemAndEffectIntoGUI(second, left + 31, rowTop + 3);
                renderItem.renderItemOverlays(this.fontRendererObj, second, left + 31, rowTop + 3);
            }
            if (result != null) {
                renderItem.renderItemAndEffectIntoGUI(result, left + 70, rowTop + 3);
                renderItem.renderItemOverlays(this.fontRendererObj, result, left + 70, rowTop + 3);
            }
            this.drawString(this.fontRendererObj, String.valueOf(index + 1), left + 100, rowTop + 8, 0xFFFFFF);
            if (mouseX >= left && mouseX < left + 300 && mouseY >= rowTop && mouseY < rowTop + 24) {
                ItemStack hovered = mouseX < left + 25 ? first : mouseX < left + 55 ? second : mouseX < left + 94 ? result : null;
                if (hovered != null) {
                    this.renderToolTip(hovered, mouseX, mouseY);
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            TradeManager.clear();
            this.selectedIndex = -1;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        List<TradeEntry> trades = TradeManager.getTrades();
        int left = Math.max(12, this.width / 2 - 150);
        int top = 24;
        for (int index = 0; index < trades.size(); index++) {
            int rowTop = top + index * 28;
            if (mouseX < left || mouseX >= left + 300 || mouseY < rowTop || mouseY >= rowTop + 24) {
                continue;
            }
            if (mouseButton == 1) {
                TradeManager.remove(index);
                this.selectedIndex = -1;
            } else {
                this.selectedIndex = index;
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_DELETE && this.selectedIndex >= 0) {
            TradeManager.remove(this.selectedIndex);
            this.selectedIndex = -1;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
