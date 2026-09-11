package keystrokesmod.utility.rpg;

import net.minecraft.item.Item;

public class RPGMoney {
    private final String moneyName;
    private final Item moneyItem;
    private final int itemStatus;
    public RPGMoney(String moneyName, Item moneyItem) {
        this(moneyName, moneyItem, 0);
    }

    public RPGMoney(String moneyName, Item moneyItem, int itemStatus) {

        this.moneyName = moneyName;
        this.moneyItem = moneyItem;
        this.itemStatus = itemStatus;
    }

    public Item getMoneyItem() {
        return moneyItem;
    }

    public String getMoneyName() {
        return moneyName;
    }

    public int getItemStatus() {
        return itemStatus;
    }
}
