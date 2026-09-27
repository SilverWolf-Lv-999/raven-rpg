package keystrokesmod.ui.trade;

import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TradeManager {
    private static final List<TradeEntry> TRADES = new ArrayList<>();

    public static synchronized void add(MerchantRecipe merchantRecipe) {
        if (merchantRecipe == null) {
            return;
        }

        TradeEntry tradeEntry = new TradeEntry(merchantRecipe);
        for (TradeEntry entry : TRADES) {
            if (entry.matches(merchantRecipe)) {
                return;
            }
        }
        TRADES.add(tradeEntry);
    }

    public static synchronized void remove(int index) {
        if (index >= 0 && index < TRADES.size()) {
            TRADES.remove(index);
        }
    }

    public static synchronized void clear() {
        TRADES.clear();
    }

    public static synchronized List<TradeEntry> getTrades() {
        return Collections.unmodifiableList(new ArrayList<>(TRADES));
    }

    public static synchronized TradeEntry get(int index) {
        return index >= 0 && index < TRADES.size() ? TRADES.get(index) : null;
    }

    public static int count(ItemStack requiredItem, ItemStack[] inventory) {
        if (requiredItem == null || inventory == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : inventory) {
            if (matches(requiredItem, stack)) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public static int capacity(ItemStack requiredItem, ItemStack[] inventory) {
        if (requiredItem == null || inventory == null) {
            return 0;
        }

        int maxStackSize = Math.max(1, requiredItem.getMaxStackSize());
        int capacity = 0;
        for (ItemStack stack : inventory) {
            if (stack == null) {
                capacity += maxStackSize;
            } else if (matches(requiredItem, stack)) {
                capacity += Math.max(0, Math.min(maxStackSize, stack.getMaxStackSize()) - stack.stackSize);
            }
        }
        return capacity;
    }

    public static int bestSoulAmount(ItemStack requiredItem, int requiredAmount, ItemStack[] inventory, int soulAmount) {
        if (requiredItem == null || inventory == null || soulAmount <= 0) {
            return 0;
        }

        int missing = Math.max(0, requiredAmount - count(requiredItem, inventory));
        int availableSpace = capacity(requiredItem, inventory);
        int inventoryLimit = Math.max(1, requiredItem.getMaxStackSize()) * inventory.length;
        return Math.max(0, Math.min(Math.min(missing, availableSpace), Math.min(soulAmount, inventoryLimit)));
    }

    private static boolean matches(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null && secondStack != null
                && ItemStack.areItemsEqual(firstStack, secondStack)
                && ItemStack.areItemStackTagsEqual(firstStack, secondStack);
    }
}
