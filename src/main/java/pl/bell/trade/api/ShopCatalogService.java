package pl.bell.trade.api;

import org.bukkit.Material;
import pl.bell.trade.model.ShopCategory;

import java.util.List;

/**
 * Sell-shop catalog mutations. Free admins use YAML; Pro uses GUI on top of the same files.
 */
public interface ShopCatalogService {

    List<ShopCategory> getCategories();

    boolean addCategory(String id, Material icon, String displayName);

    boolean removeCategory(String id);

    boolean addItem(String categoryId, Material material, double basePrice, double minPrice, double maxPrice, int unitSize);

    boolean removeItem(String categoryId, Material material);

    boolean updateItemPrice(String categoryId, Material material, double basePrice, double minPrice, double maxPrice);
}
