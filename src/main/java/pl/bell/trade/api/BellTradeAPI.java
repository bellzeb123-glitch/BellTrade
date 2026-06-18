package pl.bell.trade.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.economy.EconomyProvider;
import pl.bell.trade.engine.EconomyHealthMonitor;
import pl.bell.trade.engine.PriceEngine;
import pl.bell.trade.market.ListingManager;
import pl.bell.trade.model.ItemKey;
import pl.bell.trade.shop.ShopManager;
import pl.bell.trade.trade.TradeManager;

public final class BellTradeAPI {

    private static BellTradeAPI instance;

    private final BellTrade plugin;
    private final EconomyService economy;

    private BellTradeAPI(BellTrade plugin) {
        this.plugin = plugin;
        this.economy = new EconomyService(plugin.getCurrencyManager());
    }

    public static void init(BellTrade plugin) {
        instance = new BellTradeAPI(plugin);
    }

    public static BellTradeAPI get() {
        if (instance == null) {
            throw new IllegalStateException("BellTradeAPI not initialized");
        }
        return instance;
    }

    public BellTrade getPlugin() {
        return plugin;
    }

    public EconomyProvider getEconomy() {
        return economy;
    }

    public CurrencyManager getCurrencyManager() {
        return plugin.getCurrencyManager();
    }

    public TradeManager getTradeManager() {
        return plugin.getTradeManager();
    }

    public ListingManager getListingManager() {
        return plugin.getListingManager();
    }

    public ShopManager getShopManager() {
        return plugin.getShopManager();
    }

    public PriceEngine getPriceEngine() {
        return plugin.getPriceEngine();
    }

    public EconomyHealthMonitor getEconomyHealthMonitor() {
        return plugin.getEconomyHealthMonitor();
    }

    public void sellToShop(Player player, ItemStack item) {
        plugin.getShopManager().sellItemStack(player, item);
    }

    public double getShopPrice(ItemKey key) {
        return plugin.getPriceEngine().getCurrentPrice(key);
    }
}
