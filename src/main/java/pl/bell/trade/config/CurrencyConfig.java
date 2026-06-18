package pl.bell.trade.config;

import pl.bell.trade.BellTrade;

/**
 * Runtime currency display settings. Name and symbol can be changed from Admin GUI
 * and are persisted to config.yml immediately.
 */
public class CurrencyConfig {

    private final BellTrade plugin;
    private String currencyName;
    private String currencySymbol;
    private String format;
    private double startingBalance;
    private double maxPayAmount;

    public CurrencyConfig(BellTrade plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currencyName = plugin.getConfig().getString("economy.currency-name", "Coins");
        currencySymbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        format = plugin.getConfig().getString("economy.format", "&6{symbol}&f{amount} &7{currency}");
        startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
        maxPayAmount = plugin.getConfig().getDouble("economy.max-pay-amount", 1_000_000.0);
    }

    /** Called from Admin GUI after admin enters a new currency name. */
    public void setCurrencyName(String name) {
        if (name == null || name.isBlank()) return;
        currencyName = name.trim();
        plugin.getConfig().set("economy.currency-name", currencyName);
        plugin.saveConfig();
        plugin.getLangManager().reload();
    }

    /** Called from Admin GUI after admin enters a new currency symbol. */
    public void setCurrencySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return;
        currencySymbol = symbol.trim();
        plugin.getConfig().set("economy.currency-symbol", currencySymbol);
        plugin.saveConfig();
        plugin.getLangManager().reload();
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public String getFormat() {
        return format;
    }

    public double getStartingBalance() {
        return startingBalance;
    }

    public double getMaxPayAmount() {
        return maxPayAmount;
    }

    public String formatAmount(double amount) {
        String amountStr = amount == Math.floor(amount)
            ? String.format("%,.0f", amount)
            : String.format("%,.2f", amount);
        return format
            .replace("{symbol}", currencySymbol)
            .replace("{amount}", amountStr)
            .replace("{currency}", currencyName);
    }
}
