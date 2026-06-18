package pl.bell.trade.model;

import org.bukkit.Material;

import java.util.Objects;

public final class ItemKey {

    private final Material material;

    private ItemKey(Material material) {
        this.material = material;
    }

    public static ItemKey of(Material material) {
        Objects.requireNonNull(material, "material");
        return new ItemKey(material);
    }

    public static ItemKey parse(String key) {
        return of(Material.valueOf(key.toUpperCase()));
    }

    public Material getMaterial() {
        return material;
    }

    public String key() {
        return material.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemKey itemKey)) return false;
        return material == itemKey.material;
    }

    @Override
    public int hashCode() {
        return material.hashCode();
    }

    @Override
    public String toString() {
        return key();
    }
}
