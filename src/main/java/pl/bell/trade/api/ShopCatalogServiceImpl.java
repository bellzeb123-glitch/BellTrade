package pl.bell.trade.api;

import org.bukkit.Material;
import pl.bell.trade.config.ShopConfigManager;
import pl.bell.trade.model.ShopCategory;

import java.util.List;

public final class ShopCatalogServiceImpl implements ShopCatalogService {

    private final ShopConfigManager shopConfig;

    public ShopCatalogServiceImpl(ShopConfigManager shopConfig) {
        this.shopConfig = shopConfig;
    }

    @Override
    public List<ShopCategory> getCategories() {
        return shopConfig.getCategories();
    }

    @Override
    public boolean addCategory(String id, Material icon, String displayName) {
        return shopConfig.addCategory(id, icon, displayName);
    }

    @Override
    public boolean removeCategory(String id) {
        return shopConfig.removeCategory(id);
    }

    @Override
    public boolean addItem(String categoryId, Material material, double basePrice, double minPrice, double maxPrice, int unitSize) {
        return shopConfig.addItem(categoryId, material, basePrice, minPrice, maxPrice, unitSize);
    }

    @Override
    public boolean removeItem(String categoryId, Material material) {
        return shopConfig.removeItem(categoryId, material);
    }

    @Override
    public boolean updateItemPrice(String categoryId, Material material, double basePrice, double minPrice, double maxPrice) {
        return shopConfig.updateItemPrice(categoryId, material, basePrice, minPrice, maxPrice);
    }
}
