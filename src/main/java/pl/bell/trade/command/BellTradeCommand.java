package pl.bell.trade.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

import java.util.ArrayList;
import java.util.List;
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
            plugin.reload();
            sender.sendMessage(lang.component("reload-success"));
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
            plugin.reload();
            sender.sendMessage(lang.component("language-changed", "lang", langCode.toUpperCase()));
            return true;
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
        sender.sendMessage(lang.componentRaw("help-language"));
        sender.sendMessage(lang.componentRaw("help-reload"));
        return true;
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
                options.add("language");
                options.add("reload");
            }
            return filter(options, args[0]);
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
