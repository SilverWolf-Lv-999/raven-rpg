package keystrokesmod.utility.rpg;

import keystrokesmod.script.model.ItemStack;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RPGUtility {
    public static final String[] MONEY_NAME = {
            "铜币","银", "金", "钻", "绿宝石", "蓝宝石", "极品宝石"
    };

    public static final Set<RPGMoney> RPG_MONEYS;
    private static final Set<String> TRADING_MARKET_VILLAGER_NAMES = new HashSet<>(Arrays.asList("VIP", "超级VIP", "至尊VIP"));
    private static final Map<String, Vec3> TRADING_MARKET_VILLAGER_CACHE = new HashMap<>();
    private static World cachedTradingMarketWorld;
    private static int cachedTradingMarketVillagerTick = -1;
    private static int cachedTradingMarketSignTick = -1;
    private static boolean cachedTradingMarketSignFound;

    public static boolean isRPGMoney(ItemStack stack) {
        boolean hasMoneyTip = false;
        boolean hasFrom = false;
        List<String> tooltip = stack.getTooltip();
        for (String s : tooltip) {
            if (s.contains("货币"))
                hasMoneyTip = true;
            if (s.contains("来源") || s.contains("各种途径"))
                hasFrom = true;
        }
        return hasMoneyTip && hasFrom;
    }

    public static boolean isOnTradingMarket(EntityPlayerSP player) {
        if (!Utils.nullCheck() || player == null || player.worldObj == null) {
            TRADING_MARKET_VILLAGER_CACHE.clear();
            cachedTradingMarketWorld = null;
            cachedTradingMarketVillagerTick = -1;
            cachedTradingMarketSignTick = -1;
            cachedTradingMarketSignFound = false;
            return false;
        }

        if (cachedTradingMarketWorld != player.worldObj) {
            TRADING_MARKET_VILLAGER_CACHE.clear();
            cachedTradingMarketWorld = player.worldObj;
            cachedTradingMarketVillagerTick = -1;
            cachedTradingMarketSignTick = -1;
            cachedTradingMarketSignFound = false;
        }

        boolean villagersFound = TRADING_MARKET_VILLAGER_CACHE.size() == TRADING_MARKET_VILLAGER_NAMES.size();
        boolean scanDue = cachedTradingMarketVillagerTick < 0
                || player.ticksExisted - cachedTradingMarketVillagerTick >= 20
                || player.ticksExisted < cachedTradingMarketVillagerTick;
        if (!villagersFound && scanDue) {
            TRADING_MARKET_VILLAGER_CACHE.clear();
            for (EntityVillager villager : player.worldObj.getEntitiesWithinAABB(EntityVillager.class,
                    player.getEntityBoundingBox().expand(64.0D, 64.0D, 64.0D))) {
                IChatComponent displayName = villager.getDisplayName();
                if (displayName == null) {
                    continue;
                }

                String formattedName = displayName.getFormattedText();
                String firstColorCode = Utils.getFirstColorCode(formattedName);
                String villagerName = firstColorCode.isEmpty()
                        ? displayName.getUnformattedText()
                        : formattedName.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                if (!TRADING_MARKET_VILLAGER_NAMES.contains(villagerName)) {
                    continue;
                }

                TRADING_MARKET_VILLAGER_CACHE.put(villagerName, new Vec3(villager.posX, villager.posY, villager.posZ));
            }
            cachedTradingMarketVillagerTick = player.ticksExisted;
            villagersFound = TRADING_MARKET_VILLAGER_CACHE.size() == TRADING_MARKET_VILLAGER_NAMES.size();
            if (!villagersFound) {
                cachedTradingMarketSignTick = -1;
                cachedTradingMarketSignFound = false;
            }
        }

        if (!villagersFound) {
            return false;
        }

        double centerX = 0.0D;
        double centerY = 0.0D;
        double centerZ = 0.0D;
        for (Vec3 position : TRADING_MARKET_VILLAGER_CACHE.values()) {
            centerX += position.xCoord;
            centerY += position.yCoord;
            centerZ += position.zCoord;
        }

        centerX /= TRADING_MARKET_VILLAGER_CACHE.size();
        centerY /= TRADING_MARKET_VILLAGER_CACHE.size();
        centerZ /= TRADING_MARKET_VILLAGER_CACHE.size();

        double centerDistanceX = player.posX - centerX;
        double centerDistanceY = player.posY - centerY;
        double centerDistanceZ = player.posZ - centerZ;
        boolean scanSigns = !cachedTradingMarketSignFound
                && centerDistanceX * centerDistanceX + centerDistanceY * centerDistanceY + centerDistanceZ * centerDistanceZ <= 64.0D * 64.0D
                && (cachedTradingMarketSignTick < 0
                || player.ticksExisted - cachedTradingMarketSignTick >= 40
                || player.ticksExisted < cachedTradingMarketSignTick);
        if (scanSigns) {
            for (TileEntity tileEntity : player.worldObj.loadedTileEntityList) {
                if (!(tileEntity instanceof TileEntitySign)) {
                    continue;
                }

                TileEntitySign sign = (TileEntitySign) tileEntity;
                double signX = sign.getPos().getX() + 0.5D - centerX;
                double signY = sign.getPos().getY() + 0.5D - centerY;
                double signZ = sign.getPos().getZ() + 0.5D - centerZ;
                if (signX * signX + signY * signY + signZ * signZ > 64.0D * 64.0D) {
                    continue;
                }

                boolean hasSystemShop = false;
                boolean hasGlobalMarket = false;
                boolean hasBuy = false;
                boolean hasOpen = false;
                boolean hasPrice = false;
                boolean hasExchange = false;
                boolean hasCurrency = false;
                for (IChatComponent line : sign.signText) {
                    if (line == null) {
                        continue;
                    }

                    String text = line.getUnformattedText();
                    hasSystemShop |= text.contains("系统商店");
                    hasGlobalMarket |= text.contains("全球市场");
                    hasBuy |= text.contains("收购");
                    hasOpen |= text.contains("右键打开");
                    hasPrice |= text.contains("价格");
                    hasExchange |= text.contains("兑换") || text.contains("换金币");
                    hasCurrency |= text.contains("金币") || text.contains("货币");
                }

                boolean marketSign = (hasSystemShop && hasBuy && hasPrice)
                        || (hasGlobalMarket && hasOpen)
                        || (hasExchange && hasCurrency);
                if (!marketSign) {
                    continue;
                }

                cachedTradingMarketSignFound = true;
                break;
            }
            cachedTradingMarketSignTick = player.ticksExisted;
        }

        if (!cachedTradingMarketSignFound) {
            return false;
        }

        return centerDistanceX * centerDistanceX + centerDistanceY * centerDistanceY + centerDistanceZ * centerDistanceZ <= 24.0D * 24.0D;
    }

    static  {
        RPG_MONEYS = new HashSet<>();

        {
            RPG_MONEYS.add(new RPGMoney("铜币", Items.brick));
            RPG_MONEYS.add(new RPGMoney("银", Items.iron_ingot));
            RPG_MONEYS.add(new RPGMoney("金", Items.gold_ingot));
            RPG_MONEYS.add(new RPGMoney("钻", Items.diamond));
            RPG_MONEYS.add(new RPGMoney("绿宝石", Items.emerald));
            RPG_MONEYS.add(new RPGMoney("蓝宝石", Items.dye, 4));
            RPG_MONEYS.add(new RPGMoney("极品宝石", Items.prismarine_crystals));
        }
    }
}
