package pl.bell.trade.storage;

import pl.bell.trade.BellTrade;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Database {

    private Connection conn;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BellTrade-DB");
        t.setDaemon(true);
        return t;
    });

    public void init(File dbFile) throws SQLException, ClassNotFoundException {
        dbFile.getParentFile().mkdirs();
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("CREATE TABLE IF NOT EXISTS balances (" +
                "uuid TEXT PRIMARY KEY, balance REAL NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_balances_balance ON balances(balance DESC)");
            st.execute("CREATE TABLE IF NOT EXISTS listings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "seller_uuid TEXT NOT NULL, " +
                "item_blob BLOB NOT NULL, " +
                "price REAL NOT NULL, " +
                "material TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "expires_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_listings_seller ON listings(seller_uuid)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_listings_expires ON listings(expires_at)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_listings_material ON listings(material)");
            st.execute("CREATE TABLE IF NOT EXISTS expired_mailbox (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "owner_uuid TEXT NOT NULL, " +
                "item_blob BLOB NOT NULL, " +
                "expired_at INTEGER NOT NULL, " +
                "listing_id INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_expired_mailbox_owner ON expired_mailbox(owner_uuid)");
            st.execute("CREATE TABLE IF NOT EXISTS supply_samples (" +
                "item_key TEXT NOT NULL, quantity INTEGER NOT NULL, sampled_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_supply_samples_key_time ON supply_samples(item_key, sampled_at)");
            st.execute("CREATE TABLE IF NOT EXISTS price_history (" +
                "item_key TEXT NOT NULL, price REAL NOT NULL, amount INTEGER NOT NULL DEFAULT 1, " +
                "engine_version TEXT NOT NULL, recorded_at INTEGER NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_price_history_key_time ON price_history(item_key, recorded_at)");
        }
    }

    public Connection conn() {
        return conn;
    }

    public void async(Runnable task) {
        writer.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                BellTrade.getInstance().getLogger().log(Level.SEVERE, "Database write failed", e);
            }
        });
    }

    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                BellTrade.getInstance().getLogger().warning("DB writer did not finish in 30s");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {}
    }
}
