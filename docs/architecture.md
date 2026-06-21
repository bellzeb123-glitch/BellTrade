# BellTrade — Architektura

## Wersja

`1.26.1.2` — Purpur/Paper 1.21.x, Java 21, Maven.

## Cel produktu

BellTrade to **ekonomia serwerowa** — obieg waluty między graczami. Uzupełnia
BellMarket (sklep premium / monetyzacja), nie zastępuje go.

| Moduł | Cel |
|-------|-----|
| **Market** | Rynek gracz ↔ gracz — wystawianie przedmiotów za cenę |
| **Trade** | Bezpieczny handel bezpośredni (przedmioty + waluta) |
| **Sell Shop** | Serwer skupuje przedmioty od graczy — źródło pieniędzy w obiegu |

Jedna konfigurowalna waluta (`currency.display-name`, np. „Coins”, „Złoto”).
**Własny system ekonomii** wbudowany w plugin — **bez Vault, bez EssentialsX**
i bez innych zewnętrznych pluginów economy (CMI itd.).

> **Deploy — zero Essentials, zero Vault:** Na serwerze Bell nie ma EssentialsX
> ani Vault. Podział odpowiedzialności w ekosystemie:
>
> | Obszar | Plugin Bell |
> |--------|-------------|
> | Ekonomia (`/balance`, `/pay`, rynek, trade) | **BellTrade** |
> | Warpy, TPA, claimy | **BellLands** |
> | Chat | **BellChat** |
> | Auth / whitelist | **BellGate** |
>
> Integracja między pluginami: publiczne API + eventy (`BellTradeAPI`), nie Vault.

> **Decyzja projektowa:** BellTrade jest **jedynym źródłem prawdy** o saldach
> serwerowej waluty. Wzorzec implementacji: `CurrencyManager` z BellMarket
> (YAML/SQLite), rozszerzony o API publiczne dla innych pluginów Bell.

---

## Pozycja w ekosystemie

```
┌─────────────────┐     BellCoins (monetyzacja)     ┌──────────────┐
│   BellMarket    │ ◄── sklep premium, skiny, VIP   │   Gracz      │
└─────────────────┘                                 └──────┬───────┘
                                                           │
              BellTrade — ekonomia, rynek, trade            │
┌─────────────────┐     salda w data.db                   │
│   BellTrade     │ ◄─────────────────────────────────────┤
└─────────────────┘                                       │
┌─────────────────┐     warpy, TPA, claimy                │
│   BellLands     │ ◄─────────────────────────────────────┘
└─────────────────┘

Essentials / Vault: NIE INSTALOWAĆ
```

**BellLands** przejmuje funkcje teleportacji z Essentials (`/warp`, `/tpa`, warpy
na działce). **BellTrade** przejmuje całą ekonomię. Żaden plugin Bell nie łączy
się z Vault API.

**Konflikt z BellMarket Pro:** planowany „dom aukcyjny” w BellMarket Pro
przenosimy tutaj. BellMarket Pro pozostaje przy statystykach sklepu premium.

---

## Własna ekonomia — szczegóły

### Dlaczego własna ekonomia (bez Vault / Essentials)?

| Powód | Opis |
|-------|------|
| Niezależność | Pełna kontrola — brak Essentials i Vault na serwerze |
| Kontrola | Format sald, transakcje, eventy pod ekosystem Bell |
| Ekosystem | Pluginy łączą się przez `BellTradeAPI`, nie Vault |
| Prostota deploy | Jeden stack Bell — BellLands (TP/warp) + BellTrade (eco) |
| Health monitor | Inflacja liczona z własnych sald w SQLite |

### CurrencyManager (rdzeń)

Wzorzec z BellMarket `CurrencyManager`, dostosowany do BellTrade:

```java
// Operacje atomowe — używane przez market, trade, shop, admin
long getBalance(UUID uuid);
boolean hasEnough(UUID uuid, double amount);
boolean withdraw(UUID uuid, double amount);  // false gdy za mało
boolean deposit(UUID uuid, double amount);
void setBalance(UUID uuid, double amount);
String format(double amount);              // np. "1 250 Coins"
```

- Salda w tabeli SQLite `balances` (uuid, balance, updated_at) — ten sam `data.db` co listingi
- `starting-balance` z configu dla nowych graczy
- Zapis async (single-thread writer, jak BellLands)
- Event `BalanceChangeEvent` — inne pluginy Bell mogą nasłuchiwać

### Komendy ekonomii (natywne)

| Komenda | Alias | Opis |
|---------|-------|------|
| `/balance` | `bal`, `money` | Własne saldo lub `[gracz]` |
| `/pay <gracz> <kwota>` | — | Przelew |
| `/baltop` | `top` | Ranking najbogatszych |

Konfiguracja `commands.register-economy: true` — gdy `false`, tylko API/GUI
(przydatne przy testach lub gdy admin mapuje komendy ręcznie).

### API dla innych pluginów (zamiast Vault)

```java
// BellTradeAPI — publiczny singleton (jak BellMarketAPI)
BellTradeAPI.get().getEconomy().deposit(uuid, amount, "shop-sell");
BellTradeAPI.get().getEconomy().withdraw(uuid, amount, "market-buy");
BellTradeAPI.get().getEconomy().getBalance(uuid);
```

Pluginy Bell (np. przyszły BellCenter) integrują się przez API + eventy,
**nie** rejestrują się w Vault.

---

```text
pl.bell.trade/
├── BellTrade.java                    Główna klasa, reload, baner
├── api/
│   ├── BellTradeAPI.java             Singleton publiczny
│   ├── EconomyService.java           Fasada publiczna (implementuje EconomyProvider)
│   └── TransactionGuard.java         Atomowe transakcje (market/trade/shop)
├── command/
│   ├── BellTradeCommand.java         /belltrade (lang, reload, admin)
│   ├── MarketCommand.java            /market, /ah
│   ├── TradeCommand.java             /trade
│   └── EconomyCommand.java           /balance, /pay, /baltop
├── config/
│   ├── LangManager.java              lang/en.yml + pl.yml — MERGE (wzorzec BellLands)
│   └── ShopConfigManager.java        shop/*.yml — ceny bazowe, kategorie
├── economy/
│   ├── EconomyProvider.java          Interfejs publiczny API
│   ├── CurrencyManager.java          Salda w SQLite — jedyny backend
│   └── BalanceRepository.java        Odczyt/zapis tabeli balances
├── engine/                           ← rdzeń „inteligentnego” sklepu
│   ├── SupplyTracker.java            Ilość przedmiotów w ekosystemie serwera
│   ├── DemandTracker.java            Transakcje, listingi, velocity
│   ├── RarityScorer.java             Wynik rzadkości 0.0–1.0 per Material/NBT
│   ├── PriceEngine.java              Obliczanie ceny dynamicznej
│   └── EconomyHealthMonitor.java     Inflacja/deflacja, reakcja na zarobki
├── event/
│   ├── MarketListingEvent.java
│   ├── MarketPurchaseEvent.java
│   ├── TradeCompleteEvent.java
│   ├── ShopSellEvent.java
│   ├── BalanceChangeEvent.java
│   └── BellTradeReloadEvent.java
├── gui/
│   ├── GuiHolder.java                Tracking po UUID (nie po tytule)
│   ├── GuiListener.java
│   ├── MainMenuGUI.java              Hub: Market / Trade / Sell Shop / Saldo
│   ├── MarketGUI.java                Przeglądanie, filtry, własne oferty
│   ├── TradeGUI.java                 Okno wymiany 2 graczy
│   ├── SellShopGUI.java              Skup przedmiotów (kategorie)
│   └── AdminGUI.java                 Ceny, statystyki, język, reload
├── listener/
│   ├── PlayerJoinListener.java       Inicjalizacja cache supply
│   └── InventorySnapshotListener.java  Okresowy sampling ekwipunków (async)
├── market/
│   ├── ListingManager.java           CRUD ofert
│   └── Listing.java                  Model immutable
├── model/
│   ├── ItemKey.java                  Material + opcjonalny hash NBT (Pro: custom items)
│   ├── PriceSnapshot.java            Historia ceny
│   └── TradeSession.java             Aktywna sesja wymiany
├── storage/
│   └── Database.java                 SQLite — listingi, historia, metryki
└── trade/
    ├── TradeManager.java             Zaproszenia, timeout, potwierdzenie
    └── TradeValidator.java           Anti-scam, limity, blacklist
```

### Pro (`pl.bell.trade.pro.*`) — osobne repo `BellTrade-Pro` (PRIVATE)

> Szkielet repo: [`../../BellTrade-Pro/`](../../BellTrade-Pro/) | Zasady: [`../../Bell-Ecosystem/shared/free-pro-addon.md`](../../Bell-Ecosystem/shared/free-pro-addon.md)

Pro importuje **TYLKO**: `api/`, `model/`, `event/`, `economy.EconomyProvider`.

```text
pl.bell.trade.pro/
├── BellTradePro.java                 // FUTURE: license check
├── engine/
│   ├── AdaptivePriceEngine.java      Pełny silnik ML/heurystyczny (rozszerza PriceEngine)
│   └── AnomalyDetector.java          Wykrywanie farm / duplikacji
├── gui/
│   ├── AnalyticsGUI.java             Wykresy, top itemy, inflacja
│   ├── BulkPriceEditorGUI.java       Masowa edycja cen
│   └── ShopCatalogEditorGUI.java     Dodawanie/usuwanie kategorii i przedmiotów skupu
├── storage/
│   └── MySqlMetricsStore.java        Replikacja metryk multi-serwer
└── integration/
    └── DiscordWebhook.java           Powiadomienia o dużych transakcjach
```

---

## Moduł 1 — Market (rynek graczy)

### Flow wystawienia

```
Gracz trzyma item → /market sell <cena>
  → TradeValidator (blacklist, max cena, max stack)
  → Item z ekwipunku → Listing w DB
  → MarketListingEvent
  → GUI odświeżone
```

### Flow zakupu

```
Kupujący klika ofertę w GUI
  → TransactionGuard.begin()
  → EconomyBridge.remove(buyer, price + tax)
  → EconomyBridge.add(seller, price - tax)
  → Item → ekwipunek kupującego (lub depozyt przy pełnym inv)
  → Listing usunięty
  → MarketPurchaseEvent
  → DemandTracker.record()
```

### Limity Free vs Pro

| Parametr | Free | Pro |
|----------|------|-----|
| Aktywne oferty / gracz | 5 | 50 (lub unlimited z perm) |
| Czas wygaśnięcia | 48 h | 7 dni + auto-renew |
| Tax (prowizja) | stały % z config | per-kategoria, per-ranga LP |
| Wyszukiwanie | po materiale | + lore, enchant, custom NBT |
| Historia transakcji | 7 dni | 90 dni + export |

---

## Moduł 2 — Trade (handel bezpośredni)

### Flow

```
/trade <gracz>  →  zaproszenie (30 s timeout)
Akceptacja      →  TradeGUI (2×4 sloty + slot waluty)
Oba klikają ✓   →  TransactionGuard — atomowa wymiana
Anulowanie      →  zwrot itemów
```

### Zabezpieczenia

- Odległość max (config, domyślnie 10 bloków)
- Oba gracze muszą potwierdzić po każdej zmianie slotu
- Blokada podczas walki / w vanish (opcjonalnie)
- Softdepend BellLands: opcjonalna blokada trade poza trustem na claimie

---

## Moduł 3 — Sell Shop (inteligentny skup)

### Koncepcja

Serwer **kupuje** przedmioty od graczy — wprowadza walutę do ekonomii.
Ceny nie są statyczne: reagują na podaż, popyt i zdrowie ekonomii.

### PriceEngine — formuła bazowa (Free: uproszczona)

```
finalPrice = basePrice
           × supplyFactor      (więcej itemów na serwerze → niższa cena)
           × demandFactor      (więcej sprzedaży/listingów → wyższa cena)
           × rarityScore       (0.1–1.0)
           × healthMultiplier  (inflacja → obniżka skupu)
```

Clamped: `minPrice ≤ finalPrice ≤ maxPrice` (z config per item).

### SupplyTracker — skąd wie co jest „powszechne”?

| Źródło danych | Waga | Opis |
|---------------|------|------|
| Snapshot ekwipunków | 40% | Co 15 min async — sample N graczy online + offline heads |
| Shulker / ender chest | 20% | Opcjonalny głębszy scan (Pro) |
| Market listings | 15% | Nadmiar ofert tego samego itemu |
| Sell shop transakcje | 15% | Velocity sprzedaży do serwera |
| Suma sald (CurrencyManager) | 10% | Proxy inflacji — dużo kasy w obiegu → obniżka skupu |

Przykład: gracz ma farmę żelaza → `IRON_INGOT` supplyFactor → 0.3 → skup 3 monety
zamiast 10. Netherite bez farm → supplyFactor 0.9, rarity 0.95 → skup wysoki.

### RarityScorer — heurystyki (bez zewnętrznego AI w Free)

```text
rarity = weighted(
  vanillaTier,           // netherite > diamond > iron
  enchantPenalty,        // enchanted = trudniej zfarmić masowo
  nameCustom,            // custom display name → wyższa rzadkość
  nbtSignature,          // unikalny NBT (MM, IA) → Pro provider
  supplyPercentile       // percentyl w SupplyTracker
)
```

**Pro — AdaptivePriceEngine:** uczenie z historii 30 dni, wykrywanie anomalii
(nagły wzrost podaży o 500% w 1 h = prawdopodobna kopiaarka), sezonowe korekty.

### EconomyHealthMonitor

Co 1 h (async):

- `totalMoney` = suma sald z tabeli `balances` (lub próbkowanie + cache przy dużych bazach)
- `moneyVelocity` = transakcje / h
- `inflationIndex` = totalMoney / baseline (zapisany przy pierwszym uruchomieniu)

Reakcje:

| Stan | Akcja |
|------|-------|
| Inflacja > 150% | Obniż skup o 10%, podnieś tax market o 1% |
| Deflacja < 70% | Podnieś skup o 10%, event bonus sell |
| Stagnacja | Brak zmian |

Wszystko widoczne w **AdminGUI → Economy Health** (Free: wskaźniki, Pro: wykresy).

---

## Integracje zewnętrzne

| Plugin | Typ | Zastosowanie |
|--------|-----|--------------|
| **LuckPerms** | opcjonalny | Limity ofert, tax exempt, admin perms |
| **BellLands** | zalecany | Warpy, TPA, claimy — zastępuje Essentials `/warp`, `/tpa` |
| **PlaceholderAPI** | softdepend | `%belltrade_balance%`, `%belltrade_listings%` |
| ItemsAdder / Nexo / MM | Pro providers | ItemKey z custom ID, rarity boost |

> **Brak Vault i Essentials** — serwer Bell = wyłącznie pluginy ekosystemu + opcjonalnie LP/PAPI.

Nazwa waluty: `economy.currency-name` + `economy.currency-symbol` — używane
w GUI i wiadomościach (`{currency}`, `{symbol}`).

**Zmiana z GUI (wymagane):** Admin panel — sloty nazwa (20) i symbol (22) waluty.
Kliknięcie → wpisanie na czacie → `CurrencyConfig.setCurrencyName()` /
`setCurrencySymbol()` zapisuje do `config.yml` i odświeża `LangManager`.
Wszystkie GUI i komunikaty natychmiast używają nowej nazwy.

---

## Persystencja

### SQLite (`data.db`) — Free

| Tabela | Zawartość |
|--------|-----------|
| `balances` | uuid, balance, updated_at |
| `listings` | id, seller_uuid, item_blob, price, created, expires |
| `trade_log` | timestamp, type, buyer, seller, item_key, amount, price |
| `supply_samples` | item_key, quantity, sampled_at |
| `price_history` | item_key, price, engine_version, recorded_at |

Zapis async, single-thread writer (wzorzec BellLands `Database.java`).

### Pliki YAML (nie nadpisywać na serwerze)

| Plik | Merge? | Opis |
|------|--------|------|
| `config.yml` | NIE | Główna konfiguracja |
| `lang/en.yml`, `lang/pl.yml` | TAK (MERGE) | Teksty |
| `shop/categories/*.yml` | NIE | Kategorie skupu + basePrice — **edycja struktury: Pro**; Free: ręczny YAML |
| `shop/blacklist.yml` | NIE | Zablokowane itemy |

Szczegóły konfiguracji Free: [`shop-config.md`](shop-config.md).

---

## GUI — zasady

1. **Tracking po UUID** (`GuiHolder`), nie po tytule inventory — stabilne przy zmianie języka.
2. **Wszystkie teksty** z `LangManager.get()` / `getRaw()` — zero hardcode.
3. **AdminGUI** (54 sloty) — centralny panel:

```text
Slot 4:   Saldo ekonomii (health indicator)
Slot 10:  Market stats (oferty, transakcje/dzień)
Slot 12:  Sell shop — edytor cen (Free: istniejące pozycje)
          Pro: + ShopCatalogEditorGUI (kategorie / przedmioty)
Slot 14:  Trade log (ostatnie 28)
Slot 16:  Economy health (inflacja, velocity)
Slot 28:  Język EN/PL
Slot 30:  Reload
Slot 32:  Top sprzedawcy (Pro: pełne analytics)
```

4. **MainMenuGUI** — punkt wejścia `/belltrade` lub `/bt`:

```text
Slot 11: Market (rynek)
Slot 13: Trade (zaproś gracza)
Slot 15: Sell Shop (sprzedaj serwerowi)
Slot 22: Twoje saldo + /pay hint
```

---

## System językowy

Identyczny wzorzec jak **BellLands** (nie `messages_*.yml`):

- Pliki: `lang/en.yml`, `lang/pl.yml`
- `LangManager` z MERGE (baza JAR + customizacje z dysku)
- Komenda: `/belltrade language <en|pl>` (alias: `lang`)
- `reload()` przeładowuje config + lang + shop + engine
- Domyślny język: `en` (`config.yml → language: en`)

---

## Baner startowy (OBOWIĄZKOWY)

Wzorzec referencyjny ekosystemu. Pełna specyfikacja: [`Bell-Ecosystem/shared/banner-standard.md`](../../Bell-Ecosystem/shared/banner-standard.md).

| Wersja | Wzorzec | Suffix | Status line |
|--------|---------|--------|-------------|
| Free | A | `§f Trade` | `§aFree §7│` + `§5Pro §aActive` lub `§7Pro §5Coming Soon` |
| Pro | B | `§f Trade §5Pro` | `§5Pro §7│ §7Adaptive Economy` |

`printBanner()` musi być **pierwszym** wywołaniem w `onEnable()`.

---

## Publiczne API (Free)

```java
BellTradeAPI.get().getEconomy().getBalance(player);
BellTradeAPI.get().getMarket().getActiveListings(player);
BellTradeAPI.get().getPriceEngine().getCurrentPrice(ItemKey.of(material));
BellTradeAPI.get().sellToShop(player, itemStack); // programowe skupienie
```

Eventy do nasłuchiwania przez inne pluginy Bell (np. BellCenter w przyszłości).

---

## Free vs Pro — rekomendacja

### Werdykt: **podział Free + Pro** (jak reszta ekosystemu)

| Kryterium | Uzasadnienie |
|-----------|--------------|
| Konkurencja | Darmowe pluginy (AuctionHouse, ShopGUI+) mają rynek — Free musi być kompletny w podstawach |
| Unikalność | „Inteligentny” sklep to główny USP — naturalne miejsce na Pro |
| Monetyzacja | Pro $15–20 USD — analytics + adaptive engine + MySQL |
| Ekosystem | Spójność z BellMarket/BellChat/BellLands |
| Utrzymanie | Free repo publiczny, Pro prywatny — mniejszy crack surface na silnik cen |

### Free — co MUSI działać samodzielnie

- Market P2P (z limitami)
- Trade GUI z zabezpieczeniami
- Sell shop z **prostym** PriceEngine (supply/demand/rarity heurystyki)
- Własna ekonomia (`CurrencyManager` + `/balance`, `/pay`, `/baltop`)
- AdminGUI (edycja cen istniejących pozycji, podstawowe statystyki, health indicator)
- Katalog sklepu skupu — **tylko YAML** (dodawanie/usuwanie kategorii i przedmiotów → Pro)
- EN/PL, baner, SQLite

### Pro — co uzasadnia cenę

- **AdaptivePriceEngine** + AnomalyDetector (kopiaarki, farmy)
- Pełne **AnalyticsGUI** (wykresy 30 dni, export CSV)
- **BulkPriceEditor** — masowa zmiana cen
- **ShopCatalogEditorGUI** — dodawanie/usuwanie kategorii i przedmiotów z Admin GUI (bez YAML)
- Per-world markets
- **MySQL** replikacja metryk (multi-server)
- Discord webhooks
- Custom item providers (IA, Nexo, MythicMobs)
- Wyższe limity, brak tax dla VIP, auto-renew listingów

### Czego NIE dawać w Pro

- Podstawowy market/trade (muszą być w Free — inaczej nikt nie zainstaluje)
- Własna ekonomia i komendy balance/pay (Free)
- Podstawowy sell shop i dynamiczne ceny (Free — bez tego plugin nie ma sensu)

### Czego NIE dawać w Free (zostaje w Pro)

- Dodawanie / usuwanie **kategorii** sklepu skupu z GUI
- Dodawanie / usuwanie **przedmiotów** w katalogu z GUI
- Masowa edycja cen (`BulkPriceEditorGUI`)
- Adaptive pricing i wykrywanie anomalii

---

## Komendy — podgląd

| Komenda | Alias | Opis |
|---------|-------|------|
| `/btrade` | `btrade` | Menu główne (`/belltrade` — pełna nazwa) |
| `/btrade lang <en\|pl>` | — | Zmiana języka |
| `/btrade admin` | — | AdminGUI |
| `/btrade reload` | — | Przeładowanie |
| `/market` | `ah` | GUI rynku |
| `/market sell <cena>` | — | Wystaw przedmiot w ręce |
| `/market my` | — | Twoje oferty |
| `/trade <gracz>` | — | Zaproszenie do wymiany |
| `/balance` | `bal`, `money` | Saldo (natywne) |
| `/pay <gracz> <kwota>` | — | Przelew |
| `/baltop` | `top` | Ranking |

> `/bc` zajęte przez BellCoins — główny skrót: `/btrade`.

---

## Zależności Maven (Free)

```xml
<!-- paper-api 1.21.4+, Java 21 -->
<!-- sqlite-jdbc via plugin.yml libraries -->
<!-- Brak VaultAPI — własna ekonomia -->
```

---

## Powiązana dokumentacja

- [`roadmap.md`](roadmap.md) — plan sesji implementacji
- [`commands.md`](commands.md) — pełna lista uprawnień
- [`events.md`](events.md) — eventy publiczne
- [`../../Bell-Ecosystem/belltrade/`](../Bell-Ecosystem/belltrade/) — promo i ecosystem
