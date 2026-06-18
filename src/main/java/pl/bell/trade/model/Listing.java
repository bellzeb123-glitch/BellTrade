package pl.bell.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class Listing {

    private final long id;
    private final UUID sellerUuid;
    private final ItemStack item;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    public Listing(long id, UUID sellerUuid, ItemStack item, double price, long createdAt, long expiresAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
