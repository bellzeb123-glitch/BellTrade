package pl.bell.trade.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.bell.trade.BellTrade;
import pl.bell.trade.model.TradeSession;
import pl.bell.trade.trade.TradeManager;

import java.util.UUID;

public class TradeGuiListener implements Listener {

    private final BellTrade plugin;
    private final TradeManager tradeManager;

    public TradeGuiListener(BellTrade plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        if (holder.getType() != GuiHolder.Type.TRADE) return;

        TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null || !session.getSessionId().equals(holder.getSessionId())) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();
        if (raw >= 54) {
            if (event.isShiftClick() && event.getCurrentItem() != null) {
                event.setCancelled(true);
            }
            return;
        }

        if (TradeGUI.isPartnerOfferSlot(raw)) {
            event.setCancelled(true);
            return;
        }

        if (TradeGUI.isSelfOfferSlot(raw)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> onOfferChanged(player, session));
            return;
        }

        event.setCancelled(true);

        if (raw == TradeGUI.SLOT_CANCEL) {
            tradeManager.cancelTrade(player);
            return;
        }

        if (raw == TradeGUI.SLOT_CONFIRM) {
            UUID id = player.getUniqueId();
            session.setReady(id, !session.isReady(id));
            plugin.getTradeGUI().refresh(session);
            tradeManager.tryComplete(session);
            return;
        }

        handleMoneyClick(player, session, raw);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        if (holder.getType() != GuiHolder.Type.TRADE) return;

        for (int raw : event.getRawSlots()) {
            if (raw < 54 && !TradeGUI.isSelfOfferSlot(raw)) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TradeSession session = tradeManager.getSession(player.getUniqueId());
            if (session != null) onOfferChanged(player, session);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        if (holder.getType() != GuiHolder.Type.TRADE) return;

        TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (tradeManager.getSession(player.getUniqueId()) == session) {
                tradeManager.cancelTrade(player);
            }
        });
    }

    private void onOfferChanged(Player player, TradeSession session) {
        plugin.getTradeGUI().syncSessionFromInventory(player, session);
        if (plugin.getConfig().getBoolean("trade.reset-ready-on-change", true)) {
            session.resetReady();
        }
        plugin.getTradeGUI().refresh(session);
    }

    private void handleMoneyClick(Player player, TradeSession session, int raw) {
        double delta = switch (raw) {
            case TradeGUI.SLOT_MONEY_MINUS_10 -> -10;
            case TradeGUI.SLOT_MONEY_MINUS_1 -> -1;
            case TradeGUI.SLOT_MONEY_PLUS_1 -> 1;
            case TradeGUI.SLOT_MONEY_PLUS_10 -> 10;
            default -> 0;
        };
        if (delta == 0) return;

        double max = plugin.getConfig().getDouble("trade.max-money-offer", 1_000_000.0);
        double newAmount = Math.min(max, Math.max(0, session.getMoney(player.getUniqueId()) + delta));
        session.setMoney(player.getUniqueId(), newAmount);

        if (plugin.getConfig().getBoolean("trade.reset-ready-on-change", true)) {
            session.resetReady();
        }
        plugin.getTradeGUI().refresh(session);
    }
}
