# BellTrade — Roadmap

## Sesja 0 — Architektura ✅

- [x] Dokument architektury
- [x] Analiza Free vs Pro
- [x] Własna ekonomia (bez Vault, bez Essentials)
- [x] Wzorzec językowy BellLands

---

## Sesja 1 — Fundament

- [x] Szkielet Maven (`pom.xml`, `plugin.yml` v1.26.1.2)
- [x] `BellTrade.java` + baner
- [x] `LangManager` (MERGE) + `lang/en.yml`, `lang/pl.yml`
- [x] `CurrencyConfig` — nazwa/symbol waluty + edycja z Admin GUI
- [x] `CurrencyManager` + `BalanceRepository` (SQLite, tabela `balances`)
- [x] `EconomyCommand` — `/balance`, `/pay`, `/baltop`
- [x] `BellTradeCommand` — lang, reload, admin
- [x] `AdminGUI` — give/take/set, currency rename, lang, reload
- [x] `BellTradeAPI` + `BalanceChangeEvent`
- [x] `config.yml` — currency-name, language, economy settings

## Sesja 2 — Trade (handel bezpośredni)

- [x] `TradeManager` + `TradeSession`
- [x] `TradeGUI` + `TradeGuiListener` (UUID tracking)
- [x] `/trade <gracz>` + accept/deny/cancel
- [x] `TradeCompleteEvent`
- [x] Disconnect handling (`PlayerQuitListener`)

## Sesja 3 — Market (rynek)

- [x] `Database.java` — tabela `listings`
- [x] `ListingManager` + `Listing` model
- [x] `MarketGUI` — browse, buy, my listings
- [x] `/market sell`, `/market my`, `/market cancel`, `/market search`
- [x] `TransactionGuard` — atomowe transakcje
- [x] Tax + limity Free (5 ofert)
- [x] `MarketListingEvent`, `MarketPurchaseEvent`

## Sesja 4 — Sell Shop (podstawowy)

- [x] `shop/categories/*.yml` — kategorie + basePrice
- [x] `SellShopGUI` — sprzedaż do serwera
- [x] `PriceEngine` (Free) — supply/demand/rarity heurystyki
- [x] `SupplyTracker` — snapshot ekwipunków async
- [x] `DemandTracker` + `price_history` tabela
- [x] `ShopSellEvent`

## Sesja 5 — Admin GUI + Economy Health

- [x] `AdminGUI` — stats, **edytor cen** (istniejące pozycje), health indicator
- [x] `EconomyHealthMonitor` — inflacja/deflacja
- [x] `MainMenuGUI` — hub gracza
- [x] `BellTradeAPI` — `getEconomyHealthMonitor()`, `getPriceEngine()`
- [x] Eventy publiczne (MarketListing, MarketPurchase, BellTradeReload)

> Katalog sklepu (dodawanie/usuwanie kategorii i przedmiotów) → **tylko Pro** — patrz Pro-1.

## Sesja 6 — Polish + dystrybucja Free

- [x] PlaceholderAPI expansion (`%belltrade_*%`)
- [x] BellLands soft-integration (trade gate na claimie)
- [x] README, PLUGIN_PAGE.md
- [x] BuiltByBit listing (tekst w PLUGIN_PAGE.md)
- [x] Aktualizacja Bell-Ecosystem README

---

## BellTrade Pro (osobne repo `BellTrade-Pro`, PRIVATE)

Szkielet: [`../../BellTrade-Pro/`](../../BellTrade-Pro/) — `BellTradePro.java`, CI, `depend: [BellTrade]`.

### Pro-1

- [x] Szkielet repo (`BellTradePro.java`, `pom.xml`, CI, README)
- [ ] `AdaptivePriceEngine` + `AnomalyDetector`
- [ ] `AnalyticsGUI` — wykresy, top itemy
- [ ] `BulkPriceEditorGUI` — masowa edycja cen
- [ ] **`ShopCatalogEditorGUI`** — dodawanie/usuwanie **kategorii** i **przedmiotów** (zapis do `shop/categories/*.yml`)
- [ ] Wyższe limity, auto-renew listingów

### Pro-2

- [ ] MySQL metrics (`HikariCP`, relokowany)
- [ ] Discord webhooks
- [ ] Custom item providers (ItemsAdder, Nexo)
- [ ] Per-world markets
- [ ] BuiltByBit listing ($15–20 USD)

---

## Dystrybucja

| Produkt | Cena | Repo | GitHub |
|---------|------|------|--------|
| BellTrade Standard | DARMOWY | `BellTrade` | **Public** |
| BellTrade Pro | $15–20 USD | `BellTrade-Pro` | **Private** |
