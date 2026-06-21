package pl.bell.trade.market;

import org.bukkit.inventory.ItemStack;
import pl.bell.trade.model.ExpiredMailboxEntry;
import pl.bell.trade.storage.Database;
import pl.bell.trade.util.ItemSerializer;

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

public class ExpiredMailboxRepository {

    private final Database database;
    private final Logger logger;

    public ExpiredMailboxRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public long insert(UUID owner, ItemStack item, long expiredAt, long listingId) throws SQLException {
        byte[] blob = ItemSerializer.toBytes(item);
        String sql = "INSERT INTO expired_mailbox (owner_uuid, item_blob, expired_at, listing_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = database.conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString());
            ps.setBytes(2, blob);
            ps.setLong(3, expiredAt);
            ps.setLong(4, listingId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key for expired_mailbox insert");
    }

    public void insertAsync(UUID owner, ItemStack item, long expiredAt, long listingId) {
        database.async(() -> {
            try {
                insert(owner, item, expiredAt, listingId);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to store expired item for " + owner, e);
            }
        });
    }

    public Optional<ExpiredMailboxEntry> findById(long id) {
        String sql = "SELECT id, owner_uuid, item_blob, expired_at, listing_id FROM expired_mailbox WHERE id = ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find expired mailbox entry " + id, e);
        }
        return Optional.empty();
    }

    public List<ExpiredMailboxEntry> findByOwner(UUID owner, int offset, int limit) {
        List<ExpiredMailboxEntry> list = new ArrayList<>();
        String sql = "SELECT id, owner_uuid, item_blob, expired_at, listing_id FROM expired_mailbox "
            + "WHERE owner_uuid = ? ORDER BY expired_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to list expired mailbox for " + owner, e);
        }
        return list;
    }

    public int countByOwner(UUID owner) {
        String sql = "SELECT COUNT(*) FROM expired_mailbox WHERE owner_uuid = ?";
        try (PreparedStatement ps = database.conn().prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count expired mailbox for " + owner, e);
        }
        return 0;
    }

    public boolean deleteSync(long id, UUID owner) throws SQLException {
        try (PreparedStatement ps = database.conn().prepareStatement(
            "DELETE FROM expired_mailbox WHERE id = ? AND owner_uuid = ?")) {
            ps.setLong(1, id);
            ps.setString(2, owner.toString());
            return ps.executeUpdate() > 0;
        }
    }

    private ExpiredMailboxEntry mapRow(ResultSet rs) throws SQLException {
        ItemStack item = ItemSerializer.fromBytes(rs.getBytes("item_blob"));
        if (item == null) item = new ItemStack(org.bukkit.Material.BARRIER);
        return new ExpiredMailboxEntry(
            rs.getLong("id"),
            UUID.fromString(rs.getString("owner_uuid")),
            item,
            rs.getLong("expired_at"),
            rs.getLong("listing_id")
        );
    }
}
