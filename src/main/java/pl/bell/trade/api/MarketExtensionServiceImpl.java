package pl.bell.trade.api;

import org.bukkit.entity.Player;
import pl.bell.trade.BellTrade;
import pl.bell.trade.market.ListingRepository;
import pl.bell.trade.model.Listing;

import java.util.List;

public final class MarketExtensionServiceImpl implements MarketExtensionService {

    private final BellTrade plugin;
    private final ListingRepository listings;

    public MarketExtensionServiceImpl(BellTrade plugin) {
        this.plugin = plugin;
        this.listings = plugin.getListingManager().getRepository();
    }

    @Override
    public long getListingDurationHours(Player seller) {
        if (seller.hasPermission("belltradepro.market.long-listings")) {
            return plugin.getConfig().getLong("pro.market.listing-duration-hours", 168L);
        }
        return plugin.getConfig().getLong("market.listing-duration-hours", 48L);
    }

    @Override
    public boolean extendListingExpiry(long listingId, long additionalHours) {
        return listings.extendExpiry(listingId, additionalHours * 3_600_000L);
    }

    @Override
    public List<Listing> findExpiringWithinHours(int hours) {
        long deadline = System.currentTimeMillis() + hours * 3_600_000L;
        return listings.findExpiringBefore(deadline);
    }
}
