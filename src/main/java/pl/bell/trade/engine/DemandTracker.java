package pl.bell.trade.engine;

import pl.bell.trade.BellTrade;
import pl.bell.trade.market.ListingRepository;
import pl.bell.trade.model.ItemKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DemandTracker {

    private final BellTrade plugin;
    private final PriceHistoryRepository priceHistory;
    private final ListingRepository listings;
    private final Map<String, Integer> listingCounts = new ConcurrentHashMap<>();

    public DemandTracker(BellTrade plugin, PriceHistoryRepository priceHistory, ListingRepository listings) {
        this.plugin = plugin;
        this.priceHistory = priceHistory;
        this.listings = listings;
    }

    public void reload() {
        refreshListingCounts();
    }

    public double getFactor(ItemKey key) {
        long windowHours = plugin.getConfig().getLong("shop.demand-window-hours", 6);
        long since = System.currentTimeMillis() - windowHours * 3_600_000L;
        int recentSells = priceHistory.countRecentSells(key.key(), since);
        int activeListings = listingCounts.getOrDefault(key.key(), 0);

        double sellBoost = Math.min(recentSells / 100.0, 0.2);
        double listingPenalty = Math.min(activeListings / 50.0, 0.15);
        double minFactor = plugin.getConfig().getDouble("shop.demand-factor-min", 0.85);
        double maxFactor = plugin.getConfig().getDouble("shop.demand-factor-max", 1.2);

        double factor = 1.0 + sellBoost - listingPenalty;
        return Math.max(minFactor, Math.min(maxFactor, factor));
    }

    public void recordSale(ItemKey key, int amount) {
        // price history insert handles persistence; in-memory refresh optional
    }

    public void refreshListingCounts() {
        listingCounts.clear();
        for (var entry : listings.countActiveByMaterial().entrySet()) {
            listingCounts.put(entry.getKey(), entry.getValue());
        }
    }
}
