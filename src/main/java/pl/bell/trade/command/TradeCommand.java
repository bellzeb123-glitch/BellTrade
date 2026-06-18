package pl.bell.trade.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

import java.util.List;
import java.util.stream.Collectors;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final BellTrade plugin;

    public TradeCommand(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getLangManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.componentRaw("only-players"));
            return true;
        }
        if (!player.hasPermission("belltrade.trade")) {
            player.sendMessage(lang.component("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(lang.component("trade.usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("accept")) {
            plugin.getTradeManager().acceptInvite(player);
            return true;
        }
        if (sub.equals("deny") || sub.equals("decline")) {
            plugin.getTradeManager().denyInvite(player);
            return true;
        }
        if (sub.equals("cancel")) {
            plugin.getTradeManager().cancelTrade(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(lang.component("player-not-found", "player", args[0]));
            return true;
        }

        plugin.getTradeManager().sendInvite(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> opts = new java.util.ArrayList<>(List.of("accept", "deny", "cancel"));
            if (sender instanceof Player player) {
                Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .map(Player::getName)
                    .forEach(opts::add);
            }
            return opts.stream().filter(o -> o.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }
        return List.of();
    }
}
