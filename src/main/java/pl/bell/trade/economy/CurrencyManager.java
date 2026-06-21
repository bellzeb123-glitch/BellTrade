package pl.bell.trade.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.event.BalanceChangeEvent;
import pl.bell.trade.migration.ImportMode;
import pl.bell.trade.storage.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CurrencyManager implements EconomyProvider {

    private final BellTrade plugin;
    private final BalanceRepository repository;
    private final Map<UUID, Double> balances = new HashMap<>();

    public CurrencyManager(BellTrade plugin, Database database) {
        this.plugin = plugin;
        this.repository = new BalanceRepository(database, plugin.getLogger());
        reload();
    }

    public void reload() {
        repository.loadAll(balances);
        plugin.getLogger().info("Loaded " + balances.size() + " player balances.");
    }

    public void flush() {
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            repository.upsert(e.getKey(), e.getValue());
        }
    }

    @Override
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getCurrencyConfig().getStartingBalance());
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    @Override
    public boolean hasEnough(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    @Override
    public boolean withdraw(UUID uuid, double amount, String reason) {
        if (amount < 0) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        setBalanceInternal(uuid, current - amount, reason);
        return true;
    }

    @Override
    public boolean deposit(UUID uuid, double amount, String reason) {
        if (amount < 0) return false;
        setBalanceInternal(uuid, getBalance(uuid) + amount, reason);
        return true;
    }

    @Override
    public void setBalance(UUID uuid, double amount, String reason) {
        setBalanceInternal(uuid, Math.max(0, amount), reason);
    }

    private void setBalanceInternal(UUID uuid, double newBalance, String reason) {
        double oldBalance = getBalance(uuid);
        if (oldBalance == newBalance) return;

        balances.put(uuid, newBalance);
        repository.saveAsync(uuid, newBalance);

        double delta = newBalance - oldBalance;
        Bukkit.getScheduler().runTask(plugin, () ->
            Bukkit.getPluginManager().callEvent(
                new BalanceChangeEvent(uuid, oldBalance, newBalance, delta, reason)));
    }

    public List<BalanceRepository.BalanceEntry> getTopList(int limit) {
        List<BalanceRepository.BalanceEntry> fromDb = repository.getTop(limit);
        if (!fromDb.isEmpty()) return fromDb;

        return balances.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(limit)
            .map(e -> new BalanceRepository.BalanceEntry(e.getKey(), e.getValue()))
            .toList();
    }

    public double getTotalMoneyInCirculation() {
        double sum = repository.sumAllBalances();
        if (sum > 0) return sum;
        return balances.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public int getTrackedPlayerCount() {
        return Math.max(balances.size(), repository.getTop(Integer.MAX_VALUE).size());
    }

    @Override
    public String format(double amount) {
        return plugin.getCurrencyConfig().formatAmount(amount);
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }

    /**
     * Import sald z zewnetrznego zrodla (np. Essentials userdata).
     * @return liczba zaktualizowanych kont
     */
    public int importBalances(Map<UUID, Double> amounts, ImportMode mode, String reason) {
        int updated = 0;
        for (Map.Entry<UUID, Double> entry : amounts.entrySet()) {
            UUID uuid = entry.getKey();
            double src = entry.getValue();
            double target = switch (mode) {
                case REPLACE -> src;
                case ADD -> getBalance(uuid) + src;
                case MAX -> Math.max(getBalance(uuid), src);
            };
            if (target < 0) target = 0;
            setBalance(uuid, target, reason);
            updated++;
        }
        flush();
        return updated;
    }
}
