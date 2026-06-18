package pl.bell.trade.model;

import org.bukkit.Material;

public final class ShopItemEntry {

    private final Material material;
    private final double basePrice;
    private final double minPrice;
    private final double maxPrice;

    public ShopItemEntry(Material material, double basePrice, double minPrice, double maxPrice) {
        this.material = material;
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public Material getMaterial() {
        return material;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public ItemKey getItemKey() {
        return ItemKey.of(material);
    }
}
