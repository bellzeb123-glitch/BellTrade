package pl.bell.trade.market;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.model.ExpiredMailboxEntry;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ExpiredMailboxManager {

    private final BellTrade plugin;
    private final ExpiredMailboxRepository repository;

    public ExpiredMailboxManager(BellTrade plugin, ExpiredMailboxRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public ExpiredMailboxRepository getRepository() {
        return repository;
    }

    public void deposit(UUID owner, ItemStack item, long listingId) {
        repository.insertAsync(owner, item.clone(), System.currentTimeMillis(), listingId);
    }

    public void depositSync(UUID owner, ItemStack item, long listingId) throws SQLException {
        repository.insert(owner, item.clone(), System.currentTimeMillis(), listingId);
    }

    public int countPending(UUID owner) {
        return repository.countByOwner(owner);
    }

    public int getTotalPages(UUID owner) {
        int perPage = plugin.getConfig().getInt("market.items-per-page", 28);
        int total = countPending(owner);
        return Math.max(1, (int) Math.ceil(total / (double) perPage));
    }

    public List<ExpiredMailboxEntry> getPage(UUID owner, int page) {
        int perPage = plugin.getConfig().getInt("market.items-per-page", 28);
        page = Math.max(1, page);
        return repository.findByOwner(owner, (page - 1) * perPage, perPage);
    }

    public boolean claim(Player player, long entryId) {
        LangManager lang = plugin.getLangManager();
        var opt = repository.findById(entryId);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("market.expired-not-found"));
            return false;
        }
        ExpiredMailboxEntry entry = opt.get();
        if (!entry.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(lang.component("no-permission"));
            return false;
        }
        try {
            if (!repository.deleteSync(entryId, player.getUniqueId())) {
                player.sendMessage(lang.component("market.expired-not-found"));
                return false;
            }
        } catch (SQLException e) {
            player.sendMessage(lang.component("market.expired-claim-failed"));
            plugin.getLogger().severe("Expired claim delete failed: " + e.getMessage());
            return false;
        }
        giveOrDrop(player, entry.getItem());
        player.sendMessage(lang.component("market.expired-claimed",
            "id", String.valueOf(entryId)));
        return true;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
    }
}
