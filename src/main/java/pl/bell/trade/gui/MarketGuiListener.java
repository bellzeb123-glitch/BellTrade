package pl.bell.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.market.MarketSellFlow;

public class MarketGuiListener implements Listener {

    private final BellTrade plugin;

    public MarketGuiListener(BellTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MarketSellFlow sellFlow = plugin.getMarketSellFlow();

        if (event.getInventory().getHolder() instanceof MarketGUI.MarketHolder holder) {
            int raw = event.getRawSlot();

            if (sellFlow.isSelectingItem(player) && raw >= 54) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;
                boolean single = event.isRightClick();
                sellFlow.onItemPicked(player, clicked, single);
                return;
            }

            if (raw < 0 || raw >= 54) {
                if (event.isShiftClick()) event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            MarketGUI gui = plugin.getMarketGUI();

            if (raw == MarketGUI.SLOT_SELL) {
                sellFlow.startItemSelect(player);
                return;
            }

            if (MarketGUI.slotIndex(raw) >= 0) {
                long listingId = gui.listingIdAtSlot(holder, raw, player);
                if (listingId < 0) return;
                if (holder.getView() == MarketGUI.View.MY) {
                    plugin.getListingManager().cancelListing(player, listingId);
                    gui.openMy(player);
                } else {
                    plugin.getListingManager().purchase(player, listingId);
                    gui.openBrowse(player, holder.getPage(), holder.getMaterialFilter());
                }
                return;
            }

            if (raw == MarketGUI.SLOT_PREV && holder.getPage() > 1) {
                gui.openBrowse(player, holder.getPage() - 1, holder.getMaterialFilter());
            } else if (raw == MarketGUI.SLOT_NEXT) {
                int max = plugin.getListingManager().getTotalPages(holder.getMaterialFilter());
                if (holder.getPage() < max) {
                    gui.openBrowse(player, holder.getPage() + 1, holder.getMaterialFilter());
                }
            } else if (raw == MarketGUI.SLOT_MY) {
                sellFlow.clear(player);
                gui.openMy(player);
            } else if (raw == MarketGUI.SLOT_BACK) {
                sellFlow.clear(player);
                gui.openBrowse(player, 1, holder.getMaterialFilter());
            }
            return;
        }

        if (sellFlow.isSelectingItem(player)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            boolean single = event.isRightClick();
            sellFlow.onItemPicked(player, clicked, single);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MarketGUI.MarketHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MarketGUI.MarketHolder)) return;
        if (plugin.getMarketSellFlow().isSelectingItem(player)) {
            plugin.getMarketSellFlow().clear(player);
        }
    }
}
