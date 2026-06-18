package pl.bell.trade.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.trade.BellTrade;
import pl.bell.trade.model.ShopCategory;
import pl.bell.trade.model.ShopItemEntry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loads sell-shop categories and item prices from {@code shop/categories/*.yml}.
 * <p>
 * <b>Free:</b> read-only at runtime; admins edit YAML on disk and {@code /btrade reload}.
 * <b>Pro:</b> {@code ShopCatalogEditorGUI} writes the same files (add/remove categories and items).
 */
public class ShopConfigManager {

    private final BellTrade plugin;
    private List<ShopCategory> categories = List.of();
    private Map<String, ShopItemEntry> itemsByMaterial = Map.of();
    private Map<String, File> categoryFiles = Map.of();
    private Set<Material> blacklist = Set.of();

    public ShopConfigManager(BellTrade plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ensureShopFiles();
        loadBlacklist();
        loadCategories();
    }

    public List<ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String id) {
        for (ShopCategory cat : categories) {
            if (cat.getId().equalsIgnoreCase(id)) return cat;
        }
        return null;
    }

    public ShopItemEntry getItemEntry(Material material) {
        return itemsByMaterial.get(material.name());
    }

    public boolean isSellable(Material material) {
        return itemsByMaterial.containsKey(material.name()) && !blacklist.contains(material);
    }

    public boolean isBlacklisted(Material material) {
        return blacklist.contains(material);
    }

    public File getCategoryFile(String categoryId) {
        return categoryFiles.get(categoryId);
    }

    public boolean updateItemPrice(String categoryId, Material material, double basePrice, double minPrice, double maxPrice) {
        File file = categoryFiles.get(categoryId);
        if (file == null || !file.exists()) return false;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "items." + material.name();
        if (!yaml.contains(path)) return false;

        yaml.set(path + ".base-price", basePrice);
        yaml.set(path + ".min-price", minPrice);
        yaml.set(path + ".max-price", maxPrice);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save shop price for " + material, e);
            return false;
        }
        reload();
        return true;
    }

    public boolean addCategory(String id, Material icon, String displayName) {
        if (id == null || id.isBlank() || categoryFiles.containsKey(id)) return false;
        File file = new File(new File(plugin.getDataFolder(), "shop/categories"), sanitizeFileName(id) + ".yml");
        if (file.exists()) return false;

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", id);
        yaml.set("icon", icon.name());
        yaml.set("name", displayName != null ? displayName : id);
        yaml.set("items", null);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create category " + id, e);
            return false;
        }
        reload();
        return true;
    }

    public boolean removeCategory(String id) {
        File file = categoryFiles.get(id);
        if (file == null || !file.exists()) return false;
        if (!file.delete()) return false;
        reload();
        return true;
    }

    public boolean addItem(String categoryId, Material material, double basePrice, double minPrice, double maxPrice) {
        return addItem(categoryId, material, basePrice, minPrice, maxPrice, 1);
    }

    public boolean addItem(String categoryId, Material material, double basePrice, double minPrice, double maxPrice, int unitSize) {
        File file = categoryFiles.get(categoryId);
        if (file == null || !file.exists() || material == null) return false;
        if (itemsByMaterial.containsKey(material.name())) return false;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "items." + material.name();
        yaml.set(path + ".base-price", basePrice);
        yaml.set(path + ".min-price", minPrice);
        yaml.set(path + ".max-price", maxPrice);
        yaml.set(path + ".unit-size", Math.max(1, unitSize));
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add item " + material, e);
            return false;
        }
        reload();
        return true;
    }

    public boolean removeItem(String categoryId, Material material) {
        File file = categoryFiles.get(categoryId);
        if (file == null || !file.exists() || material == null) return false;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "items." + material.name();
        if (!yaml.contains(path)) return false;
        yaml.set(path, null);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove item " + material, e);
            return false;
        }
        reload();
        return true;
    }

    private String sanitizeFileName(String id) {
        return id.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }

    private void ensureShopFiles() {
        File shopDir = new File(plugin.getDataFolder(), "shop");
        File categoriesDir = new File(shopDir, "categories");
        if (!categoriesDir.exists()) categoriesDir.mkdirs();

        copyDefault("shop/blacklist.yml", new File(shopDir, "blacklist.yml"));
        copyDefault("shop/categories/ores.yml", new File(categoriesDir, "ores.yml"));
        copyDefault("shop/categories/crops.yml", new File(categoriesDir, "crops.yml"));
        copyDefault("shop/categories/mob_drops.yml", new File(categoriesDir, "mob_drops.yml"));
        copyDefault("shop/categories/wood.yml", new File(categoriesDir, "wood.yml"));
    }

    private void copyDefault(String resourcePath, File target) {
        if (target.exists()) return;
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Missing default shop resource: " + resourcePath);
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to copy " + resourcePath, e);
        }
    }

    private void loadBlacklist() {
        Set<Material> blocked = new HashSet<>();
        for (String mat : plugin.getConfig().getStringList("shop.blacklist")) {
            Material m = Material.matchMaterial(mat);
            if (m != null) blocked.add(m);
        }
        File file = new File(plugin.getDataFolder(), "shop/blacklist.yml");
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (String mat : yaml.getStringList("materials")) {
                Material m = Material.matchMaterial(mat);
                if (m != null) blocked.add(m);
            }
        }
        blacklist = Collections.unmodifiableSet(blocked);
    }

    private void loadCategories() {
        File dir = new File(plugin.getDataFolder(), "shop/categories");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            categories = List.of();
            itemsByMaterial = Map.of();
            categoryFiles = Map.of();
            return;
        }

        List<ShopCategory> loaded = new ArrayList<>();
        Map<String, ShopItemEntry> itemMap = new HashMap<>();
        Map<String, File> fileMap = new HashMap<>();

        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String id = yaml.getString("id", file.getName().replace(".yml", ""));
            Material icon = Material.matchMaterial(yaml.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;
            String name = yaml.getString("name", id);

            ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
            List<ShopItemEntry> items = new ArrayList<>();
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    Material mat = Material.matchMaterial(key);
                    if (mat == null) {
                        plugin.getLogger().warning("Unknown material in " + file.getName() + ": " + key);
                        continue;
                    }
                    ConfigurationSection entry = itemsSection.getConfigurationSection(key);
                    double base = entry != null ? entry.getDouble("base-price", 1.0) : 1.0;
                    double min = entry != null ? entry.getDouble("min-price", base * 0.2) : base * 0.2;
                    double max = entry != null ? entry.getDouble("max-price", base * 3.0) : base * 3.0;
                    int unitSize = entry != null ? entry.getInt("unit-size", 1) : 1;
                    ShopItemEntry shopItem = new ShopItemEntry(mat, base, min, max, unitSize);
                    items.add(shopItem);
                    itemMap.put(mat.name(), shopItem);
                }
            }
            loaded.add(new ShopCategory(id, icon, name, items));
            fileMap.put(id, file);
        }

        categories = Collections.unmodifiableList(loaded);
        itemsByMaterial = Collections.unmodifiableMap(itemMap);
        categoryFiles = Collections.unmodifiableMap(fileMap);
    }
}
