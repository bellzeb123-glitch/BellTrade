package pl.bell.trade.trade;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl.bell.trade.BellTrade;
import pl.bell.trade.event.TradeCompleteEvent;
import pl.bell.trade.gui.TradeGUI;
import pl.bell.trade.model.TradeSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private record TradeInvite(UUID inviter, long expiresAt) {}

    private final BellTrade plugin;
    private final TradeValidator validator;
    private final TradeGUI tradeGUI;

    private final Map<UUID, TradeInvite> pendingInvites = new HashMap<>();
    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    private BukkitTask inviteCleanupTask;

    public TradeManager(BellTrade plugin, TradeGUI tradeGUI) {
        this.plugin = plugin;
        this.tradeGUI = tradeGUI;
        this.validator = new TradeValidator(plugin);
        startInviteCleanup();
    }

    public TradeValidator getValidator() {
        return validator;
    }

    public TradeSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    public boolean isTrading(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public void sendInvite(Player inviter, Player target) {
        if (isTrading(inviter.getUniqueId()) || isTrading(target.getUniqueId())) {
            inviter.sendMessage(plugin.getLangManager().component("trade.already-trading"));
            return;
        }

        String error = validator.validateInvite(inviter, target);
        if (error != null) {
            validator.sendError(inviter, error);
            return;
        }

        long timeoutMs = plugin.getConfig().getLong("trade.invite-timeout-seconds", 30) * 1000L;
        pendingInvites.put(target.getUniqueId(), new TradeInvite(inviter.getUniqueId(), System.currentTimeMillis() + timeoutMs));

        inviter.sendMessage(plugin.getLangManager().component("trade.invite-sent", "player", target.getName()));
        target.sendMessage(plugin.getLangManager().component("trade.invite-received",
            "player", inviter.getName(),
            "seconds", String.valueOf(plugin.getConfig().getLong("trade.invite-timeout-seconds", 30))));
    }

    public void acceptInvite(Player target) {
        TradeInvite invite = pendingInvites.remove(target.getUniqueId());
        if (invite == null) {
            target.sendMessage(plugin.getLangManager().component("trade.no-invite"));
            return;
        }
        if (System.currentTimeMillis() > invite.expiresAt()) {
            target.sendMessage(plugin.getLangManager().component("trade.invite-expired"));
            return;
        }

        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviter == null || !inviter.isOnline()) {
            target.sendMessage(plugin.getLangManager().component("trade.partner-offline"));
            return;
        }

        if (isTrading(inviter.getUniqueId()) || isTrading(target.getUniqueId())) {
            target.sendMessage(plugin.getLangManager().component("trade.already-trading"));
            return;
        }

        String error = validator.validateInvite(inviter, target);
        if (error != null) {
            validator.sendError(target, error);
            return;
        }

        startSession(inviter, target);
    }

    public void denyInvite(Player target) {
        TradeInvite invite = pendingInvites.remove(target.getUniqueId());
        if (invite == null) {
            target.sendMessage(plugin.getLangManager().component("trade.no-invite"));
            return;
        }
        target.sendMessage(plugin.getLangManager().component("trade.invite-denied-self"));
        Player inviter = Bukkit.getPlayer(invite.inviter());
        if (inviter != null) {
            inviter.sendMessage(plugin.getLangManager().component("trade.invite-denied",
                "player", target.getName()));
        }
    }

    public void cancelTrade(Player player) {
        TradeSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(plugin.getLangManager().component("trade.not-trading"));
            return;
        }

        UUID partnerId = session.getPartner(player.getUniqueId());
        Player partner = Bukkit.getPlayer(partnerId);

        tradeGUI.returnItems(player, session);
        player.closeInventory();
        player.sendMessage(plugin.getLangManager().component("trade.cancelled-self"));

        if (partner != null && partner.isOnline()) {
            tradeGUI.returnItems(partner, session);
            partner.closeInventory();
            partner.sendMessage(plugin.getLangManager().component("trade.cancelled-partner",
                "player", player.getName()));
        }

        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());
    }

    public void startSession(Player a, Player b) {
        TradeSession session = new TradeSession(a.getUniqueId(), b.getUniqueId());
        activeSessions.put(a.getUniqueId(), session);
        activeSessions.put(b.getUniqueId(), session);

        a.sendMessage(plugin.getLangManager().component("trade.started", "player", b.getName()));
        b.sendMessage(plugin.getLangManager().component("trade.started", "player", a.getName()));

        tradeGUI.open(a, session);
        tradeGUI.open(b, session);
    }

    public void tryComplete(TradeSession session) {
        if (!session.bothReady()) return;

        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a == null || b == null) {
            abortSession(session, "trade.partner-offline");
            return;
        }

        String error = validator.validateSessionActive(a, b);
        if (error != null) {
            validator.sendError(a, error);
            validator.sendError(b, error);
            session.resetReady();
            tradeGUI.refresh(session);
            return;
        }

        double moneyA = session.getMoneyA();
        double moneyB = session.getMoneyB();
        var eco = plugin.getCurrencyManager();

        if (!eco.hasEnough(session.getPlayerA(), moneyA)) {
            a.sendMessage(plugin.getLangManager().component("not-enough-money"));
            session.resetReady();
            tradeGUI.refresh(session);
            return;
        }
        if (!eco.hasEnough(session.getPlayerB(), moneyB)) {
            b.sendMessage(plugin.getLangManager().component("not-enough-money"));
            session.resetReady();
            tradeGUI.refresh(session);
            return;
        }

        ItemStack[] itemsA = tradeGUI.collectOfferItems(a, session);
        ItemStack[] itemsB = tradeGUI.collectOfferItems(b, session);

        if (!eco.withdraw(session.getPlayerA(), moneyA, "trade-offer")) {
            session.resetReady();
            tradeGUI.refresh(session);
            return;
        }
        if (!eco.withdraw(session.getPlayerB(), moneyB, "trade-offer")) {
            eco.deposit(session.getPlayerA(), moneyA, "trade-rollback");
            session.resetReady();
            tradeGUI.refresh(session);
            return;
        }

        eco.deposit(session.getPlayerB(), moneyA, "trade-received");
        eco.deposit(session.getPlayerA(), moneyB, "trade-received");

        giveItems(a, itemsB);
        giveItems(b, itemsA);

        a.sendMessage(plugin.getLangManager().component("trade.complete"));
        b.sendMessage(plugin.getLangManager().component("trade.complete"));

        Bukkit.getPluginManager().callEvent(new TradeCompleteEvent(a, b, session));
        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());
        a.closeInventory();
        b.closeInventory();
    }

    public void onPlayerQuit(Player player) {
        pendingInvites.remove(player.getUniqueId());
        pendingInvites.entrySet().removeIf(e -> e.getValue().inviter().equals(player.getUniqueId()));

        TradeSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        UUID partnerId = session.getPartner(player.getUniqueId());
        Player partner = Bukkit.getPlayer(partnerId);

        tradeGUI.returnItems(player, session);
        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());

        if (partner != null && partner.isOnline()) {
            tradeGUI.returnItems(partner, session);
            partner.closeInventory();
            partner.sendMessage(plugin.getLangManager().component("trade.partner-quit",
                "player", player.getName()));
        }
    }

    public void shutdown() {
        if (inviteCleanupTask != null) inviteCleanupTask.cancel();
        var sessions = new java.util.LinkedHashSet<>(activeSessions.values());
        for (TradeSession session : sessions) {
            abortSession(session, null);
        }
        pendingInvites.clear();
    }

    private void abortSession(TradeSession session, String messageKey) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a != null) {
            tradeGUI.returnItems(a, session);
            a.closeInventory();
            if (messageKey != null) a.sendMessage(plugin.getLangManager().component(messageKey));
        }
        if (b != null) {
            tradeGUI.returnItems(b, session);
            b.closeInventory();
            if (messageKey != null) b.sendMessage(plugin.getLangManager().component(messageKey));
        }
        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());
    }

    private void giveItems(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            leftover.values().forEach(stack ->
                player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    private void startInviteCleanup() {
        inviteCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, TradeInvite>> it = pendingInvites.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, TradeInvite> entry = it.next();
                if (now > entry.getValue().expiresAt()) {
                    UUID targetId = entry.getKey();
                    UUID inviterId = entry.getValue().inviter();
                    it.remove();
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        target.sendMessage(plugin.getLangManager().component("trade.invite-expired"));
                    }
                    Player inviter = Bukkit.getPlayer(inviterId);
                    if (inviter != null) {
                        inviter.sendMessage(plugin.getLangManager().component("trade.invite-expired"));
                    }
                }
            }
        }, 20L, 20L);
    }
}
