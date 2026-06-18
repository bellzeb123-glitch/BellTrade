package pl.bell.trade.api;

import net.kyori.adventure.text.Component;
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
    private final ShopCatalogService shopCatalog;
    private final ShopAnalyticsService shopAnalytics;
    private final MarketExtensionService marketExtension;

    private BellTradeAPI(BellTrade plugin) {
        this.plugin = plugin;
        this.economy = new EconomyService(plugin.getCurrencyManager());
        this.shopCatalog = new ShopCatalogServiceImpl(plugin.getShopConfigManager());
        this.shopAnalytics = new ShopAnalyticsServiceImpl(plugin);
        this.marketExtension = new MarketExtensionServiceImpl(plugin);
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

    public ShopPriceEngine getShopPriceEngine() {
        return BellTradeProBridge.resolvePriceEngine(plugin.getPriceEngine());
    }

    public ShopPriceEngine getDefaultShopPriceEngine() {
        return plugin.getPriceEngine();
    }

    public ShopCatalogService getShopCatalog() {
        return shopCatalog;
    }

    public ShopAnalyticsService getShopAnalytics() {
        return shopAnalytics;
    }

    public MarketExtensionService getMarketExtension() {
        return marketExtension;
    }

    public EconomyHealthMonitor getEconomyHealthMonitor() {
        return plugin.getEconomyHealthMonitor();
    }

    public double getInflationPercent() {
        EconomyHealthMonitor monitor = plugin.getEconomyHealthMonitor();
        return monitor != null ? monitor.getInflationPercent() : 0;
    }

    public void sellToShop(Player player, ItemStack item) {
        plugin.getShopManager().sellItemStack(player, item);
    }

    public double getShopPrice(ItemKey key) {
        return getShopPriceEngine().getCurrentPrice(key);
    }

    public String getLangRaw(String key, Object... args) {
        return plugin.getLangManager().getRaw(key, args);
    }

    public java.util.List<String> getLangList(String key, Object... args) {
        return plugin.getLangManager().getList(key, args);
    }

    public Component getLangComponent(String key, Object... args) {
        return plugin.getLangManager().componentRaw(key, args);
    }

    public String getMaterialName(org.bukkit.Material material) {
        return plugin.getLangManager().materialName(material);
    }

    public Component colorizeText(String text) {
        return plugin.getLangManager().colorize(text);
    }
}
