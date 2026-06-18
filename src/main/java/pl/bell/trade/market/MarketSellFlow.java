package pl.bell.trade.market;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI sell flow: Sell button → click item in inventory → type price in chat.
 */
public class MarketSellFlow {

    private enum Phase { SELECT_ITEM, ENTER_PRICE }

    private final BellTrade plugin;
    private final Map<UUID, Phase> phase = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> pendingItems = new ConcurrentHashMap<>();

    public MarketSellFlow(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void startItemSelect(Player player) {
        if (!player.hasPermission("belltrade.market.sell")) {
            player.sendMessage(plugin.getLangManager().component("no-permission"));
            return;
        }
        phase.put(player.getUniqueId(), Phase.SELECT_ITEM);
        player.sendMessage(plugin.getLangManager().component("market.sell-select-item"));
    }

    public boolean isSelectingItem(Player player) {
        return phase.get(player.getUniqueId()) == Phase.SELECT_ITEM;
    }

    public boolean isAwaitingChat(Player player) {
        return phase.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        phase.remove(player.getUniqueId());
        pendingItems.remove(player.getUniqueId());
        player.sendMessage(plugin.getLangManager().component("market.sell-cancelled"));
    }

    public void clear(Player player) {
        phase.remove(player.getUniqueId());
        pendingItems.remove(player.getUniqueId());
    }

    public void onPlayerQuit(org.bukkit.entity.Player player) {
        ItemStack pending = pendingItems.remove(player.getUniqueId());
        phase.remove(player.getUniqueId());
        if (pending != null) {
            plugin.getListingManager().returnItemToPlayer(player, pending);
        }
    }

    /**
     * @param singleItem true = one item (RMB), false = whole stack (LMB)
     */
    public void onItemPicked(Player player, ItemStack source, boolean singleItem) {
        if (source == null || source.getType().isAir()) return;

        int take = singleItem ? 1 : source.getAmount();
        ItemStack toSell = source.clone();
        toSell.setAmount(take);

        source.setAmount(source.getAmount() - take);
        if (source.getAmount() <= 0) {
            source.setType(org.bukkit.Material.AIR);
        }

        pendingItems.put(player.getUniqueId(), toSell);
        phase.put(player.getUniqueId(), Phase.ENTER_PRICE);

        player.closeInventory();
        LangManager lang = plugin.getLangManager();
        player.sendMessage(lang.component("market.sell-enter-price",
            "item", plugin.getLangManager().materialName(toSell.getType()),
            "amount", String.valueOf(toSell.getAmount())));
    }

    public boolean handleChatInput(Player player, String message) {
        Phase p = phase.get(player.getUniqueId());
        if (p == null) return false;

        LangManager lang = plugin.getLangManager();
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("anuluj")) {
            ItemStack pending = pendingItems.remove(player.getUniqueId());
            phase.remove(player.getUniqueId());
            if (pending != null) {
                plugin.getListingManager().returnItemToPlayer(player, pending);
            }
            player.sendMessage(lang.component("market.sell-cancelled"));
            return true;
        }

        if (p != Phase.ENTER_PRICE) return false;

        double price;
        try {
            price = Double.parseDouble(message.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.component("invalid-amount"));
            return true;
        }

        ItemStack item = pendingItems.remove(player.getUniqueId());
        phase.remove(player.getUniqueId());
        if (item == null) return true;

        plugin.getListingManager().createListingFromItem(player, item, price);
        return true;
    }

    private String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        }
        return plugin.getLangManager().materialName(item.getType());
    }
}
