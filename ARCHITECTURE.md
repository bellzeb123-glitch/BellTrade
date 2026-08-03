# BellTrade — Architecture

Economy and trading plugin for the Bell ecosystem.
Provides a built-in currency, player market (auction house), P2P item+money trades,
a dynamic sell shop with supply/demand pricing, and an economy health monitor — all
without external economy plugin dependencies.

---

## Currency system

| Component | Role |
|---|---|
| `CurrencyConfig` | Runtime display settings (name, symbol, format, starting balance). Editable from Admin GUI, persisted to `config.yml`. |
| `CurrencyManager` | In-memory balance cache backed by SQLite (`BalanceRepository`). Implements `EconomyProvider` interface for deposit/withdraw/setBalance. Fires `BalanceChangeEvent` on every mutation. |
| `EconomyCommand` | `/balance`, `/pay`, `/baltop` — registered only when `commands.register-economy` is true. |

Balances are loaded into a `Map<UUID, Double>` on startup and flushed to the database
on shutdown or import. Async writes happen on every individual balance change via
`BalanceRepository.saveAsync()`.

### Balance import

BellTrade can import player balances from external sources:

- **EssentialsX** — scans `userdata/` YAML files for `money:` entries.
- **SQLite economy.db** — reads rows from an existing SQLite economy database.

Import modes: `REPLACE`, `ADD`, `MAX`. A `dry-run` preview is available.

---

## Market / auction house

| Component | Role |
|---|---|
| `ListingManager` | Core market logic: create, cancel, purchase listings. Handles tax calculation, blacklist, expiry, and listing limits (permission-based). |
| `ListingRepository` | SQLite persistence for active listings. |
| `ExpiredMailboxManager` / `ExpiredMailboxRepository` | Expired or cancelled listings are stored in a mailbox for the seller to claim later. |
| `MarketSellFlow` | Guides players through the sell-from-hand flow. |
| `MarketGUI` / `MarketGuiListener` | Browsable chest GUI with pagination, material filter, and search. |
| `MarketChatListener` | Captures chat input for price entry and search queries during market flows. |
| `TransactionGuard` | Per-listing lock to prevent double-purchase race conditions. |

### Listing limits

Limits are resolved at runtime from permissions, highest wins:

- `belltradepro.market.limit.unlimited` → unlimited
- `belltrade.market.limit.50` / `belltradepro.market.limit.50` → 50
- `belltrade.market.limit.10` / `belltradepro.market.limit.10` → 10
- Default from `market.max-listings-per-player` config (default 5)

### Tax

Base tax is configurable (`market.tax-percent`, default 5%). The `EconomyHealthMonitor`
may add a bonus during inflation. Tax-exempt permission: `belltradepro.tax.exempt`.

---

## Sell shop

| Component | Role |
|---|---|
| `ShopConfigManager` | Loads per-material entries from YAML (base price, min/max, unit size, categories). |
| `ShopManager` | Sells plain (no custom meta) items from inventory. Removes items, pays player, records sale. |
| `SellShopGUI` / `SellShopGuiListener` | Category-based chest GUI for browsing sellable items. |

### Dynamic pricing — PriceEngine

`PriceEngine` (free tier, engine version `free-1`) computes the current sell price as:

```
price = basePrice × supplyFactor × demandFactor × rarityScore × healthMultiplier
```

Clamped to `[minPrice, maxPrice]` from the shop config entry.

| Factor | Source |
|---|---|
| `supplyFactor` | `SupplyTracker` — periodic inventory sampling across online players. |
| `demandFactor` | `DemandTracker` — ratio of recent shop sells to market listings for the item. |
| `rarityScore` | `RarityScorer` — static rarity tier from material type. |
| `healthMultiplier` | `EconomyHealthMonitor` — adjusted during inflation/deflation. |

Sales are recorded in `PriceHistoryRepository` for trend analysis.

### Shop price editor

`ShopPriceEditorGUI` / `ShopPriceEditFlow` allows admins to adjust base prices via an
in-game GUI with anvil-based number input.

---

## P2P trade

| Component | Role |
|---|---|
| `TradeManager` | Invite → accept/deny → session lifecycle. Manages pending invites with timeout and active `TradeSession` instances. |
| `TradeValidator` | Pre-flight checks (distance, world, permissions, BellLands trust). |
| `TradeGUI` / `TradeGuiListener` | Split chest GUI where each side places items and money. Both players must confirm ("ready") to complete. |
| `TradeSession` | State object holding both players' UUIDs, offered items, money amounts, and ready flags. |

Trade completion atomically withdraws/deposits money and swaps items, then fires
`TradeCompleteEvent`.

---

## Economy health monitor

`EconomyHealthMonitor` tracks macro-economic indicators in real time:

- **Inflation index** — ratio of current total money in circulation to a stored baseline.
- **Money velocity** — number of balance-change events + shop transactions in the last hour.
- **Status** — `STABLE`, `INFLATION`, or `DEFLATION` based on configurable thresholds.

Effects during inflation:
- Shop sell prices decrease (lower `shopHealthMultiplier`).
- Market tax increases (`marketTaxAdjustment`).

Effects during deflation:
- Shop sell prices increase (higher multiplier) to inject money.

`EconomyHealthGUI` presents these metrics to admins in a visual dashboard.

---

## Integrations

### BellLP (group sync)

`BellLPIntegration` registers BellTrade as a `GroupSyncHandler` with BellLP using a
reflection-based `Proxy` (no compile-time dependency). Callbacks:

| Callback | Action |
|---|---|
| `onGroupSynced` | Full config + manager reload (picks up any permission/limit changes). |
| `onAllGroupsSynced` | Same full reload. |
| `refreshPlayer` | No-op — market listing limits are resolved from live permissions on every check. |

### BellLands (trust-gated trading)

`BellLandsHook` soft-hooks into BellLands via reflection. When
`integrations.belllands.trade-require-trust` is enabled, P2P trades on a claimed chunk
are only allowed if the player is the claim owner or trusted.

Used by `TradeValidator` before starting a trade session.

### PlaceholderAPI

`PlaceholderHook` registers a `BellTradeExpansion` providing placeholders for balance,
currency name/symbol, economy health, and more.

---

## Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/belltrade` | `/btrade` | `belltrade.use` | Opens main menu GUI |
| `/belltrade admin` | | `belltrade.admin` | Admin panel |
| `/belltrade reload` | | `belltrade.admin` | Reload all configs |
| `/belltrade language <en\|pl>` | `/btrade lang` | `belltrade.admin` | Switch language |
| `/belltrade import <source>` | | `belltrade.admin` | Import balances |
| `/balance [player]` | `/bal`, `/money` | `belltrade.balance` | Check balance |
| `/pay <player> <amount>` | | `belltrade.pay` | Transfer money |
| `/baltop [page]` | `/top` | `belltrade.baltop` | Richest players |
| `/trade <player\|accept\|deny\|cancel>` | | `belltrade.trade` | P2P trade |
| `/market [sell\|my\|cancel\|search]` | `/ah` | `belltrade.market` | Player market |
| `/sellshop [hand]` | `/skup` | `belltrade.shop` | Sell to server shop |

---

## API

`BellTradeAPI` is the public entry point for other plugins:

```java
BellTradeAPI api = BellTradeAPI.get();
```

Key accessors:

| Method | Returns |
|---|---|
| `getEconomy()` | `EconomyProvider` — deposit, withdraw, balance, format |
| `getCurrencyManager()` | Full `CurrencyManager` with import, top list, circulation stats |
| `getTradeManager()` | Programmatic trade session control |
| `getListingManager()` | Market listing CRUD |
| `getShopManager()` | Sell items to shop programmatically |
| `getPriceEngine()` / `getShopPriceEngine()` | Current and base prices for any item |
| `getShopCatalog()` | Browse shop categories and entries |
| `getShopAnalytics()` | Sales statistics and trends |
| `getMarketExtension()` | Extended market operations |
| `getEconomyHealthMonitor()` | Inflation index, velocity, status |
| `getInflationPercent()` | Convenience — current inflation % |
| `getLangRaw()` / `getLangComponent()` | Localization access |

### Events

| Event | When |
|---|---|
| `BalanceChangeEvent` | Any balance mutation (includes delta and reason) |
| `MarketListingEvent` | New listing created (cancellable) |
| `MarketPurchaseEvent` | Listing purchased (cancellable) |
| `ShopSellEvent` | Item sold to shop (cancellable) |
| `TradeCompleteEvent` | P2P trade finalized |
| `BellTradeReloadEvent` | Config reloaded via command or API |

### Pro bridge

`BellTradeProBridge` detects whether the BellTradePro extension is installed and
delegates to its enhanced `ShopPriceEngine` when available. The free `PriceEngine`
is always the fallback.
