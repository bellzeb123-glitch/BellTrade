package pl.bell.trade.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.bell.trade.model.ItemKey;

public class ShopSellEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemKey itemKey;
    private final int amount;
    private final double basePrice;
    private final double finalPrice;
    private final double totalPayout;
    private boolean cancelled;

    public ShopSellEvent(Player player, ItemKey itemKey, int amount, double basePrice, double finalPrice) {
        this(player, itemKey, amount, basePrice, finalPrice, finalPrice * amount);
    }

    public ShopSellEvent(Player player, ItemKey itemKey, int amount, double basePrice,
                         double finalPrice, double totalPayout) {
        super(player, false);
        this.itemKey = itemKey;
        this.amount = amount;
        this.basePrice = basePrice;
        this.finalPrice = finalPrice;
        this.totalPayout = totalPayout;
        this.cancelled = false;
    }

    public ItemKey getItemKey() { return itemKey; }
    public int getAmount() { return amount; }
    public double getBasePrice() { return basePrice; }
    public double getFinalPrice() { return finalPrice; }
    public double getTotalPayout() { return totalPayout; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
