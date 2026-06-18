package pl.bell.trade.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.bell.trade.model.Listing;

public class MarketListingEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Listing listing;
    private boolean cancelled;

    public MarketListingEvent(Player seller, Listing listing) {
        super(seller, false);
        this.listing = listing;
        this.cancelled = false;
    }

    public Listing getListing() { return listing; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
