package pl.bell.trade.integration;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Soft integration with BellLands — no compile-time dependency.
 * Optional trade gate: players on a claim must be owner or trusted.
 */
public final class BellLandsHook {

    private final BellTrade plugin;
    private final boolean active;
    private Object landManager;
    private Method getLandAt;
    private Method landGetOwner;
    private Method landIsTrusted;

    public BellLandsHook(BellTrade plugin) {
        this.plugin = plugin;
        this.active = init();
    }

    public boolean isActive() {
        return active;
    }

    public boolean canTradeOnClaim(Player player) {
        if (!active || !plugin.getConfig().getBoolean("integrations.belllands.trade-require-trust", false)) {
            return true;
        }
        try {
            @SuppressWarnings("unchecked")
            Optional<Object> opt = (Optional<Object>) getLandAt.invoke(landManager, player.getLocation().getChunk());
            if (opt == null || opt.isEmpty()) return true;

            Object land = opt.get();
            UUID owner = (UUID) landGetOwner.invoke(land);
            UUID uuid = player.getUniqueId();
            if (owner.equals(uuid)) return true;
            return (boolean) landIsTrusted.invoke(land, uuid);
        } catch (Exception e) {
            plugin.getLogger().fine("BellLands claim check failed: " + e.getMessage());
            return true;
        }
    }

    private boolean init() {
        if (Bukkit.getPluginManager().getPlugin("BellLands") == null) return false;
        try {
            Class<?> bellLands = Class.forName("pl.bell.lands.BellLands");
            Object instance = bellLands.getMethod("getInstance").invoke(null);
            landManager = bellLands.getMethod("getLandManager").invoke(instance);
            getLandAt = landManager.getClass().getMethod("getLandAt", Chunk.class);

            Class<?> landClass = Class.forName("pl.bell.lands.model.Land");
            landGetOwner = landClass.getMethod("getOwner");
            landIsTrusted = landClass.getMethod("isTrusted", UUID.class);

            plugin.getLogger().info("BellLands integration active.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("BellLands integration unavailable: " + e.getMessage());
            return false;
        }
    }
}
