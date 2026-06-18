package pl.bell.trade.engine;

import pl.bell.trade.storage.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SupplySampleRepository {

    private final Database database;
    private final Logger logger;

    public SupplySampleRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void insertBatch(Map<String, Long> quantities, long sampledAt) {
        if (quantities.isEmpty()) return;
        database.async(() -> {
            try (PreparedStatement ps = database.conn().prepareStatement(
                "INSERT INTO supply_samples (item_key, quantity, sampled_at) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Long> e : quantities.entrySet()) {
                    ps.setString(1, e.getKey());
                    ps.setLong(2, e.getValue());
                    ps.setLong(3, sampledAt);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Failed to insert supply samples", ex);
            }
        });
    }

    public Map<String, Long> latestTotals(long sinceMs) {
        Map<String, Long> totals = new HashMap<>();
        try (PreparedStatement ps = database.conn().prepareStatement(
            "SELECT item_key, MAX(quantity) AS qty FROM supply_samples WHERE sampled_at >= ? GROUP BY item_key")) {
            ps.setLong(1, sinceMs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("item_key"), rs.getLong("qty"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load supply samples", e);
        }
        return totals;
    }
}
