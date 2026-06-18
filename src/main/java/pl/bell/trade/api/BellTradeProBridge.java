package pl.bell.trade.api;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Integration point for BellTrade Pro addon. Pro registers hooks here; Free core delegates when set.
 */
public final class BellTradeProBridge {

    private static ShopPriceEngine priceEngineOverride;
    private static Consumer<Player> proAdminMenuHandler;

    private BellTradeProBridge() {
    }

    public static void register(ShopPriceEngine priceEngine, Consumer<Player> adminMenuHandler) {
        priceEngineOverride = priceEngine;
        proAdminMenuHandler = adminMenuHandler;
    }

    public static void unregister() {
        priceEngineOverride = null;
        proAdminMenuHandler = null;
    }

    public static boolean isRegistered() {
        return proAdminMenuHandler != null;
    }

    public static ShopPriceEngine resolvePriceEngine(ShopPriceEngine defaultEngine) {
        return priceEngineOverride != null ? priceEngineOverride : defaultEngine;
    }

    public static void openProAdminMenu(Player player) {
        if (proAdminMenuHandler != null) {
            proAdminMenuHandler.accept(player);
        }
    }
}
