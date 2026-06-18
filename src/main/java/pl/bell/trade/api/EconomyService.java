package pl.bell.trade.api;

import pl.bell.trade.economy.CurrencyManager;
import pl.bell.trade.economy.EconomyProvider;

import java.util.UUID;

public class EconomyService implements EconomyProvider {

    private final CurrencyManager currencyManager;

    public EconomyService(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    @Override
    public double getBalance(UUID uuid) {
        return currencyManager.getBalance(uuid);
    }

    @Override
    public boolean hasEnough(UUID uuid, double amount) {
        return currencyManager.hasEnough(uuid, amount);
    }

    @Override
    public boolean withdraw(UUID uuid, double amount, String reason) {
        return currencyManager.withdraw(uuid, amount, reason);
    }

    @Override
    public boolean deposit(UUID uuid, double amount, String reason) {
        return currencyManager.deposit(uuid, amount, reason);
    }

    @Override
    public void setBalance(UUID uuid, double amount, String reason) {
        currencyManager.setBalance(uuid, amount, reason);
    }

    @Override
    public String format(double amount) {
        return currencyManager.format(amount);
    }
}
