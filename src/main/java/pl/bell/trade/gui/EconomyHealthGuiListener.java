package pl.bell.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.bell.trade.BellTrade;

public class EconomyHealthGuiListener implements Listener {

    private final BellTrade plugin;

    public EconomyHealthGuiListener(BellTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof EconomyHealthGUI.EconomyHealthHolder)) return;

        event.setCancelled(true);
        if (event.getRawSlot() == EconomyHealthGUI.SLOT_BACK) {
            plugin.getAdminGUI().openFor(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EconomyHealthGUI.EconomyHealthHolder) {
            event.setCancelled(true);
        }
    }
}
