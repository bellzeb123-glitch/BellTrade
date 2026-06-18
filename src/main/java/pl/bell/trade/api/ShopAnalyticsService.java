package pl.bell.trade.api;

import pl.bell.trade.model.ItemSellStats;

import java.util.List;
import java.util.Map;

public interface ShopAnalyticsService {

    int countShopSellsSince(long sinceMs);

    Map<String, Integer> sellVolumeByItem(long sinceMs);

    List<ItemSellStats> topSoldItems(long sinceMs, int limit);

    double averageUnitPrice(String itemKey, long sinceMs);
}
