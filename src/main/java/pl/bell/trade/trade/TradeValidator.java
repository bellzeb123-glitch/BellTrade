package pl.bell.trade.trade;

import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.config.LangManager;

public class TradeValidator {

    private final BellTrade plugin;

    public TradeValidator(BellTrade plugin) {
        this.plugin = plugin;
    }

    /** @return lang key or null if valid */
    public String validateInvite(Player sender, Player target) {
        if (sender.equals(target)) return "trade.self";
        if (!target.isOnline()) return "player-not-found";

        double maxDist = plugin.getConfig().getDouble("trade.max-distance", 10.0);
        if (!sender.getWorld().equals(target.getWorld())) return "trade.different-world";
        if (sender.getLocation().distance(target.getLocation()) > maxDist) return "trade.too-far";

        if (!plugin.getBellLandsHook().canTradeOnClaim(sender)) return "trade.not-trusted-claim";
        if (!plugin.getBellLandsHook().canTradeOnClaim(target)) return "trade.not-trusted-claim";

        return null;
    }

    public String validateSessionActive(Player a, Player b) {
        double maxDist = plugin.getConfig().getDouble("trade.max-distance", 10.0);
        if (!a.getWorld().equals(b.getWorld())) return "trade.different-world";
        if (a.getLocation().distance(b.getLocation()) > maxDist) return "trade.too-far";
        if (!plugin.getBellLandsHook().canTradeOnClaim(a)) return "trade.not-trusted-claim";
        if (!plugin.getBellLandsHook().canTradeOnClaim(b)) return "trade.not-trusted-claim";
        return null;
    }

    public void sendError(Player player, String langKey) {
        if (langKey == null) return;
        LangManager lang = plugin.getLangManager();
        if (langKey.equals("player-not-found")) {
            player.sendMessage(lang.component("player-not-found", "player", "?"));
        } else {
            player.sendMessage(lang.component(langKey));
        }
    }
}
