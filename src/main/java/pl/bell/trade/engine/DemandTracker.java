package pl.bell.trade.engine;

import pl.bell.trade.BellTrade;
import pl.bell.trade.market.ListingRepository;
import pl.bell.trade.model.ItemKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DemandTracker {

    private static final long CACHE_TTL_MS = 10_000L;

    private record DemandEntry(double factor, long cachedAt) {}

    private final BellTrade plugin;
    private final PriceHistoryRepository priceHistory;
    private final ListingRepository listings;
    private final Map<String, Integer> listingCounts = new ConcurrentHashMap<>();
    private final Map<String, DemandEntry> demandCache = new ConcurrentHashMap<>();

    public DemandTracker(BellTrade plugin, PriceHistoryRepository priceHistory, ListingRepository listings) {
        this.plugin = plugin;
        this.priceHistory = priceHistory;
        this.listings = listings;
    }

    public void reload() {
        invalidateAllCache();
        refreshListingCounts();
    }

    public double getFactor(ItemKey key) {
        String cacheKey = key.key();
        long now = System.currentTimeMillis();

        DemandEntry cached = demandCache.get(cacheKey);
        if (cached != null && (now - cached.cachedAt()) < CACHE_TTL_MS) {
            return cached.factor();
        }

        double factor = computeFactor(key, now);
        demandCache.put(cacheKey, new DemandEntry(factor, now));
        return factor;
    }

    private double computeFactor(ItemKey key, long now) {
        long windowHours = plugin.getConfig().getLong("shop.demand-window-hours", 6);
        long since = now - windowHours * 3_600_000L;
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
        invalidateCache(key);
    }

    public void invalidateCache(ItemKey key) {
        demandCache.remove(key.key());
    }

    public void invalidateAllCache() {
        demandCache.clear();
    }

    public void refreshListingCounts() {
        listingCounts.clear();
        for (var entry : listings.countActiveByMaterial().entrySet()) {
            listingCounts.put(entry.getKey(), entry.getValue());
        }
    }
}
