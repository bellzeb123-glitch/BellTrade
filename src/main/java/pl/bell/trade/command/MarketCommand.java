package pl.bell.trade.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final BellTrade plugin;

    public MarketCommand(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getLangManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.componentRaw("only-players"));
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission("belltrade.market")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            plugin.getMarketGUI().openBrowse(player, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("my")) {
            if (!player.hasPermission("belltrade.market")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            plugin.getMarketGUI().openMy(player);
            return true;
        }

        if (sub.equals("sell")) {
            if (!player.hasPermission("belltrade.market.sell")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(lang.component("market.sell-usage"));
                return true;
            }
            try {
                double price = Double.parseDouble(args[1]);
                plugin.getListingManager().createListing(player, price);
            } catch (NumberFormatException e) {
                player.sendMessage(lang.component("invalid-amount"));
            }
            return true;
        }

        if (sub.equals("cancel")) {
            if (!player.hasPermission("belltrade.market.sell")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(lang.component("market.cancel-usage"));
                return true;
            }
            try {
                long id = Long.parseLong(args[1]);
                plugin.getListingManager().cancelListing(player, id);
            } catch (NumberFormatException e) {
                player.sendMessage(lang.component("market.invalid-id"));
            }
            return true;
        }

        if (sub.equals("search")) {
            if (!player.hasPermission("belltrade.market")) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(lang.component("market.search-usage"));
                return true;
            }
            String material = args[1].toUpperCase();
            try {
                Material.valueOf(material);
            } catch (IllegalArgumentException e) {
                player.sendMessage(lang.component("market.invalid-material", "material", args[1]));
                return true;
            }
            plugin.getMarketGUI().openBrowse(player, 1, material);
            return true;
        }

        player.sendMessage(lang.component("market.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("sell", "my", "cancel", "search"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
            return Arrays.stream(Material.values())
                .map(Enum::name)
                .filter(n -> n.startsWith(args[1].toUpperCase()))
                .limit(20)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
