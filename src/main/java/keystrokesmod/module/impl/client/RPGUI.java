package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.util.IChatComponent;

public class RPGUI extends Module {
    public RPGUI() {
        super("RPGUI", category.client, 0);
    }

    public static boolean isRpgUiEnabled() {
        return ModuleManager.rpgUi != null && ModuleManager.rpgUi.isEnabled();
    }

    public static boolean shouldStyleChest(GuiChest guiChest) {
        if (!isRpgUiEnabled() || guiChest.lowerChestInventory == null) {
            return false;
        }

        IChatComponent displayName = guiChest.lowerChestInventory.getDisplayName();
        return displayName != null && displayName.getUnformattedText().contains("万人纵横");
    }

    public static boolean shouldStyleMerchant() {
        return isRpgUiEnabled();
    }
}
