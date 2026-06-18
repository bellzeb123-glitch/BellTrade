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

public class SellShopCommand implements CommandExecutor, TabCompleter {

    private final BellTrade plugin;

    public SellShopCommand(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getLangManager();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.componentRaw("only-players"));
            return true;
        }
        if (!player.hasPermission("belltrade.shop")) {
            player.sendMessage(lang.component("no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
            plugin.getShopManager().sellFromHand(player);
            return true;
        }

        plugin.getSellShopGUI().openCategories(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("hand"), args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
