package pl.bell.trade.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import pl.bell.trade.model.TradeSession;

public class TradeCompleteEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player playerB;
    private final TradeSession session;

    public TradeCompleteEvent(Player playerA, Player playerB, TradeSession session) {
        super(playerA, false);
        this.playerB = playerB;
        this.session = session;
    }

    public Player getPlayerA() { return getPlayer(); }
    public Player getPlayerB() { return playerB; }
    public TradeSession getSession() { return session; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
