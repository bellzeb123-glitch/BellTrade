package pl.bell.trade.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.engine.EconomyHealthMonitor;

import java.util.ArrayList;
import java.util.List;

public class EconomyHealthGUI {

    public static final int SLOT_BACK = 49;

    public static class EconomyHealthHolder extends GuiHolder {
        public EconomyHealthHolder() {
            super(GuiHolder.Type.ECONOMY_HEALTH);
        }
    }

    private final BellTrade plugin;

    public EconomyHealthGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        LangManager lang = plugin.getLangManager();
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();

        EconomyHealthHolder holder = new EconomyHealthHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("admin.health-gui-title")));
        holder.setInventory(inv);
        fillGlass(inv);

        Material statusMat = switch (health.getStatus()) {
            case INFLATION -> Material.REDSTONE_BLOCK;
            case DEFLATION -> Material.LAPIS_BLOCK;
            case STABLE -> Material.EMERALD_BLOCK;
        };

        inv.setItem(13, item(statusMat,
            lang.getRaw("admin.health-status-" + health.getStatus().name().toLowerCase()),
            List.of(
                lang.getRaw("admin.health-inflation", "percent", formatPercent(health.getInflationPercent())),
                lang.getRaw("admin.health-velocity", "count", String.valueOf((int) health.getMoneyVelocity())),
                lang.getRaw("admin.health-shop-mult", "mult", String.format("%.2f", health.getShopHealthMultiplier())),
                lang.getRaw("admin.health-tax-bonus", "bonus", String.format("%.1f", health.getMarketTaxAdjustment()))
            )));

        inv.setItem(31, item(Material.BOOK,
            lang.getRaw("admin.health-details-name"),
            List.of(
                lang.getRaw("admin.health-total", "amount", plugin.getCurrencyManager().format(
                    plugin.getCurrencyManager().getTotalMoneyInCirculation())),
                lang.getRaw("admin.health-baseline", "amount", plugin.getCurrencyManager().format(health.getBaselineTotal())),
                lang.getRaw("admin.health-listings", "count", String.valueOf(health.getActiveListings())),
                lang.getRaw("admin.health-shop-sells", "count", String.valueOf(health.getShopSellsLast24h()))
            )));

        inv.setItem(SLOT_BACK, item(Material.ARROW, lang.getRaw("admin.gui-back-admin"), List.of()));
        player.openInventory(inv);
    }

    private String formatPercent(double percent) {
        return (percent >= 0 ? "+" : "") + String.format("%.1f", percent);
    }

    private ItemStack item(Material mat, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(colorize(name));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(colorize(line));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
