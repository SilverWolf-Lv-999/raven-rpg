package keystrokesmod.utility.rpg.gun;

import keystrokesmod.script.model.ItemStack;

public class RPGGun {
    public ItemStack gunItem;
    public int bullet;
    public boolean reloading;

    public RPGGun(ItemStack gunItem, int bullet, boolean reloading) {
        this.gunItem = gunItem;
        this.bullet = bullet;
        this.reloading = reloading;
    }
}
