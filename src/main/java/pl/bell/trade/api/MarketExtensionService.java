package pl.bell.trade.api;

import org.bukkit.entity.Player;
import pl.bell.trade.model.Listing;

import java.util.List;

public interface MarketExtensionService {

    long getListingDurationHours(Player seller);

    boolean extendListingExpiry(long listingId, long additionalHours);

    List<Listing> findExpiringWithinHours(int hours);
}
