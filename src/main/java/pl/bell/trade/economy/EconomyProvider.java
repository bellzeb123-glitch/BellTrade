package pl.bell.trade.economy;

import java.util.UUID;

public interface EconomyProvider {

    double getBalance(UUID uuid);

    boolean hasEnough(UUID uuid, double amount);

    boolean withdraw(UUID uuid, double amount, String reason);

    boolean deposit(UUID uuid, double amount, String reason);

    void setBalance(UUID uuid, double amount, String reason);

    String format(double amount);
}
