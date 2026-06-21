package pl.bell.trade.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class ItemKey {

    private final Material material;
    private final String customId;

    private ItemKey(Material material, String customId) {
        this.material = material;
        this.customId = customId;
    }

    public static ItemKey of(Material material) {
        Objects.requireNonNull(material, "material");
        return new ItemKey(material, null);
    }

    public static ItemKey ofBellItem(Material material, String bellItemId) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(bellItemId, "bellItemId");
        return new ItemKey(material, bellItemId.toLowerCase());
    }

    public static ItemKey fromStack(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        Optional<String> bellId = resolveBellItemsId(stack);
        if (bellId.isPresent()) {
            return ofBellItem(stack.getType(), bellId.get());
        }
        return of(stack.getType());
    }

    public static ItemKey parse(String key) {
        if (key != null && key.startsWith("bellitems:")) {
            String id = key.substring("bellitems:".length());
            return new ItemKey(Material.PAPER, id);
        }
        return of(Material.valueOf(key.toUpperCase()));
    }

    private static Optional<String> resolveBellItemsId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return Optional.empty();
        try {
            Class<?> apiClass = Class.forName("pl.bell.bellitems.api.BellItemsAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            var result = (Optional<String>) apiClass.getMethod("getItemId", ItemStack.class).invoke(api, stack);
            return result != null ? result : Optional.empty();
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public Material getMaterial() {
        return material;
    }

    public Optional<String> getCustomId() {
        return Optional.ofNullable(customId);
    }

    public boolean isBellItem() {
        return customId != null;
    }

    public String key() {
        return customId != null ? "bellitems:" + customId : material.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey itemKey)) return false;
        return material == itemKey.material && Objects.equals(customId, itemKey.customId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, customId);
    }

    @Override
    public String toString() {
        return key();
    }
}
