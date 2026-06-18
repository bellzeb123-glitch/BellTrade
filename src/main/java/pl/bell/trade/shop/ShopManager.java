package pl.bell.trade.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.engine.PriceEngine;
import pl.bell.trade.event.ShopSellEvent;
import pl.bell.trade.model.ItemKey;
import pl.bell.trade.model.ShopItemEntry;

import java.util.HashMap;

public class ShopManager {

    private final BellTrade plugin;

    public ShopManager(BellTrade plugin) {
        this.plugin = plugin;
    }

    public int countInInventory(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != material) continue;
            if (!isPlain(stack)) continue;
            total += stack.getAmount();
        }
        return total;
    }

    public void sellMaterial(Player player, Material material, int maxAmount, boolean oneStack) {
        LangManager lang = plugin.getLangManager();
        if (!player.hasPermission("belltrade.shop")) {
            player.sendMessage(lang.component("no-permission"));
            return;
        }

        ShopItemEntry entry = plugin.getShopConfigManager().getItemEntry(material);
        if (entry == null || plugin.getShopConfigManager().isBlacklisted(material)) {
            player.sendMessage(lang.component("shop.not-sellable"));
            return;
        }

        int available = countInInventory(player, material);
        if (available <= 0) {
            player.sendMessage(lang.component("shop.nothing-to-sell"));
            return;
        }

        int toSell = oneStack ? Math.min(64, available) : available;
        if (maxAmount > 0) toSell = Math.min(toSell, maxAmount);

        ItemKey key = ItemKey.of(material);
        PriceEngine engine = plugin.getPriceEngine();
        double unitPrice = engine.getCurrentPrice(key);
        double basePrice = entry.getBasePrice();
        double total = unitPrice * toSell;

        ItemStack removed = removePlainItems(player, material, toSell);
        if (removed.getAmount() <= 0) {
            player.sendMessage(lang.component("shop.nothing-to-sell"));
            return;
        }

        int actualAmount = removed.getAmount();
        total = unitPrice * actualAmount;

        ShopSellEvent event = new ShopSellEvent(player, key, actualAmount, basePrice, unitPrice);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            giveOrDrop(player, removed);
            player.sendMessage(lang.component("shop.sell-cancelled"));
            return;
        }

        if (!plugin.getCurrencyManager().deposit(player.getUniqueId(), total, "shop-sell")) {
            giveOrDrop(player, removed);
            player.sendMessage(lang.component("shop.sell-failed"));
            return;
        }

        engine.recordSale(key, unitPrice, actualAmount);
        player.sendMessage(lang.component("shop.sold",
            "amount", String.valueOf(actualAmount),
            "item", lang.materialName(material),
            "price", plugin.getCurrencyManager().format(total),
            "unit", plugin.getCurrencyManager().format(unitPrice)));
    }

    public void sellFromHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || !isPlain(hand)) {
            player.sendMessage(plugin.getLangManager().component("shop.no-item"));
            return;
        }
        sellMaterial(player, hand.getType(), hand.getAmount(), false);
    }

    public void sellItemStack(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !isPlain(stack)) {
            player.sendMessage(plugin.getLangManager().component("shop.no-item"));
            return;
        }
        sellMaterial(player, stack.getType(), stack.getAmount(), false);
    }

    private ItemStack removePlainItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack sample = new ItemStack(material);
        for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType() != material || !isPlain(stack)) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            }
            remaining -= take;
        }
        int sold = amount - remaining;
        sample.setAmount(sold);
        return sample;
    }

    private boolean isPlain(ItemStack stack) {
        if (!stack.hasItemMeta()) return true;
        var meta = stack.getItemMeta();
        return !meta.hasDisplayName() && !meta.hasEnchants() && meta.lore() == null;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
    }
}
