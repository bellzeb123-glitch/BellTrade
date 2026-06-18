package pl.bell.trade.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopPriceEditFlow {

    private final BellTrade plugin;
    private final Map<UUID, PendingEdit> pending = new ConcurrentHashMap<>();

    private record PendingEdit(String categoryId, Material material) {}

    public ShopPriceEditFlow(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void startEdit(Player player, String categoryId, Material material) {
        var entry = plugin.getShopConfigManager().getItemEntry(material);
        if (entry == null) return;

        pending.put(player.getUniqueId(), new PendingEdit(categoryId, material));
        player.closeInventory();
        LangManager lang = plugin.getLangManager();
        player.sendMessage(lang.component("admin.shop-price-prompt",
            "item", lang.materialName(material),
            "base", String.valueOf(entry.getBasePrice()),
            "min", String.valueOf(entry.getMinPrice()),
            "max", String.valueOf(entry.getMaxPrice())));
    }

    public boolean isAwaitingChat(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public boolean handleChatInput(Player player, String message) {
        PendingEdit edit = pending.get(player.getUniqueId());
        if (edit == null) return false;

        LangManager lang = plugin.getLangManager();
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("anuluj")) {
            pending.remove(player.getUniqueId());
            player.sendMessage(lang.component("admin.prompt-cancelled"));
            plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getShopPriceEditorGUI().openCategory(player, edit.categoryId()));
            return true;
        }

        String[] parts = message.replace(",", ".").trim().split("\\s+");
        if (parts.length < 3) {
            player.sendMessage(lang.component("admin.shop-price-format"));
            return true;
        }

        try {
            double base = Double.parseDouble(parts[0]);
            double min = Double.parseDouble(parts[1]);
            double max = Double.parseDouble(parts[2]);
            if (base <= 0 || min < 0 || max < min) {
                player.sendMessage(lang.component("admin.shop-price-invalid"));
                return true;
            }

            boolean ok = plugin.getShopConfigManager().updateItemPrice(
                edit.categoryId(), edit.material(), base, min, max);
            pending.remove(player.getUniqueId());

            if (!ok) {
                player.sendMessage(lang.component("admin.shop-price-failed"));
                return true;
            }

            player.sendMessage(lang.component("admin.shop-price-saved",
                "item", lang.materialName(edit.material()),
                "base", String.valueOf(base),
                "min", String.valueOf(min),
                "max", String.valueOf(max)));

            plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getShopPriceEditorGUI().openCategory(player, edit.categoryId()));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.component("admin.shop-price-format"));
        }
        return true;
    }
}
