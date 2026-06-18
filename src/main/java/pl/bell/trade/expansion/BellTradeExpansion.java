package pl.bell.trade.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.bell.trade.BellTrade;
import pl.bell.trade.engine.EconomyHealthMonitor;

public class BellTradeExpansion extends PlaceholderExpansion {

    private final BellTrade plugin;

    public BellTradeExpansion(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "belltrade";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bellzeb";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        return switch (params.toLowerCase()) {
            case "balance" -> plugin.getCurrencyManager().format(plugin.getCurrencyManager().getBalance(player.getUniqueId()));
            case "balance_raw" -> String.valueOf(plugin.getCurrencyManager().getBalance(player.getUniqueId()));
            case "currency" -> plugin.getCurrencyConfig().getCurrencyName();
            case "currency_symbol" -> plugin.getCurrencyConfig().getCurrencySymbol();
            case "listings" -> String.valueOf(plugin.getListingManager().getRepository().countBySeller(player.getUniqueId()));
            case "inflation" -> formatInflation();
            case "economy_status" -> economyStatus();
            default -> null;
        };
    }

    private String formatInflation() {
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();
        if (health == null) return "0";
        double pct = health.getInflationPercent();
        return (pct >= 0 ? "+" : "") + String.format("%.1f", pct);
    }

    private String economyStatus() {
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();
        if (health == null) return "STABLE";
        return health.getStatus().name();
    }
}
