package keystrokesmod.utility.rpg.pickaxe;

import keystrokesmod.script.model.ItemStack;


public class RPGPickaxe {
    private boolean hasMaster;

    private final ItemStack stack;

    private int canBreakLevel = 1;

    public RPGPickaxe(ItemStack stack) {
        this.stack = stack;
        this.hasMaster = false;
    }

    public boolean isHasMaster() {
        return hasMaster;
    }

    public void setHasMaster(boolean hasMaster) {
        this.hasMaster = hasMaster;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setCanBreakLevel(int canBreakLevel) {
        this.canBreakLevel = canBreakLevel;
    }

    public int getCanBreakLevel() {
        return canBreakLevel;
    }
}
