package pl.bell.trade.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.migration.EssentialsBalanceImporter;
import pl.bell.trade.migration.EssentialsUserdataLocator;
import pl.bell.trade.migration.ImportMode;
import pl.bell.trade.migration.SqliteEconomyImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BellTradeCommand implements CommandExecutor, TabCompleter {

    private final BellTrade plugin;

    public BellTradeCommand(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getLangManager();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.componentRaw("only-players"));
                return true;
            }
            if (!player.hasPermission("belltrade.use")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            plugin.getMainMenuGUI().open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("belltrade.admin")) {
                sender.sendMessage(lang.component("no-permission"));
                return true;
            }
            try {
                plugin.reload();
                sender.sendMessage(lang.component("reload-success"));
            } catch (Exception e) {
                plugin.getLogger().severe("Reload failed: " + e.getMessage());
                e.printStackTrace();
                sender.sendMessage("§cBellTrade reload failed — see console.");
            }
            return true;
        }

        if (sub.equals("language") || sub.equals("lang")) {
            if (!sender.hasPermission("belltrade.admin")) {
                sender.sendMessage(lang.component("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(lang.component("language-usage"));
                return true;
            }
            String langCode = args[1].toLowerCase();
            if (!langCode.equals("en") && !langCode.equals("pl")) {
                sender.sendMessage(lang.component("language-invalid"));
                return true;
            }
            plugin.getConfig().set("language", langCode);
            plugin.saveConfig();
            try {
                plugin.reload();
                sender.sendMessage(lang.component("language-changed", "lang", langCode.toUpperCase()));
            } catch (Exception e) {
                plugin.getLogger().severe("Reload failed: " + e.getMessage());
                e.printStackTrace();
                sender.sendMessage("§cBellTrade reload failed — see console.");
            }
            return true;
        }

        if (sub.equals("import")) {
            if (!sender.hasPermission("belltrade.admin")) {
                sender.sendMessage(lang.component("no-permission"));
                return true;
            }
            return handleImport(sender, args, lang);
        }

        if (sub.equals("admin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.componentRaw("only-players"));
                return true;
            }
            if (!player.hasPermission("belltrade.admin")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            plugin.getAdminGUI().openFor(player);
            return true;
        }

        if (sub.equals("shop") || sub.equals("sellshop") || sub.equals("skup")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.componentRaw("only-players"));
                return true;
            }
            if (!player.hasPermission("belltrade.shop")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            plugin.getSellShopGUI().openCategories(player);
            return true;
        }

        sender.sendMessage(lang.componentRaw("help-header", "version", plugin.getDescription().getVersion()));
        sender.sendMessage(lang.componentRaw("help-admin"));
        sender.sendMessage(lang.componentRaw("help-import"));
        sender.sendMessage(lang.componentRaw("help-language"));
        sender.sendMessage(lang.componentRaw("help-reload"));
        return true;
    }

    private boolean isImportMode(String arg) {
        return arg.equals("replace") || arg.equals("add") || arg.equals("max");
    }

    private boolean handleImport(CommandSender sender, String[] args, LangManager lang) {
        if (args.length < 2) {
            sender.sendMessage(lang.componentRaw("import-usage"));
            return true;
        }

        String source = args[1].toLowerCase();
        if (!source.equals("essentials") && !source.equals("sqlite") && !source.equals("economy")) {
            sender.sendMessage(lang.componentRaw("import-usage"));
            return true;
        }

        boolean dryRun = false;
        String modeKey = source.equals("essentials") ? "migration.essentials.mode" : "migration.sqlite-economy.mode";
        ImportMode mode = ImportMode.parse(plugin.getConfig().getString(modeKey, "replace"), ImportMode.REPLACE);
        String customPath = null;

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            String lower = arg.toLowerCase();
            if (lower.equals("dry-run") || lower.equals("preview")) {
                dryRun = true;
            } else if (isImportMode(lower)) {
                mode = ImportMode.parse(lower, mode);
            } else {
                customPath = arg;
            }
        }

        if (source.equals("essentials")) {
            return runEssentialsImport(sender, lang, mode, dryRun, customPath);
        }
        return runSqliteEconomyImport(sender, lang, mode, dryRun, customPath);
    }

    private boolean runEssentialsImport(CommandSender sender, LangManager lang, ImportMode mode,
                                        boolean dryRun, String customPath) {
        String configPath = plugin.getConfig().getString(
            "migration.essentials.userdata-folder", "plugins/Essentials/userdata");
        EssentialsUserdataLocator.LocateResult located = EssentialsUserdataLocator.locate(
            plugin, configPath, customPath);
        File userdataDir = located.folder();
        boolean skipZero = plugin.getConfig().getBoolean("migration.essentials.skip-zero", true);
        ImportMode finalMode = mode;
        boolean finalDryRun = dryRun;

        sender.sendMessage(lang.component("import-start", "source", "EssentialsX"));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (userdataDir == null) {
                reply(sender, lang.component("import-error",
                    "error", EssentialsUserdataLocator.formatNotFound(located)));
                return;
            }

            EssentialsBalanceImporter importer = new EssentialsBalanceImporter(plugin.getLogger());
            EssentialsBalanceImporter.Result scan = importer.scan(userdataDir, skipZero);
            if (!scan.success()) {
                reply(sender, lang.component("import-error", "error", scan.error()));
                return;
            }
            finishImport(sender, lang, finalMode, finalDryRun, scan.balances(),
                scan.filesScanned(), scan.withBalance(), scan.skipped(), scan.totalAmount(), "essentials-import");
        });
        return true;
    }

    private boolean runSqliteEconomyImport(CommandSender sender, LangManager lang, ImportMode mode,
                                           boolean dryRun, String customPath) {
        String configPath = plugin.getConfig().getString(
            "migration.sqlite-economy.file", "plugins/databases/economy.db");
        File dbFile = resolveImportFile(customPath != null ? customPath : configPath);
        boolean skipZero = plugin.getConfig().getBoolean("migration.sqlite-economy.skip-zero", true);
        ImportMode finalMode = mode;
        boolean finalDryRun = dryRun;

        sender.sendMessage(lang.component("import-start", "source", "economy.db"));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            SqliteEconomyImporter importer = new SqliteEconomyImporter(plugin.getLogger());
            SqliteEconomyImporter.Result scan = importer.scan(dbFile, skipZero);
            if (!scan.success()) {
                reply(sender, lang.component("import-error", "error", scan.error()));
                return;
            }
            finishImport(sender, lang, finalMode, finalDryRun, scan.balances(),
                scan.rowsRead(), scan.imported(), scan.skipped(), scan.totalAmount(), "sqlite-economy-import");
        });
        return true;
    }

    private void finishImport(CommandSender sender, LangManager lang, ImportMode mode, boolean dryRun,
                              Map<UUID, Double> balances, int scanned, int accounts, int skipped,
                              double total, String reason) {
        if (dryRun) {
            reply(sender, lang.component("import-preview",
                "scanned", String.valueOf(scanned),
                "accounts", String.valueOf(accounts),
                "skipped", String.valueOf(skipped),
                "total", plugin.getCurrencyManager().format(total),
                "mode", mode.name().toLowerCase()));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int updated = plugin.getCurrencyManager().importBalances(balances, mode, reason);
            reply(sender, lang.component("import-done",
                "scanned", String.valueOf(scanned),
                "accounts", String.valueOf(accounts),
                "updated", String.valueOf(updated),
                "skipped", String.valueOf(skipped),
                "total", plugin.getCurrencyManager().format(total),
                "mode", mode.name().toLowerCase()));
        });
    }

    private File resolveImportFile(String path) {
        return EssentialsUserdataLocator.resolve(plugin, path);
    }

    private void reply(CommandSender sender, net.kyori.adventure.text.Component message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("belltrade.shop")) {
                options.add("shop");
            }
            if (sender.hasPermission("belltrade.admin")) {
                options.add("admin");
                options.add("import");
                options.add("language");
                options.add("reload");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return filter(List.of("essentials", "sqlite", "economy"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("import")) {
            return filter(List.of("replace", "add", "max", "dry-run"), args[2]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("lang"))) {
            return filter(List.of("en", "pl"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
