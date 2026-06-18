package pl.bell.trade.market;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl.bell.trade.BellTrade;
import pl.bell.trade.api.TransactionGuard;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.event.MarketListingEvent;
import pl.bell.trade.event.MarketPurchaseEvent;
import pl.bell.trade.model.Listing;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ListingManager {

    private final BellTrade plugin;
    private final ListingRepository repository;
    private final TransactionGuard transactionGuard;
    private BukkitTask expireTask;

    public ListingManager(BellTrade plugin, ListingRepository repository, TransactionGuard transactionGuard) {
        this.plugin = plugin;
        this.repository = repository;
        this.transactionGuard = transactionGuard;
        startExpireTask();
    }

    public ListingRepository getRepository() {
        return repository;
    }

    public int getMaxListings(Player player) {
        if (player.hasPermission("belltradepro.market.limit.unlimited")) return Integer.MAX_VALUE;
        if (player.hasPermission("belltrade.market.limit.50") || player.hasPermission("belltradepro.market.limit.50")) return 50;
        if (player.hasPermission("belltrade.market.limit.10") || player.hasPermission("belltradepro.market.limit.10")) return 10;
        return plugin.getConfig().getInt("market.max-listings-per-player", 5);
    }

    public double getTaxPercent() {
        double base = plugin.getConfig().getDouble("market.tax-percent", 5.0);
        if (plugin.getEconomyHealthMonitor() != null) {
            base += plugin.getEconomyHealthMonitor().getMarketTaxAdjustment();
        }
        return Math.min(25.0, Math.max(0, base));
    }

    public void createListing(Player seller, double price) {
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            seller.sendMessage(plugin.getLangManager().component("market.no-item"));
            return;
        }
        ItemStack toSell = hand.clone();
        hand.setAmount(0);
        seller.getInventory().setItemInMainHand(hand);
        createListingFromItem(seller, toSell, price);
    }

    public void createListingFromItem(Player seller, ItemStack toSell, double price) {
        LangManager lang = plugin.getLangManager();
        if (toSell == null || toSell.getType().isAir()) {
            seller.sendMessage(lang.component("market.no-item"));
            return;
        }

        double maxPrice = plugin.getConfig().getDouble("market.max-price", 1_000_000.0);
        if (price <= 0 || price > maxPrice) {
            seller.sendMessage(lang.component("market.invalid-price", "max", plugin.getCurrencyManager().format(maxPrice)));
            returnItemToPlayer(seller, toSell);
            return;
        }

        if (repository.countBySeller(seller.getUniqueId()) >= getMaxListings(seller)) {
            seller.sendMessage(lang.component("market.limit-reached", "max", String.valueOf(getMaxListings(seller))));
            returnItemToPlayer(seller, toSell);
            return;
        }

        if (isBlacklisted(toSell.getType())) {
            seller.sendMessage(lang.component("market.item-blacklisted"));
            returnItemToPlayer(seller, toSell);
            return;
        }

        long now = System.currentTimeMillis();
        long hours = plugin.getConfig().getLong("market.listing-duration-hours", 48);
        if (seller.hasPermission("belltradepro.market.long-listings")) {
            hours = plugin.getConfig().getLong("pro.market.listing-duration-hours", 168L);
        }
        long expires = now + hours * 3_600_000L;

        databaseAsyncInsert(seller, toSell.clone(), price, now, expires);
    }

    public void returnItemToPlayer(Player player, ItemStack item) {
        giveOrDrop(player, item);
    }

    private void databaseAsyncInsert(Player seller, ItemStack item, double price, long created, long expires) {
        plugin.getDatabase().async(() -> {
            try {
                long id = repository.insert(seller.getUniqueId(), item, price, created, expires);
                Listing listing = new Listing(id, seller.getUniqueId(), item, price, created, expires);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MarketListingEvent event = new MarketListingEvent(seller, listing);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        giveOrDrop(seller, item);
                        repository.delete(id);
                        seller.sendMessage(plugin.getLangManager().component("market.listing-cancelled"));
                        return;
                    }
                    seller.sendMessage(plugin.getLangManager().component("market.listed",
                        "price", plugin.getCurrencyManager().format(price),
                        "id", String.valueOf(id)));
                });
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    giveOrDrop(seller, item);
                    seller.sendMessage(plugin.getLangManager().component("market.listing-failed"));
                    plugin.getLogger().severe("Listing insert failed: " + e.getMessage());
                });
            }
        });
    }

    public void cancelListing(Player seller, long listingId) {
        var opt = repository.findById(listingId);
        if (opt.isEmpty()) {
            seller.sendMessage(plugin.getLangManager().component("market.not-found"));
            return;
        }
        Listing listing = opt.get();
        if (!listing.getSellerUuid().equals(seller.getUniqueId()) && !seller.hasPermission("belltrade.admin")) {
            seller.sendMessage(plugin.getLangManager().component("no-permission"));
            return;
        }
        try {
            repository.deleteSync(listingId);
        } catch (SQLException e) {
            seller.sendMessage(plugin.getLangManager().component("market.purchase-failed"));
            plugin.getLogger().severe("Cancel listing failed: " + e.getMessage());
            return;
        }
        giveOrDrop(seller, listing.getItem());
        seller.sendMessage(plugin.getLangManager().component("market.cancelled", "id", String.valueOf(listingId)));
    }

    public void purchase(Player buyer, long listingId) {
        if (!transactionGuard.tryLock(listingId)) return;

        LangManager lang = plugin.getLangManager();
        try {
            var opt = repository.findById(listingId);
            if (opt.isEmpty()) {
                buyer.sendMessage(lang.component("market.not-found"));
                return;
            }
            Listing listing = opt.get();
            if (listing.isExpired()) {
                buyer.sendMessage(lang.component("market.expired"));
                repository.delete(listingId);
                return;
            }
            if (listing.getSellerUuid().equals(buyer.getUniqueId())) {
                buyer.sendMessage(lang.component("market.cannot-buy-own"));
                return;
            }

            double price = listing.getPrice();
            double taxPercent = getTaxPercent();
            Player sellerOnline = Bukkit.getPlayer(listing.getSellerUuid());
            if (sellerOnline != null && sellerOnline.hasPermission("belltradepro.tax.exempt")) {
                taxPercent = 0;
            }
            double tax = price * taxPercent / 100.0;
            double sellerReceives = price - tax;

            var eco = plugin.getCurrencyManager();
            if (!eco.hasEnough(buyer.getUniqueId(), price)) {
                buyer.sendMessage(lang.component("not-enough-money"));
                return;
            }

            MarketPurchaseEvent event = new MarketPurchaseEvent(buyer, listing.getSellerUuid(), listing, price, tax);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                buyer.sendMessage(lang.component("market.purchase-cancelled"));
                return;
            }

            if (!eco.withdraw(buyer.getUniqueId(), price, "market-buy")) {
                buyer.sendMessage(lang.component("not-enough-money"));
                return;
            }
            eco.deposit(listing.getSellerUuid(), sellerReceives, "market-sell");

            giveOrDrop(buyer, listing.getItem());
            repository.deleteSync(listingId);

            buyer.sendMessage(lang.component("market.purchased",
                "price", eco.format(price),
                "id", String.valueOf(listingId)));

            if (sellerOnline != null) {
                sellerOnline.sendMessage(lang.component("market.sold",
                    "buyer", buyer.getName(),
                    "price", eco.format(sellerReceives),
                    "id", String.valueOf(listingId)));
            }
        } catch (SQLException e) {
            buyer.sendMessage(lang.component("market.purchase-failed"));
            plugin.getLogger().severe("Market purchase failed: " + e.getMessage());
        } finally {
            transactionGuard.unlock(listingId);
        }
    }

    public List<Listing> getBrowsePage(int page, String materialFilter) {
        int perPage = plugin.getConfig().getInt("market.items-per-page", 28);
        return repository.findActive((page - 1) * perPage, perPage, materialFilter);
    }

    public int getTotalPages(String materialFilter) {
        int perPage = plugin.getConfig().getInt("market.items-per-page", 28);
        int total = repository.countActive(materialFilter);
        return Math.max(1, (int) Math.ceil(total / (double) perPage));
    }

    public List<Listing> getPlayerListings(UUID seller) {
        return repository.findBySeller(seller);
    }

    public void shutdown() {
        if (expireTask != null) expireTask.cancel();
    }

    private void startExpireTask() {
        long minutes = plugin.getConfig().getLong("market.expire-check-minutes", 15);
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processExpired, 20L * 60 * minutes, 20L * 60 * minutes);
    }

    private void processExpired() {
        for (Listing listing : repository.findExpired()) {
            try {
                repository.deleteSync(listing.getId());
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete expired listing " + listing.getId());
                continue;
            }
            Player seller = Bukkit.getPlayer(listing.getSellerUuid());
            if (seller != null && seller.isOnline()) {
                giveOrDrop(seller, listing.getItem());
                seller.sendMessage(plugin.getLangManager().component("market.expired-returned",
                    "id", String.valueOf(listing.getId())));
            }
        }
    }

    private boolean isBlacklisted(Material material) {
        List<String> list = plugin.getConfig().getStringList("market.blacklist");
        return list.stream().anyMatch(s -> s.equalsIgnoreCase(material.name()));
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
    }
}
