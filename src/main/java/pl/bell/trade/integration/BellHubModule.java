package pl.bell.trade.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import pl.bell.hub.api.ActionDef;
import pl.bell.hub.api.ActionField;
import pl.bell.hub.api.ActionResult;
import pl.bell.hub.api.Actor;
import pl.bell.hub.api.BellModule;
import pl.bell.hub.api.HubAction;
import pl.bell.hub.api.MapFilter;
import pl.bell.hub.api.MapMarker;
import pl.bell.hub.api.Stat;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.CurrencyConfig;
import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.engine.EconomyHealthMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Most BellTrade → panel BellHub. Odczyt i akcje admina przez {@link TradeAdmin} —
 * te same operacje co {@link pl.bell.trade.gui.AdminGUI} i edytor cen skupu.
 */
public final class BellHubModule implements BellModule {

    private final BellTrade plugin;
    private final TradeAdmin admin;

    private BellHubModule(BellTrade plugin) {
        this.plugin = plugin;
        this.admin = new TradeAdmin(plugin);
    }

    public static void register(BellTrade plugin) {
        Bukkit.getServicesManager().register(
                BellModule.class, new BellHubModule(plugin), plugin, ServicePriority.Normal);
    }

    @Override public String id() { return "belltrade"; }
    @Override public String displayName() { return "BellTrade"; }
    @Override public String icon() { return "coins"; }
    @Override public String permission() { return "bellhub.module.belltrade"; }

    @Override
    public List<Stat> dashboard() {
        CurrencyManager eco = plugin.getCurrencyManager();
        CurrencyConfig cc = plugin.getCurrencyConfig();
        EconomyHealthMonitor health = plugin.getEconomyHealthMonitor();
        List<Stat> stats = new ArrayList<>();
        stats.add(new Stat("W obiegu", eco.format(eco.getTotalMoneyInCirculation()), "gold"));
        stats.add(new Stat("Oferty", Integer.toString(health.getActiveListings()), "cyan"));
        stats.add(new Stat("Skup 24h", Integer.toString(health.getShopSellsLast24h()), "violet"));
        String statusColor = switch (health.getStatus()) {
            case INFLATION -> "red";
            case DEFLATION -> "blue";
            case STABLE -> "green";
        };
        stats.add(new Stat("Ekonomia", health.getStatus().name(), statusColor));
        stats.add(new Stat("Waluta", cc.getCurrencyName(), "silver"));
        String lang = plugin.getConfig().getString("language", "en");
        stats.add(new Stat("Język", lang.toUpperCase(), "silver"));
        return stats;
    }

    @Override
    public List<MapMarker> markers(MapFilter filter) {
        return List.of();
    }

    @Override
    public String view(String viewId, Map<String, String> params) {
        return switch (viewId) {
            case "player" -> admin.viewPlayer(params.get("player"));
            case "top" -> admin.viewTop();
            case "health" -> admin.viewHealth();
            case "market" -> admin.viewMarket();
            case "activity" -> admin.viewActivity();
            case "catalog" -> admin.viewCatalog();
            case "listings" -> admin.viewListings();
            case "listings-expiring" -> admin.viewListingsExpiring();
            case "import" -> admin.viewImport();
            case "settings" -> admin.viewSettings();
            default -> "{}";
        };
    }

    @Override
    public ActionResult invoke(HubAction action, Actor actor) {
        return admin.invoke(action, actor);
    }

    @Override
    public List<ActionDef> actions() {
        return List.of(
                ActionDef.of("balance.give", "Dodaj saldo graczowi", "Ekonomia",
                        ActionField.player("player", "Gracz"), ActionField.number("value", "Kwota")),
                ActionDef.of("balance.take", "Zabierz saldo graczowi", "Ekonomia",
                        ActionField.player("player", "Gracz"), ActionField.number("value", "Kwota")),
                ActionDef.of("balance.set", "Ustaw saldo gracza", "Ekonomia",
                        ActionField.player("player", "Gracz"), ActionField.number("value", "Kwota")),
                ActionDef.of("currency.setName", "Nazwa waluty", "Waluta",
                        ActionField.text("value", "Nazwa (np. Coins)")),
                ActionDef.of("currency.setSymbol", "Symbol waluty", "Waluta",
                        ActionField.text("value", "Symbol (np. $)")),
                ActionDef.of("settings.language", "Język pluginu", "Ustawienia",
                        ActionField.select("value", "Język", List.of("pl", "en"))),
                ActionDef.of("settings.reload", "Przeładuj BellTrade", "Ustawienia"),
                ActionDef.of("shop.updatePrice", "Edytuj ceny skupu (base/min/max)", "Skup",
                        ActionField.text("category", "ID kategorii"),
                        ActionField.text("material", "Material (np. DIAMOND)"),
                        ActionField.number("base", "Cena bazowa"),
                        ActionField.number("min", "Min"),
                        ActionField.number("max", "Max")),
                ActionDef.destructive("listing.cancel", "Usuń ofertę rynku", "Rynek",
                        ActionField.number("id", "ID oferty")),
                ActionDef.of("import.preview", "Podgląd importu sald (dry-run)", "Migracja",
                        ActionField.select("source", "Źródło", List.of("essentials", "sqlite")),
                        ActionField.select("mode", "Tryb", List.of("replace", "add", "max")),
                        ActionField.text("path", "Ścieżka (opcjonalnie)")),
                ActionDef.of("import.run", "Wykonaj import sald", "Migracja",
                        ActionField.select("source", "Źródło", List.of("essentials", "sqlite")),
                        ActionField.select("mode", "Tryb", List.of("replace", "add", "max")),
                        ActionField.text("path", "Ścieżka (opcjonalnie)")),
                ActionDef.of("settings.setStartingBalance", "Saldo startowe nowych graczy", "Config",
                        ActionField.number("value", "Kwota")),
                ActionDef.of("settings.setMaxPayAmount", "Maks. kwota /pay", "Config",
                        ActionField.number("value", "Kwota")),
                ActionDef.of("settings.setMarketTax", "Prowizja rynku (%)", "Config",
                        ActionField.number("value", "Procent 0–25")),
                ActionDef.of("settings.setMaxListings", "Limit ofert na gracza (Free)", "Config",
                        ActionField.number("value", "Liczba")),
                ActionDef.of("settings.setListingDuration", "Czas oferty (godziny, Free)", "Config",
                        ActionField.number("value", "Godziny")),
                ActionDef.of("settings.setMaxMarketPrice", "Maks. cena oferty", "Config",
                        ActionField.number("value", "Kwota")),
                ActionDef.of("settings.setProListingDuration", "Czas oferty Pro (godziny)", "Config",
                        ActionField.number("value", "Godziny")),
                ActionDef.of("settings.setShopHealthMultiplier", "Mnożnik zdrowia skupu", "Config",
                        ActionField.number("value", "Mnożnik")),
                ActionDef.of("settings.setTradeMaxDistance", "Maks. dystans trade", "Config",
                        ActionField.number("value", "Bloki")),
                ActionDef.of("settings.setTradeMaxMoney", "Maks. kasa w trade", "Config",
                        ActionField.number("value", "Kwota"))
        );
    }
}
