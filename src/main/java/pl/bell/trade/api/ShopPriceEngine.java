package pl.bell.trade.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.model.ItemKey;

/**
 * Public contract for sell-shop pricing. Free {@code PriceEngine} implements this;
 * BellTrade Pro may register an override via {@link BellTradeProBridge}.
 */
public interface ShopPriceEngine {

    String getEngineVersion();

    double getBasePrice(ItemKey key);

    double getCurrentPrice(ItemKey key);

    double getCurrentPrice(ItemKey key, ItemStack stack);

    default double getCurrentPrice(Player player, ItemKey key, ItemStack stack) {
        return getCurrentPrice(key, stack);
    }

    void recordSale(ItemKey key, double unitPrice, int amount);
}
