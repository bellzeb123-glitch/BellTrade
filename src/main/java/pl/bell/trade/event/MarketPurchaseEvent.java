package pl.bell.trade.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.bell.trade.model.Listing;

import java.util.UUID;

public class MarketPurchaseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID seller;
    private final Listing listing;
    private final double price;
    private final double tax;
    private boolean cancelled;

    public MarketPurchaseEvent(Player buyer, UUID seller, Listing listing, double price, double tax) {
        super(buyer, false);
        this.seller = seller;
        this.listing = listing;
        this.price = price;
        this.tax = tax;
        this.cancelled = false;
    }

    public Player getBuyer() { return getPlayer(); }
    public UUID getSeller() { return seller; }
    public Listing getListing() { return listing; }
    public double getPrice() { return price; }
    public double getTax() { return tax; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
