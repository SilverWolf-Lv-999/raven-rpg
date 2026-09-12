package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.rpg.RPGMoney;
import keystrokesmod.utility.rpg.RPGUtility;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoShell extends Module {
    private ItemStack pendingStack;
    private int pendingTicks;
    private int pendingStackSize;
    private boolean commandSent;

    public final SliderSetting delay = new SliderSetting("send cmd delay", 0.0, 0.0, 20.0, 1.0);
    public final ButtonSetting item = new ButtonSetting("item", true);
    public final ButtonSetting money = new ButtonSetting("money", false);
    public final ButtonSetting silentMode = new ButtonSetting("silent", false);

    private MoneyState moneyState = MoneyState.IDLE;
    private Container moneyContainer;
    private int moneyWindowId = -1;
    private RPGMoney extractedMoney;
    private RPGMoney saleMoney;
    private BlockPos saleSign;
    private int extractBeforeCount;
    private int saleBeforeCount;
    private int moneyStateTicks;
    private boolean shellMayContainMoney;
    private boolean salePromptSeen;
    private boolean saleResultSeen;
    private boolean saleInventoryChanged;

    public AutoShell() {
        super("Auto Shell", category.player);
        this.registerSetting(delay);
        this.registerSetting(item);
        this.registerSetting(money);
        this.registerSetting(silentMode);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        closeMoneyContainer();
        resetState();
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            closeMoneyContainer();
            resetState();
            return;
        }

        if (money.isToggled()) {
            if (!RPGUtility.isOnTradingMarket(mc.thePlayer)) {
                closeMoneyContainer();
                resetMoneyState();
                return;
            }
            handleMoney();
            return;
        }

        closeMoneyContainer();
        resetMoneyState();
        if (item.isToggled()) {
            handleItem();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!money.isToggled() || !Utils.nullCheck()) {
            return;
        }

        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (text == null) {
            return;
        }

        if (moneyState == MoneyState.WAITING_SALE_PROMPT
                && text.contains("输入")
                && text.contains("出售")
                && text.contains("数量")) {
            int count = getPlayerMoneyCount(saleMoney);
            if (count <= 0) {
                return;
            }

            saleBeforeCount = count;
            salePromptSeen = true;
            mc.thePlayer.sendChatMessage(String.valueOf(count));
            moneyState = MoneyState.WAITING_SALE_RESULT;
            moneyStateTicks = 0;
            return;
        }

        if (moneyState == MoneyState.WAITING_SALE_RESULT && text.contains("已售出")) {
            saleResultSeen = true;
            if (getPlayerMoneyCount(saleMoney) < saleBeforeCount) {
                saleInventoryChanged = true;
            }
            completeSaleIfReady();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!money.isToggled()
                || !silentMode.isToggled()
                || event.gui == null
                || mc.thePlayer == null
                || !isOpeningMoneyContainer()) {
            return;
        }

        if (event.gui instanceof GuiContainer && !(event.gui instanceof GuiInventory)) {
            GuiContainer gui = (GuiContainer) event.gui;
            mc.thePlayer.openContainer = gui.inventorySlots;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        resetState();
    }

    private void handleItem() {
        if (pendingStack != null) {
            if (!commandSent) {
                for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
                    ItemStack hotbarStack = mc.thePlayer.inventory.mainInventory[hotbarIndex];
                    if (!isSameStack(hotbarStack, pendingStack)) {
                        continue;
                    }
                    if (hotbarIndex != mc.thePlayer.inventory.currentItem) {
                        mc.thePlayer.inventory.currentItem = hotbarIndex;
                        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                    }
                    break;
                }

                if (++pendingTicks <= (int) delay.getInput()) {
                    return;
                }

                mc.thePlayer.sendChatMessage("/ils shell");
                commandSent = true;
                pendingTicks = 0;
                return;
            }

            ItemStack heldStack = mc.thePlayer.getHeldItem();
            if (heldStack != null && isSameStack(heldStack, pendingStack) && heldStack.stackSize >= pendingStackSize) {
                return;
            }

            resetItemState();
        }

        for (int inventoryIndex = 0; inventoryIndex < mc.thePlayer.inventory.mainInventory.length; inventoryIndex++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[inventoryIndex];
            if (stack == null || !containsSalePrice(stack)) {
                continue;
            }

            ItemStack targetStack = stack.copy();
            if (!moveToMainHand(inventoryIndex)) {
                continue;
            }

            pendingStack = targetStack;
            pendingTicks = 0;
            pendingStackSize = targetStack.stackSize;
            commandSent = false;
            return;
        }

        this.disable();
    }

    private void handleMoney() {
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer
                && moneyState == MoneyState.IDLE) {
            closeMoneyContainer();
            return;
        }

        if (moneyState != MoneyState.IDLE && ++moneyStateTicks > 200) {
            this.disable();
            return;
        }

        if (moneyState == MoneyState.IDLE) {
            if (shellMayContainMoney) {
                if (openMoneyShell()) {
                    return;
                }
                shellMayContainMoney = false;
            }

            RPGMoney nextMoney = findPlayerMoney();
            if (nextMoney != null) {
                beginSale(nextMoney);
                return;
            }

            this.disable();
            return;
        }

        if (moneyState == MoneyState.WAITING_SHELL_OPEN) {
            Container container = mc.thePlayer.openContainer;
            if (isNewMoneyContainer(container)) {
                moneyContainer = container;
                moneyWindowId = container.windowId;
                moneyState = MoneyState.EXTRACTING;
                moneyStateTicks = 0;
            }
            return;
        }

        if (moneyState == MoneyState.EXTRACTING) {
            Slot moneySlot = findMoneySlot(moneyContainer);
            if (moneySlot == null) {
                shellMayContainMoney = false;
                startNextMoneyAction();
                return;
            }

            extractedMoney = getMoney(moneySlot.getStack());
            extractBeforeCount = getPlayerMoneyCount(extractedMoney);
            moneyContainer = mc.thePlayer.openContainer;
            moneyWindowId = moneyContainer.windowId;
            moneyState = MoneyState.WAITING_SHELL_REFRESH;
            moneyStateTicks = 0;
            mc.playerController.windowClick(moneyWindowId, moneySlot.slotNumber, 0, 1, mc.thePlayer);
            return;
        }

        if (moneyState == MoneyState.WAITING_SHELL_REFRESH) {
            Container container = mc.thePlayer.openContainer;
            if (!isNewMoneyContainer(container)) {
                return;
            }

            moneyContainer = container;
            moneyWindowId = container.windowId;
            if (getPlayerMoneyCount(extractedMoney) > extractBeforeCount) {
                moneyState = MoneyState.EXTRACTING;
                moneyStateTicks = 0;
                return;
            }

            shellMayContainMoney = true;
            startNextMoneyAction();
            return;
        }

        if (moneyState == MoneyState.WAITING_SALE_SIGN) {
            if (moneyStateTicks == 1 || moneyStateTicks % 20 == 0) {
                saleSign = findSaleSign(saleMoney);
                if (saleSign != null) {
                    clickSaleSign();
                }
            }
            return;
        }

        if (moneyState == MoneyState.WAITING_SALE_PROMPT) {
            return;
        }

        if (moneyState == MoneyState.WAITING_SALE_RESULT) {
            if (getPlayerMoneyCount(saleMoney) < saleBeforeCount) {
                saleInventoryChanged = true;
            }
            completeSaleIfReady();
        }
    }

    private void startNextMoneyAction() {
        closeMoneyContainer();
        RPGMoney nextMoney = findPlayerMoney();
        if (nextMoney != null) {
            beginSale(nextMoney);
            return;
        }

        if (shellMayContainMoney) {
            moneyState = MoneyState.IDLE;
            moneyStateTicks = 0;
            return;
        }

        this.disable();
    }

    private void beginSale(RPGMoney targetMoney) {
        closeMoneyContainer();
        saleMoney = targetMoney;
        saleBeforeCount = getPlayerMoneyCount(targetMoney);
        salePromptSeen = false;
        saleResultSeen = false;
        saleInventoryChanged = false;
        saleSign = findSaleSign(targetMoney);
        moneyStateTicks = 0;

        if (saleSign == null) {
            moneyState = MoneyState.WAITING_SALE_SIGN;
            return;
        }

        clickSaleSign();
    }

    private void clickSaleSign() {
        mc.playerController.clickBlock(saleSign, EnumFacing.UP);
        moneyState = MoneyState.WAITING_SALE_PROMPT;
        moneyStateTicks = 0;
    }

    private void completeSaleIfReady() {
        if (moneyState != MoneyState.WAITING_SALE_RESULT
                || !salePromptSeen
                || !saleResultSeen
                || !saleInventoryChanged) {
            return;
        }

        closeMoneyContainer();
        saleMoney = null;
        saleSign = null;
        saleBeforeCount = 0;
        salePromptSeen = false;
        saleResultSeen = false;
        saleInventoryChanged = false;
        moneyState = MoneyState.IDLE;
        moneyStateTicks = 0;
        shellMayContainMoney = true;
    }

    private boolean openMoneyShell() {
        int inventoryIndex = findSoulIndex();
        if (inventoryIndex < 0 || !moveToMainHand(inventoryIndex)) {
            return false;
        }

        ItemStack heldStack = mc.thePlayer.getHeldItem();
        if (!isSoulStack(heldStack)) {
            return false;
        }

        moneyContainer = mc.thePlayer.openContainer;
        moneyWindowId = moneyContainer == null ? -1 : moneyContainer.windowId;
        moneyState = MoneyState.WAITING_SHELL_OPEN;
        moneyStateTicks = 0;
        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldStack);
        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        return true;
    }

    private Slot findMoneySlot(Container container) {
        if (!isMoneyContainer(container)) {
            return null;
        }

        for (Object slotObject : container.inventorySlots) {
            Slot slot = (Slot) slotObject;
            if (slot.inventory != mc.thePlayer.inventory && getMoney(slot.getStack()) != null) {
                return slot;
            }
        }
        return null;
    }

    private RPGMoney findPlayerMoney() {
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            RPGMoney money = getMoney(stack);
            if (money != null && stack.stackSize > 0) {
                return money;
            }
        }
        return null;
    }

    private int getPlayerMoneyCount(RPGMoney targetMoney) {
        if (targetMoney == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
            if (matchesMoney(stack, targetMoney)) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private RPGMoney getMoney(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        for (RPGMoney money : RPGUtility.RPG_MONEYS) {
            if (matchesMoney(stack, money)
                    && RPGUtility.isRPGMoney(keystrokesmod.script.model.ItemStack.convert(stack))) {
                return money;
            }
        }
        return null;
    }

    private boolean matchesMoney(ItemStack stack, RPGMoney money) {
        return stack != null
                && money != null
                && stack.getItem() == money.getMoneyItem()
                && stack.getMetadata() == money.getItemStatus();
    }

    private int findSoulIndex() {
        for (int index = 0; index < mc.thePlayer.inventory.mainInventory.length; index++) {
            if (isSoulStack(mc.thePlayer.inventory.mainInventory[index])) {
                return index;
            }
        }
        return -1;
    }

    private boolean isSoulStack(ItemStack stack) {
        return stack != null
                && stack.getItem() == Items.slime_ball
                && stack.getDisplayName().contains("灵魂空间");
    }

    private BlockPos findSaleSign(RPGMoney targetMoney) {
        String signItem = getSignItem(targetMoney);
        if (signItem == null) {
            return null;
        }

        double playerX = mc.thePlayer.posX;
        double playerY = mc.thePlayer.posY;
        double playerZ = mc.thePlayer.posZ;
        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntitySign)) {
                continue;
            }

            TileEntitySign sign = (TileEntitySign) tileEntity;
            double x = sign.getPos().getX() + 0.5D - playerX;
            double y = sign.getPos().getY() + 0.5D - playerY;
            double z = sign.getPos().getZ() + 0.5D - playerZ;
            if (x * x + y * y + z * z > 36.0D) {
                continue;
            }

            boolean systemShop = false;
            boolean buy = false;
            boolean item = false;
            for (int index = 0; index < sign.signText.length; index++) {
                if (sign.signText[index] == null) {
                    continue;
                }

                String text = sign.signText[index].getUnformattedText();
                systemShop |= text.contains("系统商店");
                buy |= text.contains("收购");
                item |= text.contains(signItem);
            }

            if (systemShop && buy && item) {
                return sign.getPos();
            }
        }
        return null;
    }

    private String getSignItem(RPGMoney targetMoney) {
        if (targetMoney == null) {
            return null;
        }

        String moneyName = targetMoney.getMoneyName();
        if ("铜币".equals(moneyName)) {
            return "红砖";
        }
        if ("银".equals(moneyName)) {
            return "铁锭";
        }
        if ("金".equals(moneyName)) {
            return "金锭";
        }
        if ("钻".equals(moneyName)) {
            return "钻石";
        }
        if ("绿宝石".equals(moneyName)) {
            return "绿宝石";
        }
        if ("蓝宝石".equals(moneyName)) {
            return "青金石";
        }
        if ("极品宝石".equals(moneyName)) {
            return "海晶砂粒";
        }
        return null;
    }

    private boolean isNewMoneyContainer(Container container) {
        return isMoneyContainer(container)
                && (container != moneyContainer || container.windowId != moneyWindowId);
    }

    private boolean isMoneyContainer(Container container) {
        if (container == null || container == mc.thePlayer.inventoryContainer) {
            return false;
        }

        for (Object slotObject : container.inventorySlots) {
            if (((Slot) slotObject).inventory != mc.thePlayer.inventory) {
                return true;
            }
        }
        return false;
    }

    private boolean isOpeningMoneyContainer() {
        return moneyState == MoneyState.WAITING_SHELL_OPEN
                || moneyState == MoneyState.EXTRACTING
                || moneyState == MoneyState.WAITING_SHELL_REFRESH;
    }

    private boolean containsSalePrice(ItemStack stack) {
        for (String tooltipLine : stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips)) {
            String text = EnumChatFormatting.getTextWithoutFormattingCodes(tooltipLine);
            if (text == null) {
                continue;
            }

            int saleStart = text.indexOf("售价");
            int coinStart = text.indexOf("金币", saleStart + 2);
            if (saleStart < 0 || coinStart < 0) {
                continue;
            }

            String price = text.substring(saleStart + 2, coinStart)
                    .replace(":", "")
                    .replace("：", "")
                    .replace(",", "")
                    .replace("，", "")
                    .trim();
            if (price.isEmpty()) {
                continue;
            }

            boolean numeric = true;
            for (int index = 0; index < price.length(); index++) {
                if (!Character.isDigit(price.charAt(index))) {
                    numeric = false;
                    break;
                }
            }
            if (numeric) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToMainHand(int inventoryIndex) {
        int currentItem = mc.thePlayer.inventory.currentItem;
        if (inventoryIndex < 9) {
            if (inventoryIndex != currentItem) {
                mc.thePlayer.inventory.currentItem = inventoryIndex;
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
            }
            return true;
        }

        Container container = mc.thePlayer.openContainer;
        if (container == null || container != mc.thePlayer.inventoryContainer) {
            return false;
        }

        Slot inventorySlot = null;
        for (Object slotObject : container.inventorySlots) {
            Slot slot = (Slot) slotObject;
            if (slot.inventory == mc.thePlayer.inventory && slot.getSlotIndex() == inventoryIndex) {
                inventorySlot = slot;
                break;
            }
        }
        if (inventorySlot == null || inventorySlot.getStack() == null) {
            return false;
        }

        mc.playerController.windowClick(container.windowId, inventorySlot.slotNumber, currentItem, 2, mc.thePlayer);
        return true;
    }

    private boolean isSameStack(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null
                && secondStack != null
                && firstStack.getItem() == secondStack.getItem()
                && firstStack.getMetadata() == secondStack.getMetadata();
    }

    private void closeMoneyContainer() {
        if (mc.thePlayer != null
                && mc.thePlayer.openContainer != null
                && mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer
                && (moneyState != MoneyState.IDLE
                || mc.thePlayer.openContainer == moneyContainer
                || money.isToggled())) {
            mc.thePlayer.closeScreen();
            moneyContainer = null;
            moneyWindowId = -1;
        }
    }

    private void resetState() {
        resetItemState();
        resetMoneyState();
    }

    private void resetItemState() {
        pendingStack = null;
        pendingTicks = 0;
        pendingStackSize = 0;
        commandSent = false;
    }

    private void resetMoneyState() {
        moneyState = MoneyState.IDLE;
        moneyContainer = null;
        moneyWindowId = -1;
        extractedMoney = null;
        saleMoney = null;
        saleSign = null;
        extractBeforeCount = 0;
        saleBeforeCount = 0;
        moneyStateTicks = 0;
        shellMayContainMoney = true;
        salePromptSeen = false;
        saleResultSeen = false;
        saleInventoryChanged = false;
    }

    private enum MoneyState {
        IDLE,
        WAITING_SHELL_OPEN,
        EXTRACTING,
        WAITING_SHELL_REFRESH,
        WAITING_SALE_SIGN,
        WAITING_SALE_PROMPT,
        WAITING_SALE_RESULT
    }
}
