package pl.bell.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.bell.trade.BellTrade;

public class MainMenuGuiListener implements Listener {

    private final BellTrade plugin;

    public MainMenuGuiListener(BellTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MainMenuGUI.MainMenuHolder)) return;

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= 54) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (raw == MainMenuGUI.SLOT_MARKET) {
            if (!player.hasPermission("belltrade.market")) {
                player.sendMessage(plugin.getLangManager().component("no-permission"));
                return;
            }
            plugin.getMarketGUI().openBrowse(player, 1);
        } else if (raw == MainMenuGUI.SLOT_TRADE) {
            player.closeInventory();
            player.sendMessage(plugin.getLangManager().component("menu.trade-hint"));
        } else if (raw == MainMenuGUI.SLOT_SHOP) {
            if (!player.hasPermission("belltrade.shop")) {
                player.sendMessage(plugin.getLangManager().component("no-permission"));
                return;
            }
            plugin.getSellShopGUI().openCategories(player);
        } else if (raw == MainMenuGUI.SLOT_BALANCE) {
            player.closeInventory();
            player.sendMessage(plugin.getLangManager().component("balance-self",
                "amount", plugin.getCurrencyManager().format(plugin.getCurrencyManager().getBalance(player))));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MainMenuGUI.MainMenuHolder) {
            event.setCancelled(true);
        }
    }
}
