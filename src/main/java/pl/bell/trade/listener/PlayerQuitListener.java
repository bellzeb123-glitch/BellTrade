package pl.bell.trade.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.bell.trade.BellTrade;
import pl.bell.trade.trade.TradeManager;

public class PlayerQuitListener implements Listener {

    private final BellTrade plugin;
    private final TradeManager tradeManager;

    public PlayerQuitListener(BellTrade plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tradeManager.onPlayerQuit(event.getPlayer());
        plugin.getMarketSellFlow().onPlayerQuit(event.getPlayer());
    }
}
