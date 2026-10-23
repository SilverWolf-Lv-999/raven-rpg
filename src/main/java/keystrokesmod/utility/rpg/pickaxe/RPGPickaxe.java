package keystrokesmod.utility.rpg.pickaxe;

import keystrokesmod.script.model.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class RPGPickaxe {
    public static final List<RPGPickaxe> temp = new ArrayList<>();

    private boolean hasMaster;

    private final ItemStack stack;


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
}
