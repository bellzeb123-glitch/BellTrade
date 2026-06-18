package pl.bell.trade.engine;

import pl.bell.trade.BellTrade;
import pl.bell.trade.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class PriceHistoryRepository {

    private final Database database;
    private final java.util.logging.Logger logger;

    public PriceHistoryRepository(Database database, java.util.logging.Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void insert(String itemKey, double unitPrice, int amount, String engineVersion, long recordedAt) {
        database.async(() -> {
            try (PreparedStatement ps = database.conn().prepareStatement(
                "INSERT INTO price_history (item_key, price, amount, engine_version, recorded_at) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, itemKey);
                ps.setDouble(2, unitPrice);
                ps.setInt(3, amount);
                ps.setString(4, engineVersion);
                ps.setLong(5, recordedAt);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to insert price history for " + itemKey, e);
            }
        });
    }

    public int countRecentSells(String itemKey, long sinceMs) {
        try (PreparedStatement ps = database.conn().prepareStatement(
            "SELECT COALESCE(SUM(amount), 0) FROM price_history WHERE item_key = ? AND recorded_at >= ?")) {
            ps.setString(1, itemKey);
            ps.setLong(2, sinceMs);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count recent sells for " + itemKey, e);
            return 0;
        }
    }

    public Map<String, Integer> recentSellTotals(long sinceMs) {
        Map<String, Integer> totals = new HashMap<>();
        try (PreparedStatement ps = database.conn().prepareStatement(
            "SELECT item_key, COALESCE(SUM(amount), 0) AS total FROM price_history WHERE recorded_at >= ? GROUP BY item_key")) {
            ps.setLong(1, sinceMs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("item_key"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load recent sell totals", e);
        }
        return totals;
    }

    public int countTransactionsSince(long sinceMs) {
        try (PreparedStatement ps = database.conn().prepareStatement(
            "SELECT COUNT(*) FROM price_history WHERE recorded_at >= ?")) {
            ps.setLong(1, sinceMs);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count price history transactions", e);
            return 0;
        }
    }
}
