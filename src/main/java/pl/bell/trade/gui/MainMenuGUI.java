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

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI {

    public static final int SLOT_MARKET = 11;
    public static final int SLOT_TRADE = 13;
    public static final int SLOT_SHOP = 15;
    public static final int SLOT_BALANCE = 22;

    public static class MainMenuHolder extends GuiHolder {
        public MainMenuHolder() {
            super(GuiHolder.Type.MAIN_MENU);
        }
    }

    private final BellTrade plugin;

    public MainMenuGUI(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        LangManager lang = plugin.getLangManager();
        MainMenuHolder holder = new MainMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("menu.gui-title")));
        holder.setInventory(inv);
        fillGlass(inv);

        inv.setItem(SLOT_MARKET, item(Material.CHEST, lang.getRaw("menu.market-name"), lang.getList("menu.market-lore")));
        inv.setItem(SLOT_TRADE, item(Material.PLAYER_HEAD, lang.getRaw("menu.trade-name"), lang.getList("menu.trade-lore")));
        inv.setItem(SLOT_SHOP, item(Material.EMERALD, lang.getRaw("menu.shop-name"), lang.getList("menu.shop-lore")));
        inv.setItem(SLOT_BALANCE, item(Material.GOLD_INGOT, lang.getRaw("menu.balance-name"),
            List.of(
                lang.getRaw("menu.balance-amount", "amount", plugin.getCurrencyManager().format(
                    plugin.getCurrencyManager().getBalance(player))),
                "",
                lang.getRaw("menu.balance-hint")
            )));

        player.openInventory(inv);
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
