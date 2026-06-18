package pl.bell.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.bell.trade.BellTrade;
import pl.bell.trade.model.ShopCategory;
import pl.bell.trade.model.ShopItemEntry;

public class ShopPriceEditorGuiListener implements Listener {

    private final BellTrade plugin;

    public ShopPriceEditorGuiListener(BellTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ShopPriceEditorGUI.ShopPriceEditorHolder holder)) return;
        if (!player.hasPermission("belltrade.admin")) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= 54) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        ShopPriceEditorGUI gui = plugin.getShopPriceEditorGUI();

        if (raw == ShopPriceEditorGUI.SLOT_BACK) {
            if (holder.getView() == ShopPriceEditorGUI.View.ITEMS) {
                gui.openCategories(player);
            } else {
                plugin.getAdminGUI().openFor(player);
            }
            return;
        }

        if (holder.getView() == ShopPriceEditorGUI.View.CATEGORIES) {
            if (raw == SellShopGUI.SLOT_PREV && holder.getPage() > 1) {
                gui.openCategories(player, holder.getPage() - 1);
                return;
            }
            if (raw == SellShopGUI.SLOT_NEXT) {
                int max = maxCategoryPages();
                if (holder.getPage() < max) gui.openCategories(player, holder.getPage() + 1);
                return;
            }
            ShopCategory category = gui.categoryAtSlot(holder, raw);
            if (category != null) gui.openCategory(player, category.getId());
            return;
        }

        if (raw == SellShopGUI.SLOT_PREV && holder.getPage() > 1) {
            gui.openCategory(player, holder.getCategoryId(), holder.getPage() - 1);
            return;
        }
        if (raw == SellShopGUI.SLOT_NEXT) {
            int max = maxItemPages(holder.getCategoryId());
            if (holder.getPage() < max) gui.openCategory(player, holder.getCategoryId(), holder.getPage() + 1);
            return;
        }

        ShopItemEntry entry = gui.itemAtSlot(holder, raw);
        if (entry != null) {
            plugin.getShopPriceEditFlow().startEdit(player, holder.getCategoryId(), entry.getMaterial());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopPriceEditorGUI.ShopPriceEditorHolder) {
            event.setCancelled(true);
        }
    }

    private int maxCategoryPages() {
        int size = plugin.getShopConfigManager().getCategories().size();
        return Math.max(1, (int) Math.ceil(size / (double) SellShopGUI.CONTENT_SLOTS.length));
    }

    private int maxItemPages(String categoryId) {
        var category = plugin.getShopConfigManager().getCategory(categoryId);
        if (category == null) return 1;
        return Math.max(1, (int) Math.ceil(category.getItems().size() / (double) SellShopGUI.CONTENT_SLOTS.length));
    }
}
