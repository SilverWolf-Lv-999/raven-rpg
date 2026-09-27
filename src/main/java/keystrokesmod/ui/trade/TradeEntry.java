package keystrokesmod.ui.trade;

import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

public class TradeEntry {
    private final ItemStack itemToBuy;
    private final ItemStack secondItemToBuy;
    private final ItemStack itemToSell;

    public TradeEntry(MerchantRecipe merchantRecipe) {
        this.itemToBuy = copy(merchantRecipe.getItemToBuy());
        this.secondItemToBuy = copy(merchantRecipe.getSecondItemToBuy());
        this.itemToSell = copy(merchantRecipe.getItemToSell());
    }

    public ItemStack getItemToBuy() {
        return copy(this.itemToBuy);
    }

    public ItemStack getSecondItemToBuy() {
        return copy(this.secondItemToBuy);
    }

    public ItemStack getItemToSell() {
        return copy(this.itemToSell);
    }

    public boolean matches(MerchantRecipe merchantRecipe) {
        return merchantRecipe != null
                && matches(this.itemToBuy, merchantRecipe.getItemToBuy())
                && matches(this.secondItemToBuy, merchantRecipe.getSecondItemToBuy())
                && matches(this.itemToSell, merchantRecipe.getItemToSell());
    }

    private static boolean matches(ItemStack firstStack, ItemStack secondStack) {
        return firstStack == null ? secondStack == null : secondStack != null
                && ItemStack.areItemsEqual(firstStack, secondStack)
                && ItemStack.areItemStackTagsEqual(firstStack, secondStack);
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }
}
