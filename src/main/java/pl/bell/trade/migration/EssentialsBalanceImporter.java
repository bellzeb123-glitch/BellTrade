package pl.bell.trade.migration;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Jednorazowy import sald z EssentialsX (folder userdata/*.yml, pole {@code money}).
 * AuctionHouse i inne pluginy AH korzystaly z Vault → salda sa w Essentials.
 */
public class EssentialsBalanceImporter {

    public record Result(
        int filesScanned,
        int withBalance,
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

    public EssentialsBalanceImporter(Logger logger) {
        this.logger = logger;
    }

    public Result scan(File userdataDir, boolean skipZero) {
        if (!userdataDir.isDirectory()) {
            return new Result(0, 0, 0, 0, Map.of(),
                "Nie znaleziono folderu userdata: " + userdataDir.getAbsolutePath());
        }

        File[] files = userdataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return new Result(0, 0, 0, 0, Map.of(),
                "Brak plikow .yml w: " + userdataDir.getAbsolutePath());
        }

        Map<UUID, Double> balances = new HashMap<>();
        int skipped = 0;
        double total = 0;

        for (File file : files) {
            String name = file.getName();
            String baseName = name.substring(0, name.length() - 4);

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (!yaml.contains("money")) {
                skipped++;
                continue;
            }

            UUID uuid = resolveUuid(baseName, yaml);
            if (uuid == null) {
                skipped++;
                continue;
            }

            double money = yaml.getDouble("money", 0);
            if (skipZero && money <= 0) {
                skipped++;
                continue;
            }

            balances.put(uuid, money);
            total += money;
        }

        logger.info("[BellTrade] Essentials scan: " + files.length + " plikow, "
            + balances.size() + " sald, pominieto " + skipped);
        return new Result(files.length, balances.size(), skipped, total, balances, null);
    }

    private UUID resolveUuid(String fileBaseName, YamlConfiguration yaml) {
        try {
            return UUID.fromString(fileBaseName);
        } catch (IllegalArgumentException ignored) {
            String nick = yaml.getString("last-account-name", fileBaseName);
            if (nick == null || nick.isBlank()) return null;
            return Bukkit.getOfflinePlayer(nick).getUniqueId();
        }
    }
}
