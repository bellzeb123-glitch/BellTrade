package pl.bell.trade.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class BalanceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID uuid;
    private final double oldBalance;
    private final double newBalance;
    private final double delta;
    private final String reason;

    public BalanceChangeEvent(UUID uuid, double oldBalance, double newBalance, double delta, String reason) {
        this.uuid = uuid;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.delta = delta;
        this.reason = reason;
    }

    public UUID getUuid() { return uuid; }
    public double getOldBalance() { return oldBalance; }
    public double getNewBalance() { return newBalance; }
    public double getDelta() { return delta; }
    public String getReason() { return reason; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
