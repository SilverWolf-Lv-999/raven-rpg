package keystrokesmod.ui.mainui;

import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.io.IOException;

public class MainUI extends GuiScreen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("keystrokesmod", "textures/ui/main_ui.jpg");
    private static final String[] BUTTON_LABELS = {"Single Player", "Muti Player", "Option", "Exit"};

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float textureAspect = 3840.0f / 2160.0f;
        float screenAspect = this.width / (float) Math.max(1, this.height);
        float backgroundWidth = this.width;
        float backgroundHeight = this.height;
        float backgroundX = 0.0f;
        float backgroundY = 0.0f;

        if (screenAspect > textureAspect) {
            backgroundHeight = backgroundWidth / textureAspect;
            backgroundY = (this.height - backgroundHeight) * 0.5f;
        }
        else {
            backgroundWidth = backgroundHeight * textureAspect;
            backgroundX = (this.width - backgroundWidth) * 0.5f;
        }

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(Math.round(backgroundX), Math.round(backgroundY), 0.0f, 0.0f, Math.round(backgroundWidth), Math.round(backgroundHeight), 3840.0f, 2160.0f);

        float panelInset = Math.max(14.0f, this.width * 0.018f);
        float panelX = panelInset;
        float panelY = panelInset;
        float panelWidth = Math.max(190.0f, this.width / 3.0f - panelInset * 2.0f);
        float panelHeight = Math.max(180.0f, this.height - panelInset * 2.0f);

        RoundedUtils.drawRound(panelX, panelY, panelWidth, panelHeight, 26.0f, new Color(255, 255, 255, 118));
        RoundedUtils.drawRound(panelX, panelY, panelWidth, panelHeight, 26.0f, new Color(255, 255, 255, 102));
        RoundedUtils.drawRound(panelX + 1.0f, panelY + 1.0f, panelWidth - 2.0f, panelHeight - 2.0f, 25.0f, new Color(255, 255, 255, 30));

        RavenFontRenderer titleRenderer = FontManager.getRenderer("Sf-Bold", Math.max(30.0f, Math.min(44.0f, this.height * 0.08f)));
        RavenFontRenderer subtitleRenderer = FontManager.getRenderer("Sf-Regular", Math.max(13.0f, Math.min(17.0f, this.height * 0.028f)));
        RavenFontRenderer buttonRenderer = FontManager.getRenderer("Sf-Ui", Math.max(14.0f, Math.min(18.0f, this.height * 0.032f)));
        float contentCenterX = panelX + panelWidth * 0.5f;
        float titleY = Math.max(24.0f, this.height * 0.25f);
        float titleWidth = titleRenderer.getStringWidth("Raven");
        float titleX = contentCenterX - titleWidth * 0.5f;
        titleRenderer.drawString("Raven", titleX, titleY, 0xFF2C2733);

        float subtitleWidth = subtitleRenderer.getStringWidth("rpg");
        subtitleRenderer.drawString("rpg", titleX + titleWidth - subtitleWidth + 4.0f, titleY + titleRenderer.getFontHeight() - 2.0f, 0xFF6D6478);

        float buttonHeight = Math.max(32.0f, Math.min(46.0f, this.height * 0.055f));
        float buttonGap = Math.max(8.0f, this.height * 0.016f);
        float buttonWidth = Math.max(140.0f, panelWidth - 48.0f);
        float buttonX = contentCenterX - buttonWidth * 0.5f;
        float buttonStartY = Math.min(titleY + titleRenderer.getFontHeight() + 56.0f, this.height - buttonHeight * BUTTON_LABELS.length - buttonGap * (BUTTON_LABELS.length - 1) - 32.0f);

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            float buttonY = buttonStartY + i * (buttonHeight + buttonGap);
            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            RoundedUtils.drawRound(buttonX - 1.0f, buttonY - 1.0f, buttonWidth + 2.0f, buttonHeight + 2.0f, 13.0f, hovered ? new Color(211, 204, 220, 220) : new Color(213, 206, 221, 170));
            RoundedUtils.drawRound(buttonX, buttonY, buttonWidth, buttonHeight, 12.0f, hovered ? new Color(255, 255, 255, 244) : new Color(248, 245, 251, 218));
            float textWidth = buttonRenderer.getStringWidth(BUTTON_LABELS[i]);
            float textY = buttonY + (buttonHeight - buttonRenderer.getFontHeight()) * 0.5f;
            buttonRenderer.drawString(BUTTON_LABELS[i], contentCenterX - textWidth * 0.5f, textY, 0xFF292531);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        int buttonIndex = getButtonIndex(mouseX, mouseY);
        switch (buttonIndex) {
            case 0:
                this.mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 1:
                this.mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 2:
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                break;
            case 3:
                this.mc.shutdown();
                break;
            default:
                break;
        }
    }

    private int getButtonIndex(int mouseX, int mouseY) {
        RavenFontRenderer titleRenderer = FontManager.getRenderer("Sf-Bold", Math.max(30.0f, Math.min(44.0f, this.height * 0.08f)));
        float panelInset = Math.max(14.0f, this.width * 0.018f);
        float panelX = panelInset;
        float panelWidth = Math.max(190.0f, this.width / 3.0f - panelInset * 2.0f);
        float contentCenterX = panelX + panelWidth * 0.5f;
        float titleY = Math.max(24.0f, this.height * 0.25f);
        float buttonHeight = Math.max(32.0f, Math.min(46.0f, this.height * 0.055f));
        float buttonGap = Math.max(8.0f, this.height * 0.016f);
        float buttonWidth = Math.max(140.0f, panelWidth - 48.0f);
        float buttonX = contentCenterX - buttonWidth * 0.5f;
        float buttonStartY = Math.min(titleY + titleRenderer.getFontHeight() + 56.0f, this.height - buttonHeight * BUTTON_LABELS.length - buttonGap * (BUTTON_LABELS.length - 1) - 32.0f);

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            float buttonY = buttonStartY + i * (buttonHeight + buttonGap);
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                return i;
            }
        }
        return -1;
    }
}
