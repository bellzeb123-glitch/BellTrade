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
import pl.bell.trade.model.Listing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MarketGUI {

    public static final int[] LISTING_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static final int SLOT_PREV = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_MY = 47;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_SELL = 51;

    public enum View { BROWSE, MY }

    public static class MarketHolder extends GuiHolder {
        private final View view;
        private final int page;
        private final String materialFilter;

        public MarketHolder(View view, int page, String materialFilter) {
            super(GuiHolder.Type.MARKET);
            this.view = view;
            this.page = page;
            this.materialFilter = materialFilter;
        }

        public View getView() { return view; }
        public int getPage() { return page; }
        public String getMaterialFilter() { return materialFilter; }
    }

    private final BellTrade plugin;

    public MarketGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void openBrowse(Player player, int page) {
        openBrowse(player, page, null);
    }

    public void openBrowse(Player player, int page, String materialFilter) {
        int maxPage = plugin.getListingManager().getTotalPages(materialFilter);
        page = Math.max(1, Math.min(page, maxPage));

        LangManager lang = plugin.getLangManager();
        MarketHolder holder = new MarketHolder(View.BROWSE, page, materialFilter);
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("market.gui-title-browse")));
        holder.setInventory(inv);

        fillGlass(inv);
        List<Listing> listings = plugin.getListingManager().getBrowsePage(page, materialFilter);
        for (int i = 0; i < LISTING_SLOTS.length && i < listings.size(); i++) {
            inv.setItem(LISTING_SLOTS[i], listingIcon(listings.get(i), lang, false));
        }

        addNav(inv, lang, page, maxPage, View.BROWSE);
        player.openInventory(inv);
    }

    public void openMy(Player player) {
        LangManager lang = plugin.getLangManager();
        MarketHolder holder = new MarketHolder(View.MY, 1, null);
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("market.gui-title-my")));
        holder.setInventory(inv);

        fillGlass(inv);
        List<Listing> listings = plugin.getListingManager().getPlayerListings(player.getUniqueId());
        for (int i = 0; i < LISTING_SLOTS.length && i < listings.size(); i++) {
            inv.setItem(LISTING_SLOTS[i], listingIcon(listings.get(i), lang, true));
        }

        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("market.gui-back"), List.of()));
        inv.setItem(SLOT_SELL, icon(Material.EMERALD, lang.getRaw("market.gui-sell"), lang.getList("market.gui-sell-lore")));
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("market.gui-my-info",
            "count", String.valueOf(listings.size()),
            "max", String.valueOf(plugin.getListingManager().getMaxListings(player))), List.of()));

        player.openInventory(inv);
    }

    private void addNav(Inventory inv, LangManager lang, int page, int maxPage, View view) {
        if (page > 1) {
            inv.setItem(SLOT_PREV, icon(Material.ARROW, lang.getRaw("market.gui-prev"), List.of()));
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, icon(Material.ARROW, lang.getRaw("market.gui-next"), List.of()));
        }
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("market.gui-page",
            "page", String.valueOf(page), "max", String.valueOf(maxPage)), List.of()));
        inv.setItem(SLOT_MY, icon(Material.CHEST, lang.getRaw("market.gui-my-listings"), List.of()));
        inv.setItem(SLOT_SELL, icon(Material.EMERALD, lang.getRaw("market.gui-sell"), lang.getList("market.gui-sell-lore")));
        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("menu.gui-back-main"), List.of()));
    }

    public ItemStack listingIcon(Listing listing, LangManager lang, boolean myView) {
        ItemStack display = listing.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String sellerName = Bukkit.getOfflinePlayer(listing.getSellerUuid()).getName();
        if (sellerName == null) sellerName = "?";

        List<Component> lore = new ArrayList<>();
        lore.add(colorize(lang.getRaw("market.gui-lore-id", "id", String.valueOf(listing.getId()))));
        lore.add(colorize(lang.getRaw("market.gui-lore-seller", "seller", sellerName)));
        lore.add(colorize(lang.getRaw("market.gui-lore-price",
            "price", plugin.getCurrencyManager().format(listing.getPrice()))));
        lore.add(colorize(lang.getRaw("market.gui-lore-expires",
            "time", formatTimeLeft(listing.getExpiresAt() - System.currentTimeMillis()))));
        if (myView) {
            lore.add(Component.empty());
            lore.add(colorize(lang.getRaw("market.gui-lore-cancel-hint")));
        } else {
            lore.add(Component.empty());
            lore.add(colorize(lang.getRaw("market.gui-lore-buy-hint")));
        }
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    public long listingIdAtSlot(MarketHolder holder, int rawSlot, Player player) {
        int index = slotIndex(rawSlot);
        if (index < 0) return -1;
        List<Listing> listings = holder.getView() == View.MY
            ? plugin.getListingManager().getPlayerListings(player.getUniqueId())
            : plugin.getListingManager().getBrowsePage(holder.getPage(), holder.getMaterialFilter());
        if (index >= listings.size()) return -1;
        return listings.get(index).getId();
    }

    public static int slotIndex(int rawSlot) {
        for (int i = 0; i < LISTING_SLOTS.length; i++) {
            if (LISTING_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }

    private String formatTimeLeft(long millis) {
        if (millis <= 0) return "0m";
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private ItemStack icon(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name));
        if (!loreLines.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) lore.add(colorize(line));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);
        for (int s : LISTING_SLOTS) inv.setItem(s, null);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
