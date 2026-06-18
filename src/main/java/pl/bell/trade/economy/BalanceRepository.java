package pl.bell.trade.economy;

import pl.bell.trade.storage.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BalanceRepository {

    private final Database database;
    private final Logger logger;

    public BalanceRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void loadAll(java.util.Map<UUID, Double> target) {
        target.clear();
        try (var st = database.conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, balance FROM balances")) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    target.put(uuid, rs.getDouble("balance"));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load balances", e);
        }
    }

    public void saveAsync(UUID uuid, double balance) {
        database.async(() -> upsert(uuid, balance));
    }

    public void upsert(UUID uuid, double balance) {
        String sql = "INSERT INTO balances (uuid, balance, updated_at) VALUES (?, ?, ?) " +
            "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance, updated_at = excluded.updated_at";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save balance for " + uuid, e);
        }
    }

    public List<BalanceEntry> getTop(int limit) {
        List<BalanceEntry> top = new ArrayList<>();
        String sql = "SELECT uuid, balance FROM balances ORDER BY balance DESC LIMIT ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        top.add(new BalanceEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getDouble("balance")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load baltop", e);
        }
        return top;
    }

    public double sumAllBalances() {
        try (var st = database.conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(balance), 0) FROM balances")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to sum balances", e);
        }
        return 0;
    }

    public record BalanceEntry(UUID uuid, double balance) {}
}
