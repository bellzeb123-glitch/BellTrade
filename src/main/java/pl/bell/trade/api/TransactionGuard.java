package pl.bell.trade.api;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Prevents double-processing of the same market listing purchase. */
public class TransactionGuard {

    private final Set<Long> lockedListings = ConcurrentHashMap.newKeySet();

    public boolean tryLock(long listingId) {
        return lockedListings.add(listingId);
    }

    public void unlock(long listingId) {
        lockedListings.remove(listingId);
    }
}
