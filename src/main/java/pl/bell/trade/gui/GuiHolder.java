package pl.bell.trade.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuiHolder implements InventoryHolder {

    public enum Type {
        TRADE,
        ADMIN,
        MARKET,
        SELL_SHOP,
        MAIN_MENU,
        TRADE_HELP,
        SHOP_PRICE_EDITOR,
        ECONOMY_HEALTH
    }

    private final Type type;
    private final UUID sessionId;
    private Inventory inventory;

    public GuiHolder(Type type, UUID sessionId) {
        this.type = type;
        this.sessionId = sessionId;
    }

    public GuiHolder(Type type) {
        this(type, null);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Type getType() {
        return type;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
