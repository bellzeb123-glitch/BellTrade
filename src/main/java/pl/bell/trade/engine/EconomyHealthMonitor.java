package pl.bell.trade.engine;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import pl.bell.trade.BellTrade;
import pl.bell.trade.event.BalanceChangeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class EconomyHealthMonitor implements Listener {

    public enum Status { STABLE, INFLATION, DEFLATION }

    private final BellTrade plugin;
    private final Deque<Long> transactionTimestamps = new ArrayDeque<>();
    private BukkitTask checkTask;

    private double inflationIndex = 1.0;
    private double moneyVelocity;
    private double shopHealthMultiplier = 1.0;
    private double marketTaxAdjustment;
    private Status status = Status.STABLE;
    private long baselineTotal;

    public EconomyHealthMonitor(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        refresh();
        long minutes = plugin.getConfig().getLong("economy.health.check-interval-minutes", 60);
        long ticks = Math.max(20L, minutes * 60L * 20L);
        checkTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, ticks, ticks);
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
    }

    public void reload() {
        baselineTotal = plugin.getConfig().getLong("economy.health.baseline-total", 0L);
        shopHealthMultiplier = plugin.getConfig().getDouble("shop.health-multiplier", 1.0);
        refresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBalanceChange(BalanceChangeEvent event) {
        if (event.getDelta() == 0) return;
        synchronized (transactionTimestamps) {
            transactionTimestamps.addLast(System.currentTimeMillis());
            while (transactionTimestamps.size() > 10_000) {
                transactionTimestamps.removeFirst();
            }
        }
    }

    public void refresh() {
        double totalMoney = plugin.getCurrencyManager().getTotalMoneyInCirculation();
        FileConfiguration config = plugin.getConfig();

        if (baselineTotal <= 0 && totalMoney > 0) {
            baselineTotal = (long) Math.max(1, totalMoney);
            config.set("economy.health.baseline-total", baselineTotal);
            plugin.saveConfig();
        }

        inflationIndex = baselineTotal > 0 ? totalMoney / baselineTotal : 1.0;
        moneyVelocity = countVelocityLastHour();

        double high = config.getDouble("economy.health.inflation-high", 1.5);
        double low = config.getDouble("economy.health.inflation-low", 0.7);
        double shopHigh = config.getDouble("economy.health.shop-multiplier-high", 0.9);
        double shopLow = config.getDouble("economy.health.shop-multiplier-low", 1.1);
        double taxBonus = config.getDouble("economy.health.market-tax-bonus-inflation", 1.0);

        if (inflationIndex > high) {
            status = Status.INFLATION;
            shopHealthMultiplier = shopHigh;
            marketTaxAdjustment = taxBonus;
        } else if (inflationIndex < low) {
            status = Status.DEFLATION;
            shopHealthMultiplier = shopLow;
            marketTaxAdjustment = 0;
        } else {
            status = Status.STABLE;
            shopHealthMultiplier = config.getDouble("shop.health-multiplier", 1.0);
            marketTaxAdjustment = 0;
        }
    }

    private double countVelocityLastHour() {
        long since = System.currentTimeMillis() - 3_600_000L;
        int balanceEvents;
        synchronized (transactionTimestamps) {
            while (!transactionTimestamps.isEmpty() && transactionTimestamps.peekFirst() < since) {
                transactionTimestamps.removeFirst();
            }
            balanceEvents = transactionTimestamps.size();
        }
        int shopTx = plugin.getPriceEngine().getPriceHistory().countTransactionsSince(since);
        return balanceEvents + shopTx;
    }

    public double getInflationIndex() {
        return inflationIndex;
    }

    public double getInflationPercent() {
        return (inflationIndex - 1.0) * 100.0;
    }

    public double getMoneyVelocity() {
        return moneyVelocity;
    }

    public double getShopHealthMultiplier() {
        return shopHealthMultiplier;
    }

    public double getMarketTaxAdjustment() {
        return marketTaxAdjustment;
    }

    public Status getStatus() {
        return status;
    }

    public long getBaselineTotal() {
        return baselineTotal;
    }

    public int getActiveListings() {
        return plugin.getListingManager().getRepository().countActive(null);
    }

    public int getShopSellsLast24h() {
        long since = System.currentTimeMillis() - 86_400_000L;
        return plugin.getPriceEngine().getPriceHistory().countTransactionsSince(since);
    }
}
