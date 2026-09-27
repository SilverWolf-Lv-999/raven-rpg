package keystrokesmod.module.impl.world;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.ui.trade.TradeEntry;
import keystrokesmod.ui.trade.TradeManager;
import keystrokesmod.ui.trade.VTScreen;
import keystrokesmod.utility.RPGUIUtility;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VillagerTrade extends Module {
    public static final VTScreen INSTANCE = new VTScreen();
    public static final SliderSetting range = new SliderSetting("Range", 3.0, 1.0, 6.0, 0.1);
    public static final ButtonSetting silent = new ButtonSetting("Silent", true);
    public static final ButtonSetting openTradeHandler = new ButtonSetting("Open trade manager", () -> mc.displayGuiScreen(INSTANCE));
    public static final ButtonSetting getOnSoulSpace = new ButtonSetting("Get on soul space", true);
    public static final ButtonSetting crosshairNPC = new ButtonSetting("Crosshair NPC", true);

    private GuiMerchant merchantGui;
    private TradeEntry activeTrade;
    private ItemStack pendingSoulStack;
    private int pendingSoulAmount;
    private int interactionDelay;
    private int actionTicks;
    private int actionWindowId = -1;
    private int actionBeforeCount;
    private int actionBeforeResultCount;
    private int actionExpectedInputCount;
    private int actionExpectedResultCount;
    private int batchTradesRemaining;
    private int soulBeforeCount;
    private int soulStateTicks;
    private Container soulContainer;
    private int soulWindowId = -1;
    private SoulState soulState = SoulState.IDLE;
    private boolean soulGuiSignal;
    private boolean waitingTradeRefresh;
    private boolean waitingMerchantOpen;
    private boolean merchantOpenedByModule;
    private int merchantOpenTicks;
    private final Set<Integer> rejectedEntityIds = new HashSet<>();
    private final Set<BlockPos> rejectedSignPositions = new HashSet<>();
    private int activeEntityId = -1;
    private BlockPos activeSignPosition;
    private int idleTicks;

    public VillagerTrade() {
        super("Village Trade", category.world);
        this.registerSetting(range);
        this.registerSetting(silent);
        this.registerSetting(openTradeHandler);
        this.registerSetting(getOnSoulSpace);
        this.registerSetting(crosshairNPC);
    }

    @Override
    public void onEnable() {
        this.clearState();
    }

    @Override
    public void onDisable() {
        this.clearState();
        if (mc.thePlayer != null && mc.thePlayer.openContainer instanceof ContainerMerchant) {
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            this.clearState();
            return;
        }
        if (getOnSoulSpace.isToggled()
                && this.soulState == SoulState.IDLE
                && this.isSoulContainer(mc.thePlayer.openContainer)) {
            ItemStack manualSoulStack = this.findManualSoulStack(mc.thePlayer.openContainer);
            if (manualSoulStack != null) {
                this.beginSoulExtraction(manualSoulStack);
                return;
            }
        }
        if (this.soulState != SoulState.IDLE) {
            this.handleSoulSpace();
            return;
        }
        if (this.handleMerchant()) {
            return;
        }
        if (this.waitingMerchantOpen) {
            if (mc.thePlayer.openContainer instanceof ContainerMerchant && this.merchantGui != null) {
                this.waitingMerchantOpen = false;
                this.merchantOpenTicks = 0;
            } else if (++this.merchantOpenTicks <= 3) {
                return;
            } else {
                if (this.activeEntityId >= 0) {
                    this.rejectedEntityIds.add(this.activeEntityId);
                }
                if (this.activeSignPosition != null) {
                    this.rejectedSignPositions.add(this.activeSignPosition);
                }
                this.activeEntityId = -1;
                this.activeSignPosition = null;
                this.waitingMerchantOpen = false;
                this.merchantOpenTicks = 0;
            }
        }
        if (this.interactionDelay > 0) {
            this.interactionDelay--;
            return;
        }
        this.interactWithNearestTarget();
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!this.isEnabled() || event.gui == null || mc.thePlayer == null) {
            return;
        }
        if (event.gui instanceof GuiMerchant) {
            this.merchantGui = (GuiMerchant) event.gui;
            this.waitingMerchantOpen = false;
            this.merchantOpenTicks = 0;
            if (silent.isToggled() && this.merchantOpenedByModule) {
                mc.thePlayer.openContainer = this.merchantGui.inventorySlots;
                event.setCanceled(true);
            }
            return;
        }
        if (this.soulState != SoulState.IDLE
                && event.gui instanceof GuiContainer
                && !(event.gui instanceof GuiInventory)) {
            Container container = ((GuiContainer) event.gui).inventorySlots;
            mc.thePlayer.openContainer = container;
            if (this.isSoulContainer(container)
                    && (this.soulState == SoulState.WAITING_OPEN
                    || this.soulState == SoulState.WAITING_REFRESH)) {
                this.soulGuiSignal = true;
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.clearState();
    }

    private boolean handleMerchant() {
        if (!(mc.thePlayer.openContainer instanceof ContainerMerchant)) {
            return false;
        }
        ContainerMerchant containerMerchant = (ContainerMerchant) mc.thePlayer.openContainer;
        if (this.merchantGui == null || this.merchantGui.inventorySlots != containerMerchant) {
            if (mc.currentScreen instanceof GuiMerchant
                    && ((GuiMerchant) mc.currentScreen).inventorySlots == containerMerchant) {
                this.merchantGui = (GuiMerchant) mc.currentScreen;
            } else {
                return false;
            }
        }
        this.waitingMerchantOpen = false;
        this.merchantOpenTicks = 0;
        if (this.waitingTradeRefresh) {
            int currentInputCount = this.getTradeItemCount(this.activeTrade);
            int currentResultCount = TradeManager.count(
                    this.activeTrade.getItemToSell(),
                    mc.thePlayer.inventory.mainInventory
            );
            if (currentInputCount <= this.actionExpectedInputCount
                    && currentResultCount >= this.actionExpectedResultCount) {
                this.waitingTradeRefresh = false;
                this.actionTicks = 0;
                if (this.batchTradesRemaining > 0) {
                    this.batchTradesRemaining--;
                }
            } else if (++this.actionTicks < 40) {
                return true;
            } else {
                this.waitingTradeRefresh = false;
                this.actionTicks = 0;
                this.interactionDelay = 5;
                return true;
            }
        }

        MerchantRecipeList recipes = this.merchantGui.getMerchant().getRecipes(mc.thePlayer);
        if (recipes == null || recipes.isEmpty()) {
            this.interactionDelay = 1;
            return true;
        }
        TradeEntry nextTrade = this.findTrade(recipes);
        if (nextTrade == null) {
            if (++this.actionTicks < 40) {
                this.interactionDelay = 1;
                return true;
            }
            this.finishMerchant();
            return false;
        }
        MerchantRecipe recipe = this.findRecipe(recipes, nextTrade);
        if (recipe == null || recipe.isRecipeDisabled() || recipe.getToolUses() >= recipe.getMaxTradeUses()) {
            this.finishMerchant();
            return false;
        }

        int batchTrades = this.batchTradesRemaining > 0
                ? 1
                : this.getOutputBatchSize(recipe);
        if (batchTrades <= 0) {
            this.interactionDelay = 5;
            return true;
        }
        ItemStack missingStack = this.findMissingStack(recipe, batchTrades);
        if (missingStack != null) {
            if (this.batchTradesRemaining > 0) {
                this.batchTradesRemaining = 0;
                batchTrades = this.getOutputBatchSize(recipe);
                missingStack = this.findMissingStack(recipe, batchTrades);
            }
            if (missingStack != null) {
                if (getOnSoulSpace.isToggled() && this.beginSoulExtraction(missingStack)) {
                    return true;
                }
                batchTrades = this.getAvailableInputBatchSize(recipe, batchTrades);
                if (batchTrades <= 0) {
                    this.finishMerchant();
                    return false;
                }
                missingStack = this.findMissingStack(recipe, batchTrades);
                if (missingStack != null) {
                    this.finishMerchant();
                    return false;
                }
            }
        }

        int recipeIndex = recipes.indexOf(recipe);
        RPGUIUtility.selectMerchantTrade(this.merchantGui, recipeIndex);
        int completedTrades = RPGUIUtility.completeMerchantTrades(this.merchantGui, recipe, 1);
        if (completedTrades <= 0) {
            this.interactionDelay = 1;
            return true;
        }
        if (this.batchTradesRemaining <= 0) {
            this.batchTradesRemaining = batchTrades;
        }
        this.activeTrade = nextTrade;
        this.actionWindowId = containerMerchant.windowId;
        this.actionBeforeCount = this.getTradeItemCount(nextTrade);
        this.actionBeforeResultCount = TradeManager.count(
                recipe.getItemToSell(),
                mc.thePlayer.inventory.mainInventory
        );
        int inputPerTrade = recipe.getItemToBuy().stackSize;
        if (recipe.getSecondItemToBuy() != null) {
            inputPerTrade += recipe.getSecondItemToBuy().stackSize;
        }
        this.actionExpectedInputCount = this.actionBeforeCount - inputPerTrade * completedTrades;
        this.actionExpectedResultCount = this.actionBeforeResultCount
                + recipe.getItemToSell().stackSize * completedTrades;
        this.actionTicks = 0;
        this.waitingTradeRefresh = true;
        return true;
    }

    private void handleSoulSpace() {
        if (++this.soulStateTicks > 200) {
            this.finishSoulSpace();
            return;
        }
        if (this.soulState == SoulState.PREPARE) {
            if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) {
                mc.thePlayer.closeScreen();
                return;
            }
            int soulIndex = this.findSoulIndex();
            if (soulIndex < 0) {
                return;
            }
            if (!this.moveToMainHand(soulIndex)) {
                return;
            }
            ItemStack heldStack = mc.thePlayer.getHeldItem();
            if (!this.isSoulStack(heldStack)) {
                return;
            }
            this.soulContainer = mc.thePlayer.openContainer;
            this.soulWindowId = this.soulContainer.windowId;
            this.actionTicks = 0;
            this.soulStateTicks = 0;
            this.soulGuiSignal = false;
            this.soulState = SoulState.WAITING_OPEN;
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldStack);
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
            return;
        }
        if (this.soulState == SoulState.WAITING_OPEN) {
            if (this.isNewSoulContainer(mc.thePlayer.openContainer)
                    || this.soulGuiSignal) {
                this.soulContainer = mc.thePlayer.openContainer;
                this.soulWindowId = this.soulContainer.windowId;
                this.soulState = SoulState.EXTRACTING;
                this.actionTicks = 0;
                this.soulStateTicks = 0;
                this.soulGuiSignal = false;
                return;
            }
            if (++this.actionTicks >= 40) {
                this.soulState = SoulState.PREPARE;
                this.actionTicks = 0;
            }
            return;
        }
        if (this.soulState == SoulState.EXTRACTING) {
            if (!this.isSoulContainer(mc.thePlayer.openContainer)) {
                if (++this.actionTicks > 40) {
                    this.finishSoulSpace();
                    return;
                }
                this.soulState = SoulState.WAITING_OPEN;
                this.actionTicks = 0;
                return;
            }
            Slot soulSlot = this.findSoulSlot(mc.thePlayer.openContainer, this.pendingSoulStack);
            if (soulSlot == null) {
                if (++this.actionTicks > 40) {
                    this.finishSoulSpace();
                    return;
                }
                return;
            }
            this.soulBeforeCount = TradeManager.count(this.pendingSoulStack, mc.thePlayer.inventory.mainInventory);
            int amount = TradeManager.bestSoulAmount(
                    this.pendingSoulStack,
                    this.pendingSoulAmount,
                    mc.thePlayer.inventory.mainInventory,
                    soulSlot.getStack().stackSize
            );
            int emptySlots = 0;
            for (ItemStack stack : mc.thePlayer.inventory.mainInventory) {
                if (stack == null) {
                    emptySlots++;
                }
            }
            if (emptySlots > 1) {
                amount = Math.min(
                        amount,
                        Math.max(
                                0,
                                TradeManager.capacity(
                                        this.pendingSoulStack,
                                        mc.thePlayer.inventory.mainInventory
                                ) - Math.max(1, this.pendingSoulStack.getMaxStackSize())
                        )
                );
            }
            if (amount <= 0) {
                this.actionTicks = 0;
                return;
            }
            if (!this.extractSoulStack(
                    mc.thePlayer.openContainer,
                    soulSlot,
                    amount >= Math.max(1, this.pendingSoulStack.getMaxStackSize())
            )) {
                this.finishSoulSpace();
                return;
            }
            this.soulContainer = mc.thePlayer.openContainer;
            this.soulWindowId = this.soulContainer.windowId;
            this.actionTicks = 0;
            this.soulStateTicks = 0;
            this.soulGuiSignal = false;
            this.soulState = SoulState.WAITING_REFRESH;
            return;
        }
        int currentCount = TradeManager.count(this.pendingSoulStack, mc.thePlayer.inventory.mainInventory);
        if (currentCount >= this.pendingSoulAmount) {
            this.finishSoulSpace();
            return;
        }
        if (++this.actionTicks > 40) {
            this.finishSoulSpace();
            return;
        }
        if (this.isNewSoulContainer(mc.thePlayer.openContainer) || this.soulGuiSignal) {
            this.soulContainer = mc.thePlayer.openContainer;
            this.soulWindowId = this.soulContainer.windowId;
            this.soulGuiSignal = false;
            this.actionTicks = 0;
        }
        if (currentCount > this.soulBeforeCount) {
            this.soulState = SoulState.EXTRACTING;
            this.actionTicks = 0;
            this.soulStateTicks = 0;
        } else if (this.soulContainer == mc.thePlayer.openContainer && this.actionTicks > 5) {
            this.finishSoulSpace();
        }
    }

    private boolean beginSoulExtraction(ItemStack missingStack) {
        this.pendingSoulStack = missingStack.copy();
        this.pendingSoulAmount = missingStack.stackSize;
        if (TradeManager.count(missingStack, mc.thePlayer.inventory.mainInventory) >= this.pendingSoulAmount) {
            return false;
        }
        if (this.isSoulContainer(mc.thePlayer.openContainer)) {
            this.soulContainer = mc.thePlayer.openContainer;
            this.soulWindowId = this.soulContainer.windowId;
            this.soulState = SoulState.EXTRACTING;
            this.actionTicks = 0;
            this.soulStateTicks = 0;
            this.soulGuiSignal = false;
            this.merchantGui = null;
            return true;
        }
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) {
            mc.thePlayer.closeScreen();
        }
        this.merchantGui = null;
        this.soulState = SoulState.PREPARE;
        this.actionTicks = 0;
        return true;
    }

    private boolean extractSoulStack(Container container, Slot sourceSlot, boolean wholeStack) {
        if (container == null || sourceSlot == null || sourceSlot.getStack() == null
                || mc.thePlayer.inventory.getItemStack() != null) {
            return false;
        }
        mc.playerController.windowClick(
                container.windowId,
                sourceSlot.slotNumber,
                0,
                wholeStack ? 1 : 0,
                mc.thePlayer
        );
        return true;
    }

    private void interactWithNearestTarget() {
        if (crosshairNPC.isToggled()) {
            if (mc.objectMouseOver == null
                    || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                    || !(mc.objectMouseOver.entityHit instanceof EntityLivingBase)) {
                this.interactionDelay = 2;
                return;
            }
            EntityLivingBase target = (EntityLivingBase) mc.objectMouseOver.entityHit;
            double configuredRange = range.getInput();
            if (target == mc.thePlayer
                    || target.isDead
                    || this.rejectedEntityIds.contains(target.getEntityId())
                    || mc.thePlayer.getDistanceSqToEntity(target) > configuredRange * configuredRange) {
                this.interactionDelay = 2;
                return;
            }
            mc.getNetHandler().addToSendQueue(
                    new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT)
            );
            this.interactionDelay = 1;
            this.waitingMerchantOpen = true;
            this.merchantOpenTicks = 0;
            this.activeEntityId = target.getEntityId();
            this.activeSignPosition = null;
            this.merchantOpenedByModule = true;
            this.idleTicks = 0;
            return;
        }
        EntityLivingBase nearestEntity = null;
        double nearestDistance = Double.MAX_VALUE;
        double configuredRange = range.getInput();
        for (EntityLivingBase entity : mc.theWorld.getEntitiesWithinAABB(
                EntityLivingBase.class,
                mc.thePlayer.getEntityBoundingBox().expand(configuredRange, configuredRange, configuredRange)
        )) {
            if (entity == mc.thePlayer || entity.isDead || this.rejectedEntityIds.contains(entity.getEntityId())) {
                continue;
            }
            double distance = mc.thePlayer.getDistanceSqToEntity(entity);
            if (distance < nearestDistance) {
                nearestEntity = entity;
                nearestDistance = distance;
            }
        }
        if (nearestEntity != null) {
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(nearestEntity, C02PacketUseEntity.Action.INTERACT));
            this.interactionDelay = 1;
            this.waitingMerchantOpen = true;
            this.merchantOpenTicks = 0;
            this.activeEntityId = nearestEntity.getEntityId();
            this.activeSignPosition = null;
            this.merchantOpenedByModule = true;
            this.idleTicks = 0;
            return;
        }

        TileEntitySign nearestSign = null;
        nearestDistance = Double.MAX_VALUE;
        double rangeSquared = configuredRange * configuredRange;
        int minX = (int) Math.floor(mc.thePlayer.posX - configuredRange);
        int maxX = (int) Math.floor(mc.thePlayer.posX + configuredRange);
        int minY = (int) Math.floor(mc.thePlayer.posY - configuredRange);
        int maxY = (int) Math.floor(mc.thePlayer.posY + configuredRange);
        int minZ = (int) Math.floor(mc.thePlayer.posZ - configuredRange);
        int maxZ = (int) Math.floor(mc.thePlayer.posZ + configuredRange);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (this.rejectedSignPositions.contains(blockPos)) {
                        continue;
                    }
                    TileEntity tileEntity = mc.theWorld.getTileEntity(blockPos);
                    if (!(tileEntity instanceof TileEntitySign)) {
                        continue;
                    }
                    double distance = mc.thePlayer.getDistanceSq(
                            blockPos.getX() + 0.5D,
                            blockPos.getY() + 0.5D,
                            blockPos.getZ() + 0.5D
                    );
                    if (distance <= rangeSquared && distance < nearestDistance) {
                        nearestSign = (TileEntitySign) tileEntity;
                        nearestDistance = distance;
                    }
                }
            }
        }
        if (nearestSign != null) {
            ItemStack heldStack = mc.thePlayer.getHeldItem();
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                    nearestSign.getPos(),
                    EnumFacing.UP.getIndex(),
                    heldStack == null ? null : heldStack.copy(),
                    0.5F,
                    1.0F,
                    0.5F
            ));
            this.interactionDelay = 1;
            this.waitingMerchantOpen = true;
            this.merchantOpenTicks = 0;
            this.activeEntityId = -1;
            this.activeSignPosition = nearestSign.getPos();
            this.merchantOpenedByModule = true;
            this.idleTicks = 0;
        } else {
            if (++this.idleTicks > 100) {
                this.rejectedEntityIds.clear();
                this.rejectedSignPositions.clear();
                this.idleTicks = 0;
            }
            this.interactionDelay = 5;
        }
    }

    private TradeEntry findTrade(MerchantRecipeList recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return null;
        }
        List<TradeEntry> trades = TradeManager.getTrades();
        for (TradeEntry trade : trades) {
            MerchantRecipe recipe = this.findRecipe(recipes, trade);
            if (recipe != null && !recipe.isRecipeDisabled() && recipe.getToolUses() < recipe.getMaxTradeUses()) {
                return trade;
            }
        }
        return null;
    }

    private MerchantRecipe findRecipe(MerchantRecipeList recipes, TradeEntry trade) {
        if (recipes == null || trade == null) {
            return null;
        }
        for (MerchantRecipe recipe : recipes) {
            if (trade.matches(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private ItemStack findMissingStack(MerchantRecipe recipe, int tradeCount) {
        ItemStack first = recipe.getItemToBuy();
        ItemStack second = recipe.getSecondItemToBuy();
        if (second != null && this.matches(first, second)) {
            int required = (first.stackSize + second.stackSize) * tradeCount;
            int missing = required - TradeManager.count(first, mc.thePlayer.inventory.mainInventory);
            if (missing > 0) {
                ItemStack result = first.copy();
                result.stackSize = missing;
                return result;
            }
            return null;
        }
        int firstRequired = first.stackSize * tradeCount;
        int firstMissing = firstRequired - TradeManager.count(first, mc.thePlayer.inventory.mainInventory);
        if (firstMissing > 0) {
            ItemStack missing = first.copy();
            missing.stackSize = firstMissing;
            return missing;
        }
        if (second != null) {
            int secondRequired = second.stackSize * tradeCount;
            int secondMissing = secondRequired - TradeManager.count(second, mc.thePlayer.inventory.mainInventory);
            if (secondMissing > 0) {
                ItemStack missing = second.copy();
                missing.stackSize = secondMissing;
                return missing;
            }
        }
        return null;
    }

    private int getOutputBatchSize(MerchantRecipe recipe) {
        ItemStack result = recipe.getItemToSell();
        if (result == null || result.stackSize <= 0) {
            return 0;
        }
        int availableTrades = Math.max(0, recipe.getMaxTradeUses() - recipe.getToolUses());
        int lowerBound = 0;
        while (lowerBound < availableTrades) {
            int candidate = lowerBound + (availableTrades - lowerBound + 1) / 2;
            if (this.canFitTradeBatch(recipe, candidate, true)) {
                lowerBound = candidate;
            } else {
                availableTrades = candidate - 1;
            }
        }
        if (lowerBound == 0) {
            availableTrades = Math.max(0, recipe.getMaxTradeUses() - recipe.getToolUses());
            while (lowerBound < availableTrades) {
                int candidate = lowerBound + (availableTrades - lowerBound + 1) / 2;
                if (this.canFitTradeBatch(recipe, candidate, false)) {
                    lowerBound = candidate;
                } else {
                    availableTrades = candidate - 1;
                }
            }
        }
        return lowerBound;
    }

    private boolean canFitTradeBatch(MerchantRecipe recipe, int tradeCount, boolean reserveSlot) {
        ItemStack[] inventory = new ItemStack[mc.thePlayer.inventory.mainInventory.length];
        for (int index = 0; index < inventory.length; index++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[index];
            inventory[index] = stack == null ? null : stack.copy();
        }
        ItemStack firstItem = recipe.getItemToBuy();
        ItemStack secondItem = recipe.getSecondItemToBuy();
        if (secondItem != null && this.matches(firstItem, secondItem)) {
            int required = (firstItem.stackSize + secondItem.stackSize) * tradeCount;
            int available = TradeManager.count(firstItem, inventory);
            if (!this.addInventoryItem(
                    inventory,
                    firstItem,
                    Math.max(0, required - available),
                    reserveSlot
            )) {
                return false;
            }
            this.consumeInventoryItem(inventory, firstItem, required);
        } else {
            int firstRequired = firstItem.stackSize * tradeCount;
            int firstAvailable = TradeManager.count(firstItem, inventory);
            if (!this.addInventoryItem(
                    inventory,
                    firstItem,
                    Math.max(0, firstRequired - firstAvailable),
                    reserveSlot
            )) {
                return false;
            }
            if (secondItem != null) {
                int secondRequired = secondItem.stackSize * tradeCount;
                int secondAvailable = TradeManager.count(secondItem, inventory);
                if (!this.addInventoryItem(
                        inventory,
                        secondItem,
                        Math.max(0, secondRequired - secondAvailable),
                        reserveSlot
                )) {
                    return false;
                }
            }
            this.consumeInventoryItem(inventory, firstItem, firstRequired);
            if (secondItem != null) {
                this.consumeInventoryItem(
                        inventory,
                        secondItem,
                        secondItem.stackSize * tradeCount
                );
            }
        }
        boolean hasEmptySlot = false;
        for (ItemStack stack : inventory) {
            if (stack == null) {
                hasEmptySlot = true;
                break;
            }
        }
        if (!hasEmptySlot && reserveSlot) {
            return false;
        }
        int outputCapacity = TradeManager.capacity(recipe.getItemToSell(), inventory);
        if (reserveSlot) {
            outputCapacity -= Math.max(1, recipe.getItemToSell().getMaxStackSize());
        }
        return outputCapacity >= recipe.getItemToSell().stackSize * tradeCount;
    }

    private boolean addInventoryItem(ItemStack[] inventory, ItemStack requiredItem, int amount, boolean reserveSlot) {
        if (requiredItem == null || amount <= 0) {
            return true;
        }
        int remaining = amount;
        int emptySlots = 0;
        int maxStackSize = Math.max(1, requiredItem.getMaxStackSize());
        for (ItemStack stack : inventory) {
            if (stack == null) {
                emptySlots++;
                continue;
            }
            if (!this.matches(requiredItem, stack)) {
                continue;
            }
            int capacity = Math.max(0, Math.min(maxStackSize, stack.getMaxStackSize()) - stack.stackSize);
            int added = Math.min(remaining, capacity);
            stack.stackSize += added;
            remaining -= added;
            if (remaining <= 0) {
                return true;
            }
        }
        for (int index = 0; index < inventory.length && remaining > 0; index++) {
            if (inventory[index] != null || (reserveSlot && emptySlots <= 1)) {
                continue;
            }
            int added = Math.min(remaining, maxStackSize);
            ItemStack addedStack = requiredItem.copy();
            addedStack.stackSize = added;
            inventory[index] = addedStack;
            emptySlots--;
            remaining -= added;
        }
        return remaining <= 0;
    }

    private void consumeInventoryItem(ItemStack[] inventory, ItemStack requiredItem, int amount) {
        if (requiredItem == null || amount <= 0) {
            return;
        }
        int remaining = amount;
        for (int index = 0; index < inventory.length && remaining > 0; index++) {
            ItemStack stack = inventory[index];
            if (!this.matches(requiredItem, stack)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.stackSize);
            stack.stackSize -= consumed;
            remaining -= consumed;
            if (stack.stackSize <= 0) {
                inventory[index] = null;
            }
        }
    }

    private int getAvailableInputBatchSize(MerchantRecipe recipe, int limit) {
        int availableTrades = limit;
        ItemStack first = recipe.getItemToBuy();
        if (first == null || first.stackSize <= 0) {
            return 0;
        }
        ItemStack second = recipe.getSecondItemToBuy();
        if (second != null && this.matches(first, second)) {
            return Math.min(
                    availableTrades,
                    TradeManager.count(first, mc.thePlayer.inventory.mainInventory)
                            / (first.stackSize + second.stackSize)
            );
        }
        availableTrades = Math.min(
                availableTrades,
                TradeManager.count(first, mc.thePlayer.inventory.mainInventory) / first.stackSize
        );
        if (second != null) {
            if (second.stackSize <= 0) {
                return 0;
            }
            availableTrades = Math.min(
                    availableTrades,
                    TradeManager.count(second, mc.thePlayer.inventory.mainInventory) / second.stackSize
            );
        }
        return availableTrades;
    }

    private int getTradeItemCount(TradeEntry trade) {
        if (trade == null) {
            return 0;
        }
        int count = TradeManager.count(trade.getItemToBuy(), mc.thePlayer.inventory.mainInventory);
        ItemStack second = trade.getSecondItemToBuy();
        return second == null || this.matches(trade.getItemToBuy(), second)
                ? count
                : count + TradeManager.count(second, mc.thePlayer.inventory.mainInventory);
    }

    private Slot findSoulSlot(Container container, ItemStack requiredStack) {
        if (container == null || requiredStack == null) {
            return null;
        }
        for (Object object : container.inventorySlots) {
            Slot slot = (Slot) object;
            if (slot.inventory != mc.thePlayer.inventory && this.matchesSoulItem(requiredStack, slot.getStack())) {
                return slot;
            }
        }
        return null;
    }

    private ItemStack findManualSoulStack(Container container) {
        for (TradeEntry trade : TradeManager.getTrades()) {
            ItemStack first = trade.getItemToBuy();
            ItemStack second = trade.getSecondItemToBuy();
            if (second != null && this.matches(first, second)) {
                int required = first.stackSize + second.stackSize;
                int current = TradeManager.count(first, mc.thePlayer.inventory.mainInventory);
                if (current < required && this.findSoulSlot(container, first) != null) {
                    ItemStack missing = first.copy();
                    missing.stackSize = required - current;
                    return missing;
                }
                continue;
            }
            if (TradeManager.count(first, mc.thePlayer.inventory.mainInventory) < first.stackSize
                    && this.findSoulSlot(container, first) != null) {
                return first;
            }
            if (second != null
                    && TradeManager.count(second, mc.thePlayer.inventory.mainInventory) < second.stackSize
                    && this.findSoulSlot(container, second) != null) {
                return second;
            }
        }
        return null;
    }

    private int findSoulIndex() {
        for (int index = 0; index < mc.thePlayer.inventory.mainInventory.length; index++) {
            if (this.isSoulStack(mc.thePlayer.inventory.mainInventory[index])) {
                return index;
            }
        }
        return -1;
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
        for (Object object : container.inventorySlots) {
            Slot slot = (Slot) object;
            if (slot.inventory == mc.thePlayer.inventory && slot.getSlotIndex() == inventoryIndex) {
                mc.playerController.windowClick(container.windowId, slot.slotNumber, currentItem, 2, mc.thePlayer);
                return true;
            }
        }
        return false;
    }

    private boolean isSoulStack(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        String displayName = stack.getDisplayName();
        return displayName != null && displayName.contains("灵魂空间");
    }

    private boolean matches(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null && secondStack != null
                && ItemStack.areItemsEqual(firstStack, secondStack)
                && ItemStack.areItemStackTagsEqual(firstStack, secondStack);
    }

    private boolean matchesSoulItem(ItemStack firstStack, ItemStack secondStack) {
        return firstStack != null
                && secondStack != null
                && firstStack.getDisplayName().equals(secondStack.getDisplayName());
    }

    private void finishMerchant() {
        if (this.merchantOpenedByModule && mc.thePlayer.openContainer instanceof ContainerMerchant) {
            mc.thePlayer.closeScreen();
        }
        this.merchantGui = null;
        this.activeTrade = null;
        this.waitingTradeRefresh = false;
        this.batchTradesRemaining = 0;
        this.merchantOpenedByModule = false;
        this.interactionDelay = 1;
        this.waitingMerchantOpen = false;
        this.merchantOpenTicks = 0;
        if (this.activeEntityId >= 0) {
            this.rejectedEntityIds.add(this.activeEntityId);
        }
        if (this.activeSignPosition != null) {
            this.rejectedSignPositions.add(this.activeSignPosition);
        }
        this.activeEntityId = -1;
        this.activeSignPosition = null;
    }

    private void finishSoulSpace() {
        if (mc.thePlayer.openContainer != mc.thePlayer.inventoryContainer) {
            mc.thePlayer.closeScreen();
        }
        this.pendingSoulStack = null;
        this.pendingSoulAmount = 0;
        this.soulContainer = null;
        this.soulWindowId = -1;
        this.soulState = SoulState.IDLE;
        this.soulStateTicks = 0;
        this.soulGuiSignal = false;
        this.actionTicks = 0;
        this.merchantGui = null;
        this.interactionDelay = 0;
        this.waitingMerchantOpen = false;
        this.merchantOpenTicks = 0;
    }

    private void clearState() {
        this.merchantGui = null;
        this.activeTrade = null;
        this.pendingSoulStack = null;
        this.pendingSoulAmount = 0;
        this.interactionDelay = 0;
        this.actionTicks = 0;
        this.actionWindowId = -1;
        this.actionBeforeCount = 0;
        this.actionBeforeResultCount = 0;
        this.actionExpectedInputCount = 0;
        this.actionExpectedResultCount = 0;
        this.batchTradesRemaining = 0;
        this.soulBeforeCount = 0;
        this.soulContainer = null;
        this.soulWindowId = -1;
        this.soulState = SoulState.IDLE;
        this.soulStateTicks = 0;
        this.soulGuiSignal = false;
        this.waitingTradeRefresh = false;
        this.waitingMerchantOpen = false;
        this.merchantOpenedByModule = false;
        this.merchantOpenTicks = 0;
        this.rejectedEntityIds.clear();
        this.rejectedSignPositions.clear();
        this.activeEntityId = -1;
        this.activeSignPosition = null;
        this.idleTicks = 0;
    }

    private boolean isNewSoulContainer(Container container) {
        return this.isSoulContainer(container)
                && (container != this.soulContainer || container.windowId != this.soulWindowId);
    }

    private boolean isSoulContainer(Container container) {
        return container != null
                && container != mc.thePlayer.inventoryContainer
                && !(container instanceof ContainerMerchant)
                && this.hasExternalSlot(container);
    }

    private boolean hasExternalSlot(Container container) {
        for (Object object : container.inventorySlots) {
            if (((Slot) object).inventory != mc.thePlayer.inventory) {
                return true;
            }
        }
        return false;
    }

    private enum SoulState {
        IDLE,
        PREPARE,
        WAITING_OPEN,
        EXTRACTING,
        WAITING_REFRESH
    }
}
