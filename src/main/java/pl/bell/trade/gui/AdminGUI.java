package pl.bell.trade.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.CurrencyConfig;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.engine.EconomyHealthMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminGUI implements Listener {

    public static final int SLOT_HEALTH = 4;
    public static final int SLOT_MARKET_STATS = 10;
    public static final int SLOT_SHOP_EDITOR = 12;
    public static final int SLOT_ACTIVITY = 14;
    public static final int SLOT_HEALTH_DETAIL = 16;
    public static final int SLOT_GIVE = 19;
    public static final int SLOT_TAKE = 20;
    public static final int SLOT_SET = 21;
    public static final int SLOT_TOP = 22;
    public static final int SLOT_CURRENCY_NAME = 28;
    public static final int SLOT_CURRENCY_SYMBOL = 30;
    public static final int SLOT_LANG = 31;
    public static final int SLOT_RELOAD = 32;

    private static class AdminHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final BellTrade plugin;
    private final Map<UUID, String> awaitingInput = new HashMap<>();

    public AdminGUI(BellTrade plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openFor(Player admin) {
        AdminHolder holder = new AdminHolder();
        LangManager lang = plugin.getLangManager();
        CurrencyConfig cc = plugin.getCurrencyConfig();
        CurrencyManager eco = plugin.getCurrencyManager();
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();

        Inventory inv = Bukkit.createInventory(holder, 54, lang.colorize(lang.getRaw("admin.gui-title")));
        holder.setInventory(inv);
        fill(inv);

        Material healthMat = switch (health.getStatus()) {
            case INFLATION -> Material.REDSTONE;
            case DEFLATION -> Material.LAPIS_LAZULI;
            case STABLE -> Material.EMERALD;
        };

        inv.setItem(SLOT_HEALTH, item(healthMat,
            lang.getRaw("admin.gui-health-name"),
            List.of(
                lang.getRaw("admin.gui-stats-players", "count", String.valueOf(eco.getTrackedPlayerCount())),
                lang.getRaw("admin.gui-stats-total", "amount", eco.format(eco.getTotalMoneyInCirculation())),
                lang.getRaw("admin.gui-stats-currency", "currency", cc.getCurrencyName()),
                "",
                lang.getRaw("admin.gui-health-inflation", "percent", formatPercent(health.getInflationPercent())),
                lang.getRaw("admin.gui-health-status-" + health.getStatus().name().toLowerCase())
            )));

        inv.setItem(SLOT_MARKET_STATS, item(Material.BOOK,
            lang.getRaw("admin.gui-market-stats-name"),
            List.of(
                lang.getRaw("admin.gui-market-listings", "count", String.valueOf(health.getActiveListings())),
                lang.getRaw("admin.gui-market-tax", "tax", String.format("%.1f", plugin.getListingManager().getTaxPercent())),
                "",
                lang.getRaw("admin.gui-market-click")
            )));

        inv.setItem(SLOT_SHOP_EDITOR, item(Material.WRITABLE_BOOK,
            lang.getRaw("admin.gui-shop-editor-name"),
            lang.getList("admin.gui-shop-editor-lore")));

        inv.setItem(SLOT_ACTIVITY, item(Material.PAPER,
            lang.getRaw("admin.gui-activity-name"),
            List.of(
                lang.getRaw("admin.gui-activity-shop", "count", String.valueOf(health.getShopSellsLast24h())),
                lang.getRaw("admin.gui-activity-velocity", "count", String.valueOf((int) health.getMoneyVelocity())),
                "",
                lang.getRaw("admin.gui-activity-click")
            )));

        inv.setItem(SLOT_HEALTH_DETAIL, item(Material.HEART_OF_THE_SEA,
            lang.getRaw("admin.gui-health-detail-name"),
            lang.getList("admin.gui-health-detail-lore")));

        inv.setItem(SLOT_GIVE, item(Material.EMERALD,
            lang.getRaw("admin.gui-give-name"),
            lang.getList("admin.gui-give-lore")));
        inv.setItem(SLOT_TAKE, item(Material.REDSTONE,
            lang.getRaw("admin.gui-take-name"),
            lang.getList("admin.gui-take-lore")));
        inv.setItem(SLOT_SET, item(Material.GOLD_INGOT,
            lang.getRaw("admin.gui-set-name"),
            lang.getList("admin.gui-set-lore")));
        inv.setItem(SLOT_TOP, item(Material.GOLDEN_APPLE,
            lang.getRaw("admin.gui-top-name"),
            lang.getList("admin.gui-top-lore")));

        inv.setItem(SLOT_CURRENCY_NAME, item(Material.NAME_TAG,
            lang.getRaw("admin.gui-currency-name-title"),
            List.of(
                lang.getRaw("admin.gui-currency-name-current", "name", cc.getCurrencyName()),
                "",
                lang.getRaw("admin.gui-currency-name-hint")
            )));
        inv.setItem(SLOT_CURRENCY_SYMBOL, item(Material.GOLD_NUGGET,
            lang.getRaw("admin.gui-currency-symbol-title"),
            List.of(
                lang.getRaw("admin.gui-currency-symbol-current", "symbol", cc.getCurrencySymbol()),
                "",
                lang.getRaw("admin.gui-currency-symbol-hint")
            )));

        String currentLang = plugin.getConfig().getString("language", "en").toUpperCase();
        inv.setItem(SLOT_LANG, item(Material.WRITABLE_BOOK,
            lang.getRaw("admin.gui-lang-name"),
            List.of(
                lang.getRaw("admin.gui-lang-current", "lang", currentLang),
                "",
                lang.getRaw("admin.gui-lang-hint")
            )));

        inv.setItem(SLOT_RELOAD, item(Material.COMMAND_BLOCK,
            lang.getRaw("admin.gui-reload-name"),
            lang.getList("admin.gui-reload-lore")));

        admin.openInventory(inv);
        admin.sendMessage(lang.component("admin.panel-opened"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof AdminHolder)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        switch (slot) {
            case SLOT_MARKET_STATS -> showMarketStats(player);
            case SLOT_SHOP_EDITOR -> plugin.getShopPriceEditorGUI().openCategories(player);
            case SLOT_ACTIVITY -> showActivity(player);
            case SLOT_HEALTH, SLOT_HEALTH_DETAIL -> plugin.getEconomyHealthGUI().open(player);
            case SLOT_GIVE -> prompt(player, "give");
            case SLOT_TAKE -> prompt(player, "take");
            case SLOT_SET -> prompt(player, "set");
            case SLOT_TOP -> showTop(player);
            case SLOT_CURRENCY_NAME -> promptCurrency(player, "currency-name");
            case SLOT_CURRENCY_SYMBOL -> promptCurrency(player, "currency-symbol");
            case SLOT_LANG -> {
                String newLang = event.isLeftClick() ? "en" : "pl";
                plugin.getConfig().set("language", newLang);
                plugin.saveConfig();
                plugin.reload();
                openFor(player);
            }
            case SLOT_RELOAD -> {
                plugin.reload();
                player.sendMessage(plugin.getLangManager().component("admin.reloaded"));
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AdminHolder) {
            event.setCancelled(true);
        }
    }

    private void showMarketStats(Player admin) {
        LangManager lang = plugin.getLangManager();
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();
        admin.sendMessage(lang.component("admin.market-stats-header"));
        admin.sendMessage(lang.component("admin.market-stats-listings", "count", String.valueOf(health.getActiveListings())));
        admin.sendMessage(lang.component("admin.market-stats-tax", "tax", String.format("%.1f", plugin.getListingManager().getTaxPercent())));
    }

    private void showActivity(Player admin) {
        LangManager lang = plugin.getLangManager();
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();
        admin.sendMessage(lang.component("admin.activity-header"));
        admin.sendMessage(lang.component("admin.activity-shop", "count", String.valueOf(health.getShopSellsLast24h())));
        admin.sendMessage(lang.component("admin.activity-velocity", "count", String.valueOf((int) health.getMoneyVelocity())));
    }

    private void prompt(Player admin, String action) {
        admin.closeInventory();
        awaitingInput.put(admin.getUniqueId(), action + ":step1");
        String key = switch (action) {
            case "take" -> "admin.prompt-player-take";
            case "set" -> "admin.prompt-player-set";
            default -> "admin.prompt-player-give";
        };
        admin.sendMessage(plugin.getLangManager().component(key));
    }

    private void promptCurrency(Player admin, String action) {
        admin.closeInventory();
        awaitingInput.put(admin.getUniqueId(), action);
        String key = action.equals("currency-symbol")
            ? "admin.prompt-currency-symbol"
            : "admin.prompt-currency-name";
        admin.sendMessage(plugin.getLangManager().component(key));
    }

    public boolean handleChatInput(Player admin, String message) {
        if (!awaitingInput.containsKey(admin.getUniqueId())) return false;
        LangManager lang = plugin.getLangManager();

        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("anuluj")) {
            awaitingInput.remove(admin.getUniqueId());
            admin.sendMessage(lang.component("admin.prompt-cancelled"));
            return true;
        }

        String state = awaitingInput.get(admin.getUniqueId());

        if (state.equals("currency-name")) {
            plugin.getCurrencyConfig().setCurrencyName(message);
            awaitingInput.remove(admin.getUniqueId());
            admin.sendMessage(lang.component("admin.currency-name-set", "name", message.trim()));
            Bukkit.getScheduler().runTask(plugin, () -> openFor(admin));
            return true;
        }
        if (state.equals("currency-symbol")) {
            plugin.getCurrencyConfig().setCurrencySymbol(message);
            awaitingInput.remove(admin.getUniqueId());
            admin.sendMessage(lang.component("admin.currency-symbol-set", "symbol", message.trim()));
            Bukkit.getScheduler().runTask(plugin, () -> openFor(admin));
            return true;
        }

        String[] parts = state.split(":", 2);
        String action = parts[0];
        String step = parts[1];
        CurrencyManager eco = plugin.getCurrencyManager();

        if (step.equals("step1")) {
            Player online = Bukkit.getPlayer(message);
            OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayerIfCached(message);
            if (offline == null || offline.getName() == null) {
                admin.sendMessage(lang.component("player-not-found", "player", message));
                awaitingInput.remove(admin.getUniqueId());
                return true;
            }
            awaitingInput.put(admin.getUniqueId(), action + ":" + offline.getUniqueId());
            admin.sendMessage(lang.component("admin.prompt-amount", "player", offline.getName()));
        } else {
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(step);
            } catch (Exception e) {
                awaitingInput.remove(admin.getUniqueId());
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(message);
            } catch (NumberFormatException e) {
                admin.sendMessage(lang.component("invalid-amount"));
                awaitingInput.remove(admin.getUniqueId());
                return true;
            }

            String targetName = eco.getPlayerName(targetUuid);
            switch (action) {
                case "give" -> {
                    eco.deposit(targetUuid, amount, "admin-give");
                    admin.sendMessage(lang.component("currency.given",
                        "player", targetName, "amount", eco.format(amount)));
                    Player t = Bukkit.getPlayer(targetUuid);
                    if (t != null) t.sendMessage(lang.component("currency.received", "amount", eco.format(amount)));
                }
                case "take" -> {
                    double current = eco.getBalance(targetUuid);
                    double toTake = Math.min(amount, current);
                    eco.withdraw(targetUuid, toTake, "admin-take");
                    admin.sendMessage(lang.component("currency.taken",
                        "player", targetName, "amount", eco.format(toTake)));
                }
                case "set" -> {
                    eco.setBalance(targetUuid, amount, "admin-set");
                    admin.sendMessage(lang.component("currency.set",
                        "player", targetName, "amount", eco.format(amount)));
                }
            }
            awaitingInput.remove(admin.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> openFor(admin));
        }
        return true;
    }

    public boolean isAwaitingInput(Player player) {
        return awaitingInput.containsKey(player.getUniqueId());
    }

    private void showTop(Player admin) {
        admin.closeInventory();
        LangManager lang = plugin.getLangManager();
        CurrencyManager eco = plugin.getCurrencyManager();
        admin.sendMessage(lang.component("currency.top-header"));
        var top = eco.getTopList(10);
        for (int i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            admin.sendMessage(lang.component("currency.top-entry",
                "rank", String.valueOf(i + 1),
                "player", eco.getPlayerName(entry.uuid()),
                "amount", eco.format(entry.balance())));
        }
    }

    private String formatPercent(double percent) {
        return (percent >= 0 ? "+" : "") + String.format("%.1f", percent) + "%";
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

    private void fill(Inventory inv) {
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
