package pl.bell.trade.engine;

import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.ShopConfigManager;
import pl.bell.trade.model.ItemKey;
import pl.bell.trade.model.ShopItemEntry;

public class PriceEngine {

    public static final String ENGINE_VERSION = "free-1";

    private final BellTrade plugin;
    private final ShopConfigManager shopConfig;
    private final SupplyTracker supplyTracker;
    private final DemandTracker demandTracker;
    private final RarityScorer rarityScorer;
    private final PriceHistoryRepository priceHistory;

    public PriceEngine(BellTrade plugin, ShopConfigManager shopConfig,
                       SupplyTracker supplyTracker, DemandTracker demandTracker,
                       RarityScorer rarityScorer, PriceHistoryRepository priceHistory) {
        this.plugin = plugin;
        this.shopConfig = shopConfig;
        this.supplyTracker = supplyTracker;
        this.demandTracker = demandTracker;
        this.rarityScorer = rarityScorer;
        this.priceHistory = priceHistory;
    }

    public double getBasePrice(ItemKey key) {
        ShopItemEntry entry = shopConfig.getItemEntry(key.getMaterial());
        return entry != null ? entry.getBasePrice() : 0;
    }

    public double getCurrentPrice(ItemKey key) {
        return getCurrentPrice(key, null);
    }

    public double getCurrentPrice(ItemKey key, ItemStack stack) {
        ShopItemEntry entry = shopConfig.getItemEntry(key.getMaterial());
        if (entry == null) return 0;

        double supplyFactor = supplyTracker.getFactor(key);
        double demandFactor = demandTracker.getFactor(key);
        double rarity = rarityScorer.score(key, stack);
        double healthMult = plugin.getEconomyHealthMonitor() != null
            ? plugin.getEconomyHealthMonitor().getShopHealthMultiplier()
            : plugin.getConfig().getDouble("shop.health-multiplier", 1.0);

        double price = entry.getBasePrice() * supplyFactor * demandFactor * rarity * healthMult;
        return Math.max(entry.getMinPrice(), Math.min(entry.getMaxPrice(), price));
    }

    public void recordSale(ItemKey key, double unitPrice, int amount) {
        supplyTracker.recordSale(key, amount);
        demandTracker.recordSale(key, amount);
        priceHistory.insert(key.key(), unitPrice, amount, ENGINE_VERSION, System.currentTimeMillis());
    }

    public PriceHistoryRepository getPriceHistory() {
        return priceHistory;
    }
}
