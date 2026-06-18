package pl.bell.trade.model;

import org.bukkit.Material;

import java.util.List;

public final class ShopCategory {

    private final String id;
    private final Material icon;
    private final String displayName;
    private final List<ShopItemEntry> items;

    public ShopCategory(String id, Material icon, String displayName, List<ShopItemEntry> items) {
        this.id = id;
        this.icon = icon;
        this.displayName = displayName;
        this.items = List.copyOf(items);
    }

    public String getId() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ShopItemEntry> getItems() {
        return items;
    }
}
