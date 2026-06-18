package pl.bell.trade.api;

import pl.bell.trade.BellTrade;
import pl.bell.trade.config.ShopConfigManager;
import pl.bell.trade.engine.PriceHistoryRepository;
import pl.bell.trade.model.ItemSellStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopAnalyticsServiceImpl implements ShopAnalyticsService {

    private final PriceHistoryRepository priceHistory;

    public ShopAnalyticsServiceImpl(BellTrade plugin) {
        this.priceHistory = plugin.getPriceEngine().getPriceHistory();
    }

    @Override
    public int countShopSellsSince(long sinceMs) {
        return priceHistory.countTransactionsSince(sinceMs);
    }

    @Override
    public Map<String, Integer> sellVolumeByItem(long sinceMs) {
        return priceHistory.recentSellTotals(sinceMs);
    }

    @Override
    public List<ItemSellStats> topSoldItems(long sinceMs, int limit) {
        List<ItemSellStats> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : priceHistory.topSoldItems(sinceMs, limit)) {
            String key = entry.getKey();
            int volume = entry.getValue();
            double avg = priceHistory.averageUnitPrice(key, sinceMs);
            stats.add(new ItemSellStats(key, volume, avg));
        }
        return stats;
    }

    @Override
    public double averageUnitPrice(String itemKey, long sinceMs) {
        return priceHistory.averageUnitPrice(itemKey, sinceMs);
    }
}
