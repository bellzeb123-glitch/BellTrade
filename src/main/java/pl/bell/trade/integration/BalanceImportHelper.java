package pl.bell.trade.integration;

import pl.bell.trade.BellTrade;
import pl.bell.trade.migration.EssentialsBalanceImporter;
import pl.bell.trade.migration.EssentialsUserdataLocator;
import pl.bell.trade.migration.ImportMode;
import pl.bell.trade.migration.SqliteEconomyImporter;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/** Skan i import sald — współdzielone przez komendę /btrade import i panel BellHub. */
public final class BalanceImportHelper {

    public record ImportScanResult(
            boolean success,
            String error,
            Map<UUID, Double> balances,
            int scanned,
            int accounts,
            int skipped,
            double totalAmount,
            String sourceLabel
    ) {}

    public record ImportApplyResult(int updated, ImportScanResult scan) {}

    private BalanceImportHelper() {}

    public static ImportScanResult scan(BellTrade plugin, String source, ImportMode mode, String customPath) {
        if ("essentials".equalsIgnoreCase(source)) {
            return scanEssentials(plugin, customPath);
        }
        if ("sqlite".equalsIgnoreCase(source) || "economy".equalsIgnoreCase(source)) {
            return scanSqlite(plugin, customPath);
        }
        return new ImportScanResult(false, "Źródło: essentials lub sqlite", Map.of(), 0, 0, 0, 0, "");
    }

    public static ImportApplyResult apply(BellTrade plugin, ImportScanResult scan, ImportMode mode) {
        if (!scan.success() || scan.balances().isEmpty()) {
            return new ImportApplyResult(0, scan);
        }
        int updated = plugin.getCurrencyManager().importBalances(scan.balances(), mode, "bellhub-import");
        return new ImportApplyResult(updated, scan);
    }

    private static ImportScanResult scanEssentials(BellTrade plugin, String customPath) {
        String configPath = plugin.getConfig().getString(
                "migration.essentials.userdata-folder", "plugins/Essentials/userdata");
        EssentialsUserdataLocator.LocateResult located = EssentialsUserdataLocator.locate(
                plugin, configPath, customPath);
        File userdataDir = located.folder();
        if (userdataDir == null) {
            return new ImportScanResult(false,
                    EssentialsUserdataLocator.formatNotFound(located), Map.of(), 0, 0, 0, 0, "EssentialsX");
        }
        boolean skipZero = plugin.getConfig().getBoolean("migration.essentials.skip-zero", true);
        EssentialsBalanceImporter importer = new EssentialsBalanceImporter(plugin.getLogger());
        EssentialsBalanceImporter.Result scan = importer.scan(userdataDir, skipZero);
        if (!scan.success()) {
            return new ImportScanResult(false, scan.error(), Map.of(), 0, 0, 0, 0, "EssentialsX");
        }
        return new ImportScanResult(true, null, scan.balances(),
                scan.filesScanned(), scan.withBalance(), scan.skipped(), scan.totalAmount(), "EssentialsX");
    }

    private static ImportScanResult scanSqlite(BellTrade plugin, String customPath) {
        String configPath = plugin.getConfig().getString(
                "migration.sqlite-economy.file", "plugins/databases/economy.db");
        File dbFile = EssentialsUserdataLocator.resolve(plugin,
                customPath != null && !customPath.isBlank() ? customPath : configPath);
        boolean skipZero = plugin.getConfig().getBoolean("migration.sqlite-economy.skip-zero", true);
        SqliteEconomyImporter importer = new SqliteEconomyImporter(plugin.getLogger());
        SqliteEconomyImporter.Result scan = importer.scan(dbFile, skipZero);
        if (!scan.success()) {
            return new ImportScanResult(false, scan.error(), Map.of(), 0, 0, 0, 0, "economy.db");
        }
        return new ImportScanResult(true, null, scan.balances(),
                scan.rowsRead(), scan.imported(), scan.skipped(), scan.totalAmount(), "economy.db");
    }

    public static ImportMode resolveMode(BellTrade plugin, String source, String modeParam) {
        String modeKey = "essentials".equalsIgnoreCase(source)
                ? "migration.essentials.mode" : "migration.sqlite-economy.mode";
        ImportMode def = ImportMode.parse(plugin.getConfig().getString(modeKey, "replace"), ImportMode.REPLACE);
        if (modeParam == null || modeParam.isBlank()) return def;
        return ImportMode.parse(modeParam, def);
    }

    public static String formatPreview(BellTrade plugin, ImportScanResult scan, ImportMode mode) {
        if (!scan.success()) return scan.error() != null ? scan.error() : "Błąd skanowania.";
        return "Podgląd " + scan.sourceLabel() + ": przeskanowano " + scan.scanned()
                + ", kont " + scan.accounts() + ", pominięto " + scan.skipped()
                + ", suma " + plugin.getCurrencyManager().format(scan.totalAmount())
                + ", tryb " + mode.name().toLowerCase() + ".";
    }

    public static String formatDone(BellTrade plugin, ImportApplyResult result, ImportMode mode) {
        ImportScanResult scan = result.scan();
        return "Import " + scan.sourceLabel() + ": zaktualizowano " + result.updated()
                + " kont (przeskanowano " + scan.scanned() + ", pominięto " + scan.skipped()
                + ", suma " + plugin.getCurrencyManager().format(scan.totalAmount())
                + ", tryb " + mode.name().toLowerCase() + ").";
    }
}
