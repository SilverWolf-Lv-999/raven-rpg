package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import net.minecraft.entity.player.EntityPlayer;

public class NoInteractionLimit extends Module {
    public NoInteractionLimit() {
        super("No Interaction Limit", category.player);
    }

    public boolean canInteractWhileUsing(EntityPlayer player) {
        return isEnabled() && player != null && player.isUsingItem();
    }
}
