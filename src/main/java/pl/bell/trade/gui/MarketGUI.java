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
import pl.bell.trade.model.ExpiredMailboxEntry;
import pl.bell.trade.model.Listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MarketGUI {

    public static final int[] LISTING_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static final int SLOT_PREV = 45;
    public static final int SLOT_EXPIRED = 46;
    public static final int SLOT_MY = 47;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_SELL = 51;
    public static final int SLOT_NEXT = 53;

    public enum View { BROWSE, MY, EXPIRED }

    public static class MarketHolder extends GuiHolder {
        private final View view;
        private final int page;
        private final String materialFilter;
        private final Map<Integer, Long> slotToListingId = new HashMap<>();
        private final Map<Integer, Long> slotToExpiredId = new HashMap<>();

        public MarketHolder(View view, int page, String materialFilter) {
            super(GuiHolder.Type.MARKET);
            this.view = view;
            this.page = page;
            this.materialFilter = materialFilter;
        }

        public View getView() { return view; }
        public int getPage() { return page; }
        public String getMaterialFilter() { return materialFilter; }
        public Map<Integer, Long> getSlotToListingId() { return slotToListingId; }
        public Map<Integer, Long> getSlotToExpiredId() { return slotToExpiredId; }
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
            holder.getSlotToListingId().put(LISTING_SLOTS[i], listings.get(i).getId());
        }

        addBrowseNav(inv, lang, player, page, maxPage);
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
            holder.getSlotToListingId().put(LISTING_SLOTS[i], listings.get(i).getId());
        }

        addPlayerNav(inv, lang, player, View.MY, 1, 1);
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("market.gui-my-info",
            "count", String.valueOf(listings.size()),
            "max", String.valueOf(plugin.getListingManager().getMaxListings(player))), List.of()));

        player.openInventory(inv);
    }

    public void openExpired(Player player, int page) {
        int maxPage = plugin.getExpiredMailboxManager().getTotalPages(player.getUniqueId());
        page = Math.max(1, Math.min(page, maxPage));

        LangManager lang = plugin.getLangManager();
        MarketHolder holder = new MarketHolder(View.EXPIRED, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("market.gui-title-expired")));
        holder.setInventory(inv);

        fillGlass(inv);
        List<ExpiredMailboxEntry> entries = plugin.getExpiredMailboxManager().getPage(player.getUniqueId(), page);
        for (int i = 0; i < LISTING_SLOTS.length && i < entries.size(); i++) {
            inv.setItem(LISTING_SLOTS[i], expiredIcon(entries.get(i), lang));
            holder.getSlotToExpiredId().put(LISTING_SLOTS[i], entries.get(i).getId());
        }

        addPlayerNav(inv, lang, player, View.EXPIRED, page, maxPage);
        int total = plugin.getExpiredMailboxManager().countPending(player.getUniqueId());
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("market.gui-expired-info",
            "count", String.valueOf(total)), List.of()));

        player.openInventory(inv);
    }

    private void addBrowseNav(Inventory inv, LangManager lang, Player player, int page, int maxPage) {
        if (page > 1) {
            inv.setItem(SLOT_PREV, icon(Material.ARROW, lang.getRaw("market.gui-prev"), List.of()));
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, icon(Material.ARROW, lang.getRaw("market.gui-next"), List.of()));
        }
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("market.gui-page",
            "page", String.valueOf(page), "max", String.valueOf(maxPage)), List.of()));
        inv.setItem(SLOT_MY, tabIcon(lang, player, View.MY, false));
        inv.setItem(SLOT_EXPIRED, tabIcon(lang, player, View.EXPIRED, false));
        inv.setItem(SLOT_SELL, icon(Material.EMERALD, lang.getRaw("market.gui-sell"), lang.getList("market.gui-sell-lore")));
        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("menu.gui-back-main"), List.of()));
    }

    private void addPlayerNav(Inventory inv, LangManager lang, Player player, View active, int page, int maxPage) {
        inv.setItem(SLOT_MY, tabIcon(lang, player, View.MY, active == View.MY));
        inv.setItem(SLOT_EXPIRED, tabIcon(lang, player, View.EXPIRED, active == View.EXPIRED));
        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("market.gui-back"), List.of()));
        inv.setItem(SLOT_SELL, icon(Material.EMERALD, lang.getRaw("market.gui-sell"), lang.getList("market.gui-sell-lore")));

        if (active == View.EXPIRED) {
            if (page > 1) {
                inv.setItem(SLOT_PREV, icon(Material.ARROW, lang.getRaw("market.gui-prev"), List.of()));
            }
            if (page < maxPage) {
                inv.setItem(SLOT_NEXT, icon(Material.ARROW, lang.getRaw("market.gui-next"), List.of()));
            }
        }
    }

    private ItemStack tabIcon(LangManager lang, Player player, View tab, boolean selected) {
        int expiredCount = plugin.getExpiredMailboxManager().countPending(player.getUniqueId());
        Material mat = tab == View.MY
            ? (selected ? Material.CHEST : Material.ENDER_CHEST)
            : (selected ? Material.CHEST_MINECART : Material.POISONOUS_POTATO);

        String nameKey = tab == View.MY ? "market.gui-my-listings" : "market.gui-expired-listings";
        List<String> lore = new ArrayList<>();
        if (tab == View.EXPIRED && expiredCount > 0) {
            lore.add(lang.getRaw("market.gui-expired-count", "count", String.valueOf(expiredCount)));
        }
        if (selected) {
            lore.add(lang.getRaw("market.gui-tab-active"));
        } else {
            lore.add(lang.getRaw("market.gui-tab-open"));
        }
        return icon(mat, lang.getRaw(nameKey), lore);
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

    public ItemStack expiredIcon(ExpiredMailboxEntry entry, LangManager lang) {
        ItemStack display = entry.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = new ArrayList<>();
        if (entry.getListingId() > 0) {
            lore.add(colorize(lang.getRaw("market.gui-lore-id", "id", String.valueOf(entry.getListingId()))));
        }
        lore.add(colorize(lang.getRaw("market.gui-lore-expired-at",
            "time", formatTimeAgo(System.currentTimeMillis() - entry.getExpiredAt()))));
        lore.add(Component.empty());
        lore.add(colorize(lang.getRaw("market.gui-lore-claim-hint")));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    public long listingIdAtSlot(MarketHolder holder, int rawSlot, Player player) {
        return holder.getSlotToListingId().getOrDefault(rawSlot, -1L);
    }

    public long expiredIdAtSlot(MarketHolder holder, int rawSlot, Player player) {
        if (holder.getView() != View.EXPIRED) return -1;
        return holder.getSlotToExpiredId().getOrDefault(rawSlot, -1L);
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

    private String formatTimeAgo(long millis) {
        if (millis < 60_000) return "<1m";
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 24) return (hours / 24) + "d";
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
