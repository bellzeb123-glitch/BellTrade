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
import pl.bell.trade.model.ShopCategory;
import pl.bell.trade.model.ShopItemEntry;

import java.util.ArrayList;
import java.util.List;

public class SellShopGUI {

    public static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;

    public enum View { CATEGORIES, ITEMS }

    public static class SellShopHolder extends GuiHolder {
        private final View view;
        private final String categoryId;
        private final int page;

        public SellShopHolder(View view, String categoryId, int page) {
            super(GuiHolder.Type.SELL_SHOP);
            this.view = view;
            this.categoryId = categoryId;
            this.page = page;
        }

        public View getView() { return view; }
        public String getCategoryId() { return categoryId; }
        public int getPage() { return page; }
    }

    private final BellTrade plugin;

    public SellShopGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void openCategories(Player player) {
        openCategories(player, 1);
    }

    public void openCategories(Player player, int page) {
        LangManager lang = plugin.getLangManager();
        List<ShopCategory> categories = plugin.getShopConfigManager().getCategories();
        int maxPage = Math.max(1, (int) Math.ceil(categories.size() / (double) CONTENT_SLOTS.length));
        page = Math.max(1, Math.min(page, maxPage));

        SellShopHolder holder = new SellShopHolder(View.CATEGORIES, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("shop.gui-title-categories")));
        holder.setInventory(inv);

        fillGlass(inv);
        int offset = (page - 1) * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && offset + i < categories.size(); i++) {
            ShopCategory cat = categories.get(offset + i);
            inv.setItem(CONTENT_SLOTS[i], categoryIcon(cat, lang));
        }

        addCategoryNav(inv, lang, page, maxPage);
        player.openInventory(inv);
    }

    public void openCategory(Player player, String categoryId) {
        openCategory(player, categoryId, 1);
    }

    public void openCategory(Player player, String categoryId, int page) {
        ShopCategory category = plugin.getShopConfigManager().getCategory(categoryId);
        if (category == null) {
            openCategories(player);
            return;
        }

        LangManager lang = plugin.getLangManager();
        List<ShopItemEntry> items = category.getItems();
        int maxPage = Math.max(1, (int) Math.ceil(items.size() / (double) CONTENT_SLOTS.length));
        page = Math.max(1, Math.min(page, maxPage));

        SellShopHolder holder = new SellShopHolder(View.ITEMS, categoryId, page);
        Inventory inv = Bukkit.createInventory(holder, 54,
            lang.colorize(lang.getRaw("shop.gui-title-items", "category",
                stripColor(lang.categoryDisplayName(category.getId(), category.getDisplayName())))));
        holder.setInventory(inv);

        fillGlass(inv);
        int offset = (page - 1) * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && offset + i < items.size(); i++) {
            ShopItemEntry entry = items.get(offset + i);
            inv.setItem(CONTENT_SLOTS[i], itemIcon(player, entry, lang));
        }

        addItemNav(inv, lang, category, page, maxPage);
        player.openInventory(inv);
    }

    public ShopCategory categoryAtSlot(SellShopHolder holder, int rawSlot) {
        int index = slotIndex(rawSlot);
        if (index < 0) return null;
        List<ShopCategory> categories = plugin.getShopConfigManager().getCategories();
        int offset = (holder.getPage() - 1) * CONTENT_SLOTS.length + index;
        if (offset >= categories.size()) return null;
        return categories.get(offset);
    }

    public ShopItemEntry itemAtSlot(SellShopHolder holder, int rawSlot) {
        int index = slotIndex(rawSlot);
        if (index < 0) return null;
        ShopCategory category = plugin.getShopConfigManager().getCategory(holder.getCategoryId());
        if (category == null) return null;
        List<ShopItemEntry> items = category.getItems();
        int offset = (holder.getPage() - 1) * CONTENT_SLOTS.length + index;
        if (offset >= items.size()) return null;
        return items.get(offset);
    }

    public static int slotIndex(int rawSlot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }

    private ItemStack categoryIcon(ShopCategory category, LangManager lang) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(lang.categoryDisplayName(category.getId(), category.getDisplayName())));
        List<Component> lore = new ArrayList<>();
        lore.add(colorize(lang.getRaw("shop.gui-category-lore",
            "count", String.valueOf(category.getItems().size()))));
        lore.add(Component.empty());
        lore.add(colorize(lang.getRaw("shop.gui-category-open")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack itemIcon(Player player, ShopItemEntry entry, LangManager lang) {
        Material mat = entry.getMaterial();
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(lang.materialName(mat)));
        double unit = pl.bell.trade.api.BellTradeAPI.get().getShopPriceEngine()
            .getCurrentPrice(player, entry.getItemKey(), null);
        int owned = plugin.getShopManager().countInInventory(player, mat);

        List<Component> lore = new ArrayList<>();
        int unitSize = entry.getUnitSize();
        if (unitSize > 1) {
            lore.add(colorize(lang.getRaw("shop.gui-lore-price-unit",
                "price", plugin.getCurrencyManager().format(unit),
                "unit", String.valueOf(unitSize))));
        } else {
            lore.add(colorize(lang.getRaw("shop.gui-lore-price",
                "price", plugin.getCurrencyManager().format(unit))));
        }
        lore.add(colorize(lang.getRaw("shop.gui-lore-owned", "amount", String.valueOf(owned))));
        lore.add(Component.empty());
        lore.add(colorize(lang.getRaw("shop.gui-lore-sell-all")));
        lore.add(colorize(lang.getRaw("shop.gui-lore-sell-stack")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addCategoryNav(Inventory inv, LangManager lang, int page, int maxPage) {
        if (page > 1) {
            inv.setItem(SLOT_PREV, icon(Material.ARROW, lang.getRaw("shop.gui-prev"), List.of()));
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, icon(Material.ARROW, lang.getRaw("shop.gui-next"), List.of()));
        }
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("shop.gui-page",
            "page", String.valueOf(page), "max", String.valueOf(maxPage)), List.of()));
        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("menu.gui-back-main"), List.of()));
    }

    private void addItemNav(Inventory inv, LangManager lang, ShopCategory category, int page, int maxPage) {
        if (page > 1) {
            inv.setItem(SLOT_PREV, icon(Material.ARROW, lang.getRaw("shop.gui-prev"), List.of()));
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, icon(Material.ARROW, lang.getRaw("shop.gui-next"), List.of()));
        }
        inv.setItem(SLOT_BACK, icon(Material.ARROW, lang.getRaw("shop.gui-back"), List.of()));
        inv.setItem(SLOT_INFO, icon(Material.BOOK, lang.getRaw("shop.gui-page",
            "page", String.valueOf(page), "max", String.valueOf(maxPage)), List.of()));
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
        for (int s : CONTENT_SLOTS) inv.setItem(s, null);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private String stripColor(String text) {
        return text.replaceAll("(?i)&[0-9a-fk-or]", "");
    }
}
