package pl.bell.trade.market;

import org.bukkit.inventory.ItemStack;
import pl.bell.trade.model.Listing;
import pl.bell.trade.storage.Database;
import pl.bell.trade.util.ItemSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListingRepository {

    private final Database database;
    private final Logger logger;

    public ListingRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public long insert(UUID seller, ItemStack item, double price, long createdAt, long expiresAt) throws SQLException {
        String material = item.getType().name();
        byte[] blob = ItemSerializer.toBytes(item);
        String sql = "INSERT INTO listings (seller_uuid, item_blob, price, material, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = database.conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.toString());
            ps.setBytes(2, blob);
            ps.setDouble(3, price);
            ps.setString(4, material);
            ps.setLong(5, createdAt);
            ps.setLong(6, expiresAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key for listing insert");
    }

    public void delete(long id) {
        database.async(() -> {
            try (PreparedStatement ps = database.conn().prepareStatement("DELETE FROM listings WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to delete listing " + id, e);
            }
        });
    }

    public void deleteSync(long id) throws SQLException {
        try (PreparedStatement ps = database.conn().prepareStatement("DELETE FROM listings WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Listing> findById(long id) {
        String sql = "SELECT id, seller_uuid, item_blob, price, created_at, expires_at FROM listings WHERE id = ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find listing " + id, e);
        }
        return Optional.empty();
    }

    public List<Listing> findActive(int offset, int limit, String materialFilter) {
        List<Listing> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        String sql = materialFilter == null
            ? "SELECT id, seller_uuid, item_blob, price, created_at, expires_at FROM listings WHERE expires_at > ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
            : "SELECT id, seller_uuid, item_blob, price, created_at, expires_at FROM listings WHERE expires_at > ? AND material = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            int idx = 1;
            ps.setLong(idx++, now);
            if (materialFilter != null) ps.setString(idx++, materialFilter.toUpperCase());
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to list market listings", e);
        }
        return list;
    }

    public List<Listing> findBySeller(UUID seller) {
        List<Listing> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        String sql = "SELECT id, seller_uuid, item_blob, price, created_at, expires_at FROM listings WHERE seller_uuid = ? AND expires_at > ? ORDER BY created_at DESC";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setString(1, seller.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to list seller listings", e);
        }
        return list;
    }

    public int countBySeller(UUID seller) {
        long now = System.currentTimeMillis();
        String sql = "SELECT COUNT(*) FROM listings WHERE seller_uuid = ? AND expires_at > ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setString(1, seller.toString());
            ps.setLong(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count listings", e);
        }
        return 0;
    }

    public List<Listing> findExpired() {
        List<Listing> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        String sql = "SELECT id, seller_uuid, item_blob, price, created_at, expires_at FROM listings WHERE expires_at <= ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find expired listings", e);
        }
        return list;
    }

    public int countActive(String materialFilter) {
        long now = System.currentTimeMillis();
        String sql = materialFilter == null
            ? "SELECT COUNT(*) FROM listings WHERE expires_at > ?"
            : "SELECT COUNT(*) FROM listings WHERE expires_at > ? AND material = ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setLong(1, now);
            if (materialFilter != null) ps.setString(2, materialFilter.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count active listings", e);
        }
        return 0;
    }

    public java.util.Map<String, Integer> countActiveByMaterial() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        long now = System.currentTimeMillis();
        String sql = "SELECT material, COUNT(*) AS cnt FROM listings WHERE expires_at > ? GROUP BY material";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("material"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count listings by material", e);
        }
        return counts;
    }

    private Listing mapRow(ResultSet rs) throws SQLException {
        ItemStack item = ItemSerializer.fromBytes(rs.getBytes("item_blob"));
        if (item == null) item = new ItemStack(org.bukkit.Material.BARRIER);
        return new Listing(
            rs.getLong("id"),
            UUID.fromString(rs.getString("seller_uuid")),
            item,
            rs.getDouble("price"),
            rs.getLong("created_at"),
            rs.getLong("expires_at")
        );
    }
}
