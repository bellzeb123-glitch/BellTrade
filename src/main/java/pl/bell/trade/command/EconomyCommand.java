package pl.bell.trade.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;
import pl.bell.trade.economy.BalanceRepository;
import pl.bell.trade.economy.CurrencyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final BellTrade plugin;

    public EconomyCommand(BellTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getLangManager();
        CurrencyManager eco = plugin.getCurrencyManager();
        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            case "balance", "bal", "money" -> handleBalance(sender, args, lang, eco);
            case "pay" -> handlePay(sender, args, lang, eco);
            case "baltop", "top" -> handleBaltop(sender, args, lang, eco);
            default -> false;
        };
    }

    private boolean handleBalance(CommandSender sender, String[] args, LangManager lang, CurrencyManager eco) {
        if (!sender.hasPermission("belltrade.balance")) {
            sender.sendMessage(lang.component("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.componentRaw("balance-usage"));
                return true;
            }
            sender.sendMessage(lang.component("balance-self",
                "amount", eco.format(eco.getBalance(player))));
            return true;
        }

        if (!sender.hasPermission("belltrade.balance.others")) {
            sender.sendMessage(lang.component("no-permission"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(lang.component("player-not-found", "player", args[0]));
            return true;
        }
        sender.sendMessage(lang.component("balance-other",
            "player", target.getName(),
            "amount", eco.format(eco.getBalance(target.getUniqueId()))));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args, LangManager lang, CurrencyManager eco) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.componentRaw("only-players"));
            return true;
        }
        if (!player.hasPermission("belltrade.pay")) {
            player.sendMessage(lang.component("no-permission"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(lang.component("pay-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(lang.component("player-not-found", "player", args[0]));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(lang.component("pay-self"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.component("invalid-amount"));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(lang.component("invalid-amount"));
            return true;
        }
        if (amount > plugin.getCurrencyConfig().getMaxPayAmount()) {
            player.sendMessage(lang.component("pay-too-much",
                "max", eco.format(plugin.getCurrencyConfig().getMaxPayAmount())));
            return true;
        }

        if (!eco.withdraw(player.getUniqueId(), amount, "pay")) {
            player.sendMessage(lang.component("not-enough-money"));
            return true;
        }
        eco.deposit(target.getUniqueId(), amount, "pay-received");

        player.sendMessage(lang.component("pay-sent",
            "amount", eco.format(amount), "player", target.getName()));
        target.sendMessage(lang.component("pay-received",
            "amount", eco.format(amount), "player", player.getName()));
        return true;
    }

    private boolean handleBaltop(CommandSender sender, String[] args, LangManager lang, CurrencyManager eco) {
        if (!sender.hasPermission("belltrade.baltop")) {
            sender.sendMessage(lang.component("no-permission"));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }

        int perPage = 10;
        List<BalanceRepository.BalanceEntry> top = eco.getTopList(page * perPage);
        int start = (page - 1) * perPage;
        if (start >= top.size()) {
            sender.sendMessage(lang.component("baltop-empty"));
            return true;
        }

        sender.sendMessage(lang.componentRaw("baltop-header", "page", String.valueOf(page)));
        for (int i = start; i < Math.min(start + perPage, top.size()); i++) {
            BalanceRepository.BalanceEntry entry = top.get(i);
            sender.sendMessage(lang.componentRaw("baltop-entry",
                "rank", String.valueOf(i + 1),
                "player", eco.getPlayerName(entry.uuid()),
                "amount", eco.format(entry.balance())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("pay") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if ((cmd.equals("balance") || cmd.equals("bal") || cmd.equals("money")) && args.length == 1
            && sender.hasPermission("belltrade.balance.others")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if ((cmd.equals("baltop") || cmd.equals("top")) && args.length == 1) {
            return filter(List.of("1", "2", "3"), args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(o -> o.startsWith(lower)).collect(Collectors.toList());
    }
}
