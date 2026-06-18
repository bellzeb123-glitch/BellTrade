package pl.bell.trade.engine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.ShopConfigManager;
import pl.bell.trade.model.ItemKey;
import pl.bell.trade.model.ShopItemEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SupplyTracker {

    private final BellTrade plugin;
    private final SupplySampleRepository repository;
    private final Map<String, Long> supplyCache = new ConcurrentHashMap<>();
    private BukkitTask snapshotTask;

    public SupplyTracker(BellTrade plugin, SupplySampleRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void start() {
        long intervalMinutes = plugin.getConfig().getLong("shop.supply-snapshot-minutes", 15);
        long intervalTicks = intervalMinutes * 60L * 20L;
        snapshotTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runSnapshot, intervalTicks, intervalTicks);
        refreshFromDatabase();
    }

    public void shutdown() {
        if (snapshotTask != null) snapshotTask.cancel();
    }

    public void reload() {
        refreshFromDatabase();
    }

    public double getFactor(ItemKey key) {
        long qty = supplyCache.getOrDefault(key.key(), 0L);
        long high = plugin.getConfig().getLong("shop.supply-high-threshold", 10_000L);
        long low = plugin.getConfig().getLong("shop.supply-low-threshold", 500L);
        double minFactor = plugin.getConfig().getDouble("shop.supply-factor-min", 0.35);
        double maxFactor = plugin.getConfig().getDouble("shop.supply-factor-max", 1.0);

        if (qty <= low) return maxFactor;
        if (qty >= high) return minFactor;
        double ratio = (double) (qty - low) / (high - low);
        return maxFactor - ratio * (maxFactor - minFactor);
    }

    public void recordSale(ItemKey key, int amount) {
        supplyCache.merge(key.key(), (long) amount, Long::sum);
    }

    private void refreshFromDatabase() {
        long windowHours = plugin.getConfig().getLong("shop.supply-window-hours", 24);
        long since = System.currentTimeMillis() - windowHours * 3_600_000L;
        Map<String, Long> latest = repository.latestTotals(since);
        supplyCache.clear();
        supplyCache.putAll(latest);
    }

    private void runSnapshot() {
        ShopConfigManager shop = plugin.getShopConfigManager();
        Map<String, Long> counts = new HashMap<>();
        List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        int maxPlayers = plugin.getConfig().getInt("shop.supply-sample-max-players", 20);
        int step = Math.max(1, online.size() / maxPlayers);

        for (int i = 0; i < online.size(); i += step) {
            Player player = online.get(i);
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack == null || stack.getType().isAir()) continue;
                if (!isPlain(stack)) continue;
                ShopItemEntry entry = shop.getItemEntry(stack.getType());
                if (entry == null) continue;
                counts.merge(stack.getType().name(), (long) stack.getAmount(), Long::sum);
            }
        }

        long now = System.currentTimeMillis();
        repository.insertBatch(counts, now);
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            supplyCache.merge(e.getKey(), e.getValue(), Long::sum);
        }
    }

    private boolean isPlain(ItemStack stack) {
        if (!stack.hasItemMeta()) return true;
        var meta = stack.getItemMeta();
        return !meta.hasDisplayName() && !meta.hasEnchants() && meta.lore() == null;
    }
}
