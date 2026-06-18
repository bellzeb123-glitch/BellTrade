package pl.bell.trade.integration;

import org.bukkit.Bukkit;
import pl.bell.trade.BellTrade;
import pl.bell.trade.expansion.BellTradeExpansion;

public final class PlaceholderHook {

    private final BellTrade plugin;
    private BellTradeExpansion expansion;

    public PlaceholderHook(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!plugin.getConfig().getBoolean("integrations.placeholderapi", true)) return;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        try {
            expansion = new BellTradeExpansion(plugin);
            if (expansion.register()) {
                plugin.getLogger().info("PlaceholderAPI expansion registered.");
            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("PlaceholderAPI present but API classes missing.");
        }
    }

    public void unregister() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Exception ignored) {}
            expansion = null;
        }
    }
}
