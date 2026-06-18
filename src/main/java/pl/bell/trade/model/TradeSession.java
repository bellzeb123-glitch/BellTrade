package pl.bell.trade.model;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class TradeSession {

    public static final int OFFER_SIZE = 4;

    private final UUID sessionId;
    private final UUID playerA;
    private final UUID playerB;
    private final ItemStack[] offerA = new ItemStack[OFFER_SIZE];
    private final ItemStack[] offerB = new ItemStack[OFFER_SIZE];
    private double moneyA;
    private double moneyB;
    private boolean readyA;
    private boolean readyB;

    public TradeSession(UUID playerA, UUID playerB) {
        this.sessionId = UUID.randomUUID();
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getPlayerA() { return playerA; }
    public UUID getPlayerB() { return playerB; }

    public UUID getPartner(UUID player) {
        if (player.equals(playerA)) return playerB;
        if (player.equals(playerB)) return playerA;
        return null;
    }

    public boolean isPlayerA(UUID player) {
        return playerA.equals(player);
    }

    public ItemStack[] getOffer(UUID player) {
        return isPlayerA(player) ? offerA : offerB;
    }

    public ItemStack[] getPartnerOffer(UUID player) {
        return isPlayerA(player) ? offerB : offerA;
    }

    public double getMoney(UUID player) {
        return isPlayerA(player) ? moneyA : moneyB;
    }

    public void setMoney(UUID player, double amount) {
        if (amount < 0) amount = 0;
        if (isPlayerA(player)) moneyA = amount;
        else moneyB = amount;
    }

    public boolean isReady(UUID player) {
        return isPlayerA(player) ? readyA : readyB;
    }

    public void setReady(UUID player, boolean ready) {
        if (isPlayerA(player)) readyA = ready;
        else readyB = ready;
    }

    public void resetReady() {
        readyA = false;
        readyB = false;
    }

    public boolean bothReady() {
        return readyA && readyB;
    }

    public ItemStack[] getOfferA() { return Arrays.copyOf(offerA, OFFER_SIZE); }
    public ItemStack[] getOfferB() { return Arrays.copyOf(offerB, OFFER_SIZE); }
    public double getMoneyA() { return moneyA; }
    public double getMoneyB() { return moneyB; }
}
