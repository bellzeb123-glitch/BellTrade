package pl.bell.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class ExpiredMailboxEntry {

    private final long id;
    private final UUID ownerUuid;
    private final ItemStack item;
    private final long expiredAt;
    private final long listingId;

    public ExpiredMailboxEntry(long id, UUID ownerUuid, ItemStack item, long expiredAt, long listingId) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.item = item;
        this.expiredAt = expiredAt;
        this.listingId = listingId;
    }

    public long getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public ItemStack getItem() { return item.clone(); }
    public long getExpiredAt() { return expiredAt; }
    public long getListingId() { return listingId; }
}
