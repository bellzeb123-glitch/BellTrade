package pl.bell.trade.model;

/**
 * Aggregated sell-shop statistics for analytics (Free API / Pro GUI).
 */
public record ItemSellStats(String itemKey, int volume, double averagePrice) {
}
