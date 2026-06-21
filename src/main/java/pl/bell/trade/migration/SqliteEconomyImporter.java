package pl.bell.trade.migration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Import sald z SQLite economy.db (stary ekosystem serwera Bell — tabela {@code economy}).
 */
public class SqliteEconomyImporter {

    public record Result(
        int rowsRead,
        int imported,
        int skipped,
        double totalAmount,
        Map<UUID, Double> balances,
        String error
    ) {
        public boolean success() {
            return error == null;
        }
    }

    private final Logger logger;

    public SqliteEconomyImporter(Logger logger) {
        this.logger = logger;
    }

    public Result scan(File dbFile, boolean skipZero) {
        if (!dbFile.isFile()) {
            return new Result(0, 0, 0, 0, Map.of(),
                "Nie znaleziono pliku bazy: " + dbFile.getAbsolutePath());
        }

        Map<UUID, Double> balances = new HashMap<>();
        int skipped = 0;
        double total = 0;
        int read = 0;

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            return new Result(0, 0, 0, 0, Map.of(), "Brak sterownika SQLite w jar.");
        }

        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, balance FROM economy")) {

            while (rs.next()) {
                read++;
                String uuidStr = rs.getString("uuid");
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ex) {
                    skipped++;
                    continue;
                }
                double balance = rs.getDouble("balance");
                if (skipZero && balance <= 0) {
                    skipped++;
                    continue;
                }
                balances.put(uuid, balance);
                total += balance;
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("no such table")) {
                return new Result(0, 0, 0, 0, Map.of(),
                    "Brak tabeli economy w pliku. Oczekiwany stary format economy.db.");
            }
            logger.severe("[BellTrade] SQLite economy import error: " + ex.getMessage());
            return new Result(0, 0, 0, 0, Map.of(), "Blad odczytu SQLite: " + ex.getMessage());
        }

        logger.info("[BellTrade] SQLite economy scan: " + read + " wierszy, "
            + balances.size() + " sald, pominieto " + skipped);
        return new Result(read, balances.size(), skipped, total, balances, null);
    }
}
