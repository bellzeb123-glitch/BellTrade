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
import pl.bell.trade.model.ShopCategory;
import pl.bell.trade.model.ShopItemEntry;

import java.util.ArrayList;
import java.util.List;

public class ShopPriceEditorGUI {

    public static final int SLOT_BACK = 48;

    public enum View { CATEGORIES, ITEMS }

    public static class ShopPriceEditorHolder extends GuiHolder {
        private final View view;
        private final String categoryId;
        private final int page;

        public ShopPriceEditorHolder(View view, String categoryId, int page) {
            super(GuiHolder.Type.SHOP_PRICE_EDITOR);
            this.view = view;
            this.categoryId = categoryId;
            this.page = page;
        }

        public View getView() { return view; }
        public String getCategoryId() { return categoryId; }
        public int getPage() { return page; }
    }

    private final BellTrade plugin;

    public ShopPriceEditorGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void openCategories(Player player) {
        openCategories(player, 1);
    }

    public void openCategories(Player player, int page) {
        LangManager lang = plugin.getLangManager();
        List<ShopCategory> categories = plugin.getShopConfigManager().getCategories();
        int maxPage = Math.max(1, (int) Math.ceil(categories.size() / (double) SellShopGUI.CONTENT_SLOTS.length));
        page = Math.max(1, Math.min(page, maxPage));

        ShopPriceEditorHolder holder = new ShopPriceEditorHolder(View.CATEGORIES, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("admin.shop-editor-title-categories")));
        holder.setInventory(inv);
        fillGlass(inv);

        int offset = (page - 1) * SellShopGUI.CONTENT_SLOTS.length;
        for (int i = 0; i < SellShopGUI.CONTENT_SLOTS.length && offset + i < categories.size(); i++) {
            ShopCategory cat = categories.get(offset + i);
            inv.setItem(SellShopGUI.CONTENT_SLOTS[i], categoryIcon(cat, lang));
        }
        addNav(inv, lang, page, maxPage, false);
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
        int maxPage = Math.max(1, (int) Math.ceil(items.size() / (double) SellShopGUI.CONTENT_SLOTS.length));
        page = Math.max(1, Math.min(page, maxPage));

        ShopPriceEditorHolder holder = new ShopPriceEditorHolder(View.ITEMS, categoryId, page);
        Inventory inv = Bukkit.createInventory(holder, 54,
            lang.colorize(lang.getRaw("admin.shop-editor-title-items", "category",
                stripColor(lang.categoryDisplayName(category.getId(), category.getDisplayName())))));
        holder.setInventory(inv);
        fillGlass(inv);

        int offset = (page - 1) * SellShopGUI.CONTENT_SLOTS.length;
        for (int i = 0; i < SellShopGUI.CONTENT_SLOTS.length && offset + i < items.size(); i++) {
            ShopItemEntry entry = items.get(offset + i);
            inv.setItem(SellShopGUI.CONTENT_SLOTS[i], itemIcon(entry, lang));
        }
        addNav(inv, lang, page, maxPage, true);
        player.openInventory(inv);
    }

    public ShopCategory categoryAtSlot(ShopPriceEditorHolder holder, int rawSlot) {
        int index = SellShopGUI.slotIndex(rawSlot);
        if (index < 0) return null;
        List<ShopCategory> categories = plugin.getShopConfigManager().getCategories();
        int offset = (holder.getPage() - 1) * SellShopGUI.CONTENT_SLOTS.length + index;
        if (offset >= categories.size()) return null;
        return categories.get(offset);
    }

    public ShopItemEntry itemAtSlot(ShopPriceEditorHolder holder, int rawSlot) {
        int index = SellShopGUI.slotIndex(rawSlot);
        if (index < 0) return null;
        ShopCategory category = plugin.getShopConfigManager().getCategory(holder.getCategoryId());
        if (category == null) return null;
        List<ShopItemEntry> items = category.getItems();
        int offset = (holder.getPage() - 1) * SellShopGUI.CONTENT_SLOTS.length + index;
        if (offset >= items.size()) return null;
        return items.get(offset);
    }

    private ItemStack categoryIcon(ShopCategory category, LangManager lang) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(lang.categoryDisplayName(category.getId(), category.getDisplayName())));
        List<Component> lore = new ArrayList<>();
        lore.add(colorize(lang.getRaw("admin.shop-editor-category-lore",
            "count", String.valueOf(category.getItems().size()))));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack itemIcon(ShopItemEntry entry, LangManager lang) {
        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(lang.materialName(entry.getMaterial())));
        double live = plugin.getPriceEngine().getCurrentPrice(entry.getItemKey());
        List<Component> lore = new ArrayList<>();
        lore.add(colorize(lang.getRaw("admin.shop-editor-lore-base", "price", String.valueOf(entry.getBasePrice()))));
        lore.add(colorize(lang.getRaw("admin.shop-editor-lore-minmax",
            "min", String.valueOf(entry.getMinPrice()),
            "max", String.valueOf(entry.getMaxPrice()))));
        lore.add(colorize(lang.getRaw("admin.shop-editor-lore-live",
            "price", plugin.getCurrencyManager().format(live))));
        lore.add(Component.empty());
        lore.add(colorize(lang.getRaw("admin.shop-editor-lore-edit")));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addNav(Inventory inv, LangManager lang, int page, int maxPage, boolean itemsView) {
        if (page > 1) {
            inv.setItem(SellShopGUI.SLOT_PREV, icon(Material.ARROW, lang.getRaw("shop.gui-prev"), List.of()));
        }
        if (page < maxPage) {
            inv.setItem(SellShopGUI.SLOT_NEXT, icon(Material.ARROW, lang.getRaw("shop.gui-next"), List.of()));
        }
        inv.setItem(SellShopGUI.SLOT_INFO, icon(Material.BOOK, lang.getRaw("shop.gui-page",
            "page", String.valueOf(page), "max", String.valueOf(maxPage)), List.of()));
        inv.setItem(SLOT_BACK, icon(Material.ARROW,
            lang.getRaw(itemsView ? "shop.gui-back" : "admin.gui-back-admin"), List.of()));
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
        for (int s : SellShopGUI.CONTENT_SLOTS) inv.setItem(s, null);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private String stripColor(String text) {
        return text.replaceAll("(?i)&[0-9a-fk-or]", "");
    }
}
