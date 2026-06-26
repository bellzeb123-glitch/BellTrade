package pl.bell.trade.integration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.bell.hub.api.ActionResult;
import pl.bell.hub.api.Actor;
import pl.bell.hub.api.HubAction;
import pl.bell.trade.BellTrade;
import pl.bell.trade.api.BellTradeAPI;
import pl.bell.trade.config.CurrencyConfig;
import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.engine.EconomyHealthMonitor;
import pl.bell.trade.market.ListingManager;
import pl.bell.trade.migration.ImportMode;
import pl.bell.trade.model.Listing;
import pl.bell.trade.model.ShopCategory;
import pl.bell.trade.model.ShopItemEntry;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Logika admina BellTrade dla panelu BellHub. Woła istniejące managery — zero duplikacji
 * z {@link pl.bell.trade.gui.AdminGUI} i edytorem cen skupu.
 */
public final class TradeAdmin {

    private final BellTrade plugin;

    public TradeAdmin(BellTrade plugin) {
        this.plugin = plugin;
    }

    private CurrencyManager eco() { return plugin.getCurrencyManager(); }
    private CurrencyConfig currency() { return plugin.getCurrencyConfig(); }
    private ListingManager market() { return plugin.getListingManager(); }
    private EconomyHealthMonitor health() { return plugin.getEconomyHealthMonitor(); }

    // ── Widoki ──────────────────────────────────────────────

    public String viewPlayer(String name) {
        if (name == null || name.isBlank()) return "{\"found\":false}";
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        UUID uuid = op.getUniqueId();
        String display = op.getName() != null ? op.getName() : name;
        int listings = market().getRepository().countBySeller(uuid);
        return "{\"found\":true,\"player\":\"" + esc(display) + "\",\"uuid\":\"" + uuid + "\""
                + ",\"balance\":" + eco().getBalance(uuid)
                + ",\"balanceFormatted\":\"" + esc(eco().format(eco().getBalance(uuid))) + "\""
                + ",\"listings\":" + listings + "}";
    }

    public String viewTop() {
        List<String> rows = new ArrayList<>();
        var top = eco().getTopList(10);
        for (int i = 0; i < top.size(); i++) {
            var e = top.get(i);
            rows.add("{\"rank\":" + (i + 1)
                    + ",\"player\":\"" + esc(eco().getPlayerName(e.uuid())) + "\""
                    + ",\"balance\":" + e.balance()
                    + ",\"formatted\":\"" + esc(eco().format(e.balance())) + "\"}");
        }
        return "{\"entries\":" + arr(rows) + "}";
    }

    public String viewHealth() {
        EconomyHealthMonitor h = health();
        EconomyHealthMonitor.Status st = h.getStatus();
        var cfg = plugin.getConfig();
        return "{\"status\":\"" + st.name().toLowerCase() + "\""
                + ",\"inflationPercent\":" + fmt(h.getInflationPercent())
                + ",\"moneyVelocity\":" + (int) h.getMoneyVelocity()
                + ",\"shopMultiplier\":" + fmt(h.getShopHealthMultiplier())
                + ",\"marketTaxBonus\":" + fmt(h.getMarketTaxAdjustment())
                + ",\"totalInCirculation\":" + eco().getTotalMoneyInCirculation()
                + ",\"baseline\":" + h.getBaselineTotal()
                + ",\"activeListings\":" + h.getActiveListings()
                + ",\"shopSells24h\":" + h.getShopSellsLast24h()
                + ",\"trackedPlayers\":" + eco().getTrackedPlayerCount()
                + ",\"healthInflationHigh\":" + cfg.getDouble("economy.health.inflation-high", 1.5)
                + ",\"healthInflationLow\":" + cfg.getDouble("economy.health.inflation-low", 0.7)
                + ",\"healthCheckIntervalMinutes\":" + cfg.getLong("economy.health.check-interval-minutes", 60)
                + "}";
    }

    public String viewMarket() {
        return "{\"activeListings\":" + health().getActiveListings()
                + ",\"taxPercent\":" + fmt(market().getTaxPercent())
                + ",\"baseTaxPercent\":" + plugin.getConfig().getDouble("market.tax-percent", 5.0) + "}";
    }

    public String viewActivity() {
        return "{\"shopSells24h\":" + health().getShopSellsLast24h()
                + ",\"moneyVelocity\":" + (int) health().getMoneyVelocity() + "}";
    }

    public String viewCatalog() {
        List<String> cats = new ArrayList<>();
        var engine = BellTradeAPI.get().getShopPriceEngine();
        for (ShopCategory cat : plugin.getShopConfigManager().getCategories()) {
            List<String> items = new ArrayList<>();
            for (ShopItemEntry entry : cat.getItems()) {
                double live = engine.getCurrentPrice(entry.getItemKey());
                items.add("{\"material\":\"" + entry.getMaterial().name() + "\""
                        + ",\"name\":\"" + esc(plugin.getLangManager().materialName(entry.getMaterial())) + "\""
                        + ",\"base\":" + entry.getBasePrice()
                        + ",\"min\":" + entry.getMinPrice()
                        + ",\"max\":" + entry.getMaxPrice()
                        + ",\"live\":" + live
                        + ",\"unit\":" + entry.getUnitSize() + "}");
            }
            cats.add("{\"id\":\"" + esc(cat.getId()) + "\""
                    + ",\"name\":\"" + esc(stripColor(plugin.getLangManager().categoryDisplayName(cat.getId(), cat.getDisplayName()))) + "\""
                    + ",\"icon\":\"" + cat.getIcon().name() + "\""
                    + ",\"items\":" + arr(items) + "}");
        }
        return "{\"categories\":" + arr(cats) + "}";
    }

    public String viewListings() {
        return listingsJson(market().getRepository().findActive(0, 50, null));
    }

    public String viewListingsExpiring() {
        int hours = 24;
        long deadline = System.currentTimeMillis() + hours * 3_600_000L;
        return listingsJson(market().getRepository().findExpiringBefore(deadline));
    }

    private String listingsJson(List<Listing> listings) {
        List<String> rows = new ArrayList<>();
        for (Listing l : listings) {
            rows.add("{\"id\":" + l.getId()
                    + ",\"seller\":\"" + esc(eco().getPlayerName(l.getSellerUuid())) + "\""
                    + ",\"sellerUuid\":\"" + l.getSellerUuid() + "\""
                    + ",\"material\":\"" + l.getItem().getType().name() + "\""
                    + ",\"amount\":" + l.getItem().getAmount()
                    + ",\"price\":" + l.getPrice()
                    + ",\"expiresAt\":" + l.getExpiresAt() + "}");
        }
        return "{\"listings\":" + arr(rows) + "}";
    }

    public String viewImport() {
        var cfg = plugin.getConfig();
        return "{\"essentialsPath\":\"" + esc(cfg.getString("migration.essentials.userdata-folder", "")) + "\""
                + ",\"essentialsMode\":\"" + esc(cfg.getString("migration.essentials.mode", "replace")) + "\""
                + ",\"essentialsSkipZero\":" + cfg.getBoolean("migration.essentials.skip-zero", true)
                + ",\"sqlitePath\":\"" + esc(cfg.getString("migration.sqlite-economy.file", "")) + "\""
                + ",\"sqliteMode\":\"" + esc(cfg.getString("migration.sqlite-economy.mode", "replace")) + "\""
                + ",\"sqliteSkipZero\":" + cfg.getBoolean("migration.sqlite-economy.skip-zero", true) + "}";
    }

    public String viewSettings() {
        var cfg = plugin.getConfig();
        return "{\"language\":\"" + esc(cfg.getString("language", "en")) + "\""
                + ",\"currencyName\":\"" + esc(currency().getCurrencyName()) + "\""
                + ",\"currencySymbol\":\"" + esc(currency().getCurrencySymbol()) + "\""
                + ",\"startingBalance\":" + currency().getStartingBalance()
                + ",\"maxPayAmount\":" + currency().getMaxPayAmount()
                + ",\"marketTaxPercent\":" + cfg.getDouble("market.tax-percent", 5.0)
                + ",\"maxListingsPerPlayer\":" + cfg.getInt("market.max-listings-per-player", 5)
                + ",\"listingDurationHours\":" + cfg.getInt("market.listing-duration-hours", 48)
                + ",\"maxMarketPrice\":" + cfg.getDouble("market.max-price", 1_000_000)
                + ",\"expireCheckMinutes\":" + cfg.getInt("market.expire-check-minutes", 15)
                + ",\"shopHealthMultiplier\":" + cfg.getDouble("shop.health-multiplier", 1.0)
                + ",\"tradeMaxDistance\":" + cfg.getDouble("trade.max-distance", 10.0)
                + ",\"tradeMaxMoneyOffer\":" + cfg.getDouble("trade.max-money-offer", 1_000_000)
                + ",\"proListingDurationHours\":" + cfg.getInt("pro.market.listing-duration-hours", 168)
                + ",\"registerEconomyCommands\":" + cfg.getBoolean("commands.register-economy", true) + "}";
    }

    // ── Akcje ───────────────────────────────────────────────

    public ActionResult invoke(HubAction action, Actor actor) {
        if (actor == null || (!actor.admin() && !actor.has("bellhub.module.belltrade"))) {
            return ActionResult.error("Brak uprawnień.");
        }
        try {
            return switch (action.name()) {
                case "balance.give" -> balanceGive(action.param("player"), action.param("value"));
                case "balance.take" -> balanceTake(action.param("player"), action.param("value"));
                case "balance.set" -> balanceSet(action.param("player"), action.param("value"));
                case "currency.setName" -> currencySetName(action.param("value"));
                case "currency.setSymbol" -> currencySetSymbol(action.param("value"));
                case "settings.language" -> setLanguage(action.param("value"));
                case "settings.reload" -> {
                    plugin.reload();
                    yield ActionResult.ok("BellTrade przeładowany.");
                }
                case "shop.updatePrice" -> shopUpdatePrice(action);
                case "listing.cancel" -> listingCancel(action.param("id"));
                case "import.preview" -> importPreview(action);
                case "import.run" -> importRun(action);
                case "settings.setStartingBalance" -> setDoubleConfig("economy.starting-balance", action.param("value"), 0, -1);
                case "settings.setMaxPayAmount" -> setDoubleConfig("economy.max-pay-amount", action.param("value"), 0, -1);
                case "settings.setMarketTax" -> setDoubleConfig("market.tax-percent", action.param("value"), 0, 25);
                case "settings.setMaxListings" -> setIntConfig("market.max-listings-per-player", action.param("value"), 1, 10_000);
                case "settings.setListingDuration" -> setIntConfig("market.listing-duration-hours", action.param("value"), 1, 8760);
                case "settings.setMaxMarketPrice" -> setDoubleConfig("market.max-price", action.param("value"), 1, -1);
                case "settings.setProListingDuration" -> setIntConfig("pro.market.listing-duration-hours", action.param("value"), 1, 8760);
                case "settings.setShopHealthMultiplier" -> setDoubleConfig("shop.health-multiplier", action.param("value"), 0.01, -1);
                case "settings.setTradeMaxDistance" -> setDoubleConfig("trade.max-distance", action.param("value"), 1, -1);
                case "settings.setTradeMaxMoney" -> setDoubleConfig("trade.max-money-offer", action.param("value"), 1, -1);
                default -> ActionResult.error("Nieznana akcja: " + action.name());
            };
        } catch (NumberFormatException e) {
            return ActionResult.error("Niepoprawna liczba.");
        } catch (Exception e) {
            return ActionResult.error("Błąd: " + e.getMessage());
        }
    }

    private ActionResult importPreview(HubAction action) {
        String source = normalizeSource(action.param("source"));
        if (source == null) return ActionResult.error("Źródło: essentials lub sqlite.");
        ImportMode mode = BalanceImportHelper.resolveMode(plugin, source, action.param("mode"));
        String path = blankToNull(action.param("path"));
        try {
            BalanceImportHelper.ImportScanResult scan = scanAsync(source, mode, path);
            if (!scan.success()) return ActionResult.error(scan.error() != null ? scan.error() : "Błąd skanowania.");
            return ActionResult.ok(BalanceImportHelper.formatPreview(plugin, scan, mode));
        } catch (Exception e) {
            return ActionResult.error("Import: " + e.getMessage());
        }
    }

    private ActionResult importRun(HubAction action) {
        String source = normalizeSource(action.param("source"));
        if (source == null) return ActionResult.error("Źródło: essentials lub sqlite.");
        ImportMode mode = BalanceImportHelper.resolveMode(plugin, source, action.param("mode"));
        String path = blankToNull(action.param("path"));
        try {
            BalanceImportHelper.ImportScanResult scan = scanAsync(source, mode, path);
            if (!scan.success()) return ActionResult.error(scan.error() != null ? scan.error() : "Błąd skanowania.");
            if (scan.balances().isEmpty()) return ActionResult.error("Brak kont do importu.");
            CompletableFuture<BalanceImportHelper.ImportApplyResult> applyFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () ->
                    applyFuture.complete(BalanceImportHelper.apply(plugin, scan, mode)));
            BalanceImportHelper.ImportApplyResult applied = applyFuture.get(15, TimeUnit.SECONDS);
            return ActionResult.ok(BalanceImportHelper.formatDone(plugin, applied, mode));
        } catch (Exception e) {
            return ActionResult.error("Import: " + e.getMessage());
        }
    }

    private BalanceImportHelper.ImportScanResult scanAsync(String source, ImportMode mode, String path)
            throws Exception {
        CompletableFuture<BalanceImportHelper.ImportScanResult> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                future.complete(BalanceImportHelper.scan(plugin, source, mode, path)));
        return future.get(90, TimeUnit.SECONDS);
    }

    private ActionResult setDoubleConfig(String key, String value, double min, double max) {
        double v = parseDouble(value);
        if (v < min) return ActionResult.error("Wartość >= " + min);
        if (max > 0 && v > max) return ActionResult.error("Wartość <= " + max);
        plugin.getConfig().set(key, v);
        plugin.saveConfig();
        plugin.reload();
        return ActionResult.ok(key + " = " + v);
    }

    private ActionResult setIntConfig(String key, String value, int min, int max) {
        int v = Integer.parseInt(value.trim());
        if (v < min || v > max) return ActionResult.error("Wartość " + min + "–" + max);
        plugin.getConfig().set(key, v);
        plugin.saveConfig();
        plugin.reload();
        return ActionResult.ok(key + " = " + v);
    }

    private ActionResult balanceGive(String name, String value) {
        UUID uuid = resolve(name);
        if (uuid == null) return ActionResult.error("Gracz nie znaleziony.");
        double amount = parseDouble(value);
        if (amount <= 0) return ActionResult.error("Kwota musi być dodatnia.");
        eco().deposit(uuid, amount, "bellhub-give");
        notifyPlayer(uuid, amount, true);
        return ActionResult.ok("Dodano " + eco().format(amount) + " graczowi " + eco().getPlayerName(uuid) + ".");
    }

    private ActionResult balanceTake(String name, String value) {
        UUID uuid = resolve(name);
        if (uuid == null) return ActionResult.error("Gracz nie znaleziony.");
        double amount = parseDouble(value);
        if (amount <= 0) return ActionResult.error("Kwota musi być dodatnia.");
        double taken = Math.min(amount, eco().getBalance(uuid));
        eco().withdraw(uuid, taken, "bellhub-take");
        return ActionResult.ok("Zabrano " + eco().format(taken) + " od " + eco().getPlayerName(uuid) + ".");
    }

    private ActionResult balanceSet(String name, String value) {
        UUID uuid = resolve(name);
        if (uuid == null) return ActionResult.error("Gracz nie znaleziony.");
        double amount = parseDouble(value);
        if (amount < 0) return ActionResult.error("Saldo nie może być ujemne.");
        eco().setBalance(uuid, amount, "bellhub-set");
        return ActionResult.ok("Ustawiono saldo " + eco().getPlayerName(uuid) + " na " + eco().format(amount) + ".");
    }

    private ActionResult currencySetName(String name) {
        if (name == null || name.isBlank()) return ActionResult.error("Podaj nazwę waluty.");
        currency().setCurrencyName(name.trim());
        return ActionResult.ok("Nazwa waluty: " + name.trim());
    }

    private ActionResult currencySetSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return ActionResult.error("Podaj symbol waluty.");
        currency().setCurrencySymbol(symbol.trim());
        return ActionResult.ok("Symbol waluty: " + symbol.trim());
    }

    private ActionResult setLanguage(String code) {
        if (!"pl".equals(code) && !"en".equals(code)) return ActionResult.error("Język: pl lub en.");
        plugin.getConfig().set("language", code);
        plugin.saveConfig();
        plugin.reload();
        return ActionResult.ok("Język = " + code + ".");
    }

    private ActionResult shopUpdatePrice(HubAction action) {
        String categoryId = action.param("category");
        String matName = action.param("material");
        if (categoryId == null || matName == null) return ActionResult.error("Podaj kategorię i materiał.");
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActionResult.error("Nieznany materiał: " + matName);
        }
        double base = parseDouble(action.param("base"));
        double min = parseDouble(action.param("min"));
        double max = parseDouble(action.param("max"));
        if (base <= 0 || min < 0 || max < min) return ActionResult.error("base > 0, min >= 0, max >= min.");
        if (!plugin.getShopConfigManager().updateItemPrice(categoryId, material, base, min, max)) {
            return ActionResult.error("Nie udało się zapisać ceny.");
        }
        return ActionResult.ok("Zapisano ceny " + material.name() + " w " + categoryId + ".");
    }

    private ActionResult listingCancel(String idStr) {
        if (idStr == null || idStr.isBlank()) return ActionResult.error("Podaj ID oferty.");
        long id = Long.parseLong(idStr.trim());
        var opt = market().getRepository().findById(id);
        if (opt.isEmpty()) return ActionResult.error("Oferta nie istnieje.");
        Listing listing = opt.get();
        try {
            market().getRepository().deleteSync(id);
            plugin.getExpiredMailboxManager().depositSync(listing.getSellerUuid(), listing.getItem(), listing.getId());
        } catch (SQLException e) {
            return ActionResult.error("Błąd bazy: " + e.getMessage());
        }
        return ActionResult.ok("Oferta #" + id + " usunięta; przedmiot w skrzynce wygasłych sprzedawcy.");
    }

    private static String normalizeSource(String source) {
        if (source == null) return null;
        String s = source.toLowerCase().trim();
        if (s.equals("essentials")) return "essentials";
        if (s.equals("sqlite") || s.equals("economy")) return "sqlite";
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void notifyPlayer(UUID uuid, double amount, boolean received) {
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) return;
        String key = received ? "currency.received" : "currency.taken";
        online.sendMessage(plugin.getLangManager().component(key, "amount", eco().format(amount)));
    }

    private UUID resolve(String name) {
        if (name == null || name.isBlank()) return null;
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && cached.getName() != null) return cached.getUniqueId();
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        return op.getName() != null ? op.getUniqueId() : null;
    }

    private static double parseDouble(String s) {
        return Double.parseDouble(s.replace(",", ".").trim());
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.4f", v);
    }

    private static String arr(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stripColor(String text) {
        return text.replaceAll("(?i)&[0-9a-fk-or]", "");
    }
}
