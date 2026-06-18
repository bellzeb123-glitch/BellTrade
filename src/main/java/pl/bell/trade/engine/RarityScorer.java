package pl.bell.trade.engine;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pl.bell.trade.model.ItemKey;

import java.util.EnumMap;
import java.util.Map;

public class RarityScorer {

    private static final Map<Material, Double> TIER_SCORES = new EnumMap<>(Material.class);

    static {
        putTier(0.95, "NETHERITE");
        putTier(0.90, "ANCIENT_DEBRIS", "ECHO_SHARD", "DRAGON_EGG", "ELYTRA");
        putTier(0.85, "DIAMOND", "EMERALD");
        putTier(0.75, "GOLD", "LAPIS", "QUARTZ");
        putTier(0.60, "IRON", "COPPER", "REDSTONE");
        putTier(0.45, "COAL", "CHARCOAL");
        putTier(0.35, "WHEAT", "CARROT", "POTATO", "OAK", "COBBLESTONE", "DIRT", "SAND");
    }

    private static void putTier(double score, String... keywords) {
        for (Material mat : Material.values()) {
            String name = mat.name();
            for (String kw : keywords) {
                if (name.contains(kw)) {
                    TIER_SCORES.putIfAbsent(mat, score);
                }
            }
        }
    }

    public double score(ItemKey key, ItemStack stack) {
        double base = TIER_SCORES.getOrDefault(key.getMaterial(), 0.5);
        if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasEnchants()) {
            base = Math.min(1.0, base + 0.08);
        }
        return Math.max(0.1, Math.min(1.0, base));
    }
}
