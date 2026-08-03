# BellTrade

Plugin ekonomii serwerowej dla **Paper/Purpur 1.21.x** (Java 21).

Rynek gracz ↔ gracz, bezpieczny trade, inteligentny skup serwera i własna waluta — **bez Vault i EssentialsX**.

> Wersja: **1.26.1.2** | Autor: Bellzeb

## Status

**Gotowy do produkcji** (Free) — pełny cykl Sesji 1–6.

## Funkcje

| Moduł | Opis |
|-------|------|
| **Market** | GUI rynku, wystawianie z GUI lub `/market`, tax, limity |
| **Trade** | GUI wymiany, potwierdzenie obu stron, timeout |
| **Sell Shop** | Skup z dynamicznymi cenami (podaż/popyt/inflacja) |
| **Economy** | `/balance`, `/pay`, `/baltop`, SQLite |
| **Admin** | Panel 54-slot, health monitor, edytor cen |
| **Main menu** | `/btrade` — hub gracza |

## Szybki start

```powershell
cd BellTrade
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"   # lub 26
.\mvnw.cmd clean package
```

JAR: `target/BellTrade-1.26.1.2.jar` → `plugins/`

Po starcie: `/btrade` (menu), `/btrade admin` (panel).

## Komendy

| Komenda | Alias | Opis |
|---------|-------|------|
| `/btrade` | `belltrade` | Menu główne |
| `/btrade admin` | — | Panel admina |
| `/market` | `ah` | Rynek |
| `/sellshop` | `skup` | Sklep skupu |
| `/trade <gracz>` | — | Handel |
| `/balance` | `bal`, `money` | Saldo |
| `/pay` | — | Przelew |
| `/baltop` | `top` | Ranking |

## Konfiguracja sklepu

| Operacja | Free | Pro (plan) |
|----------|------|------------|
| Zmiana cen | Admin GUI lub YAML | + masowy edytor |
| Nowe kategorie/przedmioty | YAML | GUI (`ShopCatalogEditorGUI`) |

Szczegóły: [`docs/shop-config.md`](docs/shop-config.md)

## Integracje (opcjonalne)

| Plugin | Funkcja |
|--------|---------|
| **PlaceholderAPI** | `%belltrade_balance%`, `%belltrade_listings%`, … — [`docs/placeholders.md`](docs/placeholders.md) |
| **BellLands** | `integrations.belllands.trade-require-trust: true` — blokada trade na cudzej działce bez trust |
| **LuckPerms** | Limity ofert (`belltrade.market.limit.10`) |

```yaml
integrations:
  placeholderapi: true
  belllands:
    trade-require-trust: false   # true = wymaga trust na claimie
```

## Wymagania

- Paper / Purpur 1.21.x, Java 21+
- **Nie instaluj:** EssentialsX, Vault

## Stack serwera Bell

| Funkcja | Plugin |
|---------|--------|
| Ekonomia | **BellTrade** |
| Claimy, TPA, warpy | **BellLands** |
| Chat | **BellChat** |
| Sklep premium (BellCoins) | **BellMarket** |

## API

```java
BellTradeAPI.get().getEconomy().deposit(uuid, amount, "reason");
BellTradeAPI.get().getPriceEngine().getCurrentPrice(ItemKey.of(material));
BellTradeAPI.get().sellToShop(player, itemStack);
```

Eventy: `MarketListingEvent`, `MarketPurchaseEvent`, `ShopSellEvent`, `BalanceChangeEvent` — [`docs/events.md`](docs/events.md)

## Repozytoria i dystrybucja

| Repo | GitHub | Rola |
|------|--------|------|
| **BellTrade** (ten katalog) | **Public** | Free — pełny plugin, publiczne API |
| **BellTrade-Pro** | **Private** | Addon — `depend: [BellTrade]`, funkcje premium |

Zasady ekosystemu: [`../Bell-Ecosystem/shared/free-pro-addon.md`](../Bell-Ecosystem/shared/free-pro-addon.md)

- **Free:** BuiltByBit / SpigotMC — tekst listingu: [`PLUGIN_PAGE.md`](PLUGIN_PAGE.md)
- **Pro:** osobne prywatne repo — [`../BellTrade-Pro/README.md`](../BellTrade-Pro/README.md)
- **CI:** push do `main` → GitHub Actions buduje JAR (artifact)

Na serwerze z Pro: oba pliki w `plugins/` — najpierw Free, potem Pro.

## Dokumentacja

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/roadmap.md`](docs/roadmap.md)
- [`docs/commands.md`](docs/commands.md)
- [`docs/events.md`](docs/events.md)
- [`docs/placeholders.md`](docs/placeholders.md)
- [`../Bell-Ecosystem/belltrade/`](../Bell-Ecosystem/belltrade/)
