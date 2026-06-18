package pl.bell.trade.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.bell.trade.BellTrade;
import pl.bell.trade.gui.AdminGUI;

public class MarketChatListener implements Listener {

    private final BellTrade plugin;
    private final AdminGUI adminGUI;

    public MarketChatListener(BellTrade plugin, AdminGUI adminGUI) {
        this.plugin = plugin;
        this.adminGUI = adminGUI;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getMarketSellFlow().isAwaitingChat(player)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getMarketSellFlow().handleChatInput(player, message));
            return;
        }

        if (plugin.getShopPriceEditFlow().isAwaitingChat(player)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getShopPriceEditFlow().handleChatInput(player, message));
            return;
        }

        if (adminGUI.isAwaitingInput(player)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            plugin.getServer().getScheduler().runTask(plugin,
                () -> adminGUI.handleChatInput(player, message));
        }
    }
}
