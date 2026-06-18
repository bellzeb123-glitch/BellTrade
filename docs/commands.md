# BellTrade — Komendy i uprawnienia

## Komendy

### `/btrade` (alias główny; `/belltrade` też działa)

| Subkomenda | Uprawnienie | Opis |
|------------|-------------|------|
| *(brak)* | `belltrade.use` | Otwiera MainMenuGUI |
| `language <en\|pl>` | `belltrade.admin` | Zmiana języka serwera |
| `lang <en\|pl>` | `belltrade.admin` | Alias language |
| `admin` | `belltrade.admin` | Otwiera AdminGUI |
| `reload` | `belltrade.admin` | Przeładowanie config + lang + shop |

### `/market` (alias: `ah`)

| Subkomenda | Uprawnienie | Opis |
|------------|-------------|------|
| *(brak)* | `belltrade.market` | GUI rynku |
| `sell <cena>` | `belltrade.market.sell` | Wystaw przedmiot w ręce |
| `my` | `belltrade.market` | Twoje aktywne oferty |
| `search <nazwa>` | `belltrade.market` | Filtr (Pro: zaawansowany) |
| `cancel <id>` | `belltrade.market.sell` | Anuluj ofertę |

### `/trade`

| Użycie | Uprawnienie | Opis |
|--------|-------------|------|
| `/trade <gracz>` | `belltrade.trade` | Wyślij zaproszenie |
| `/trade accept` | `belltrade.trade` | Akceptuj zaproszenie |
| `/trade deny` | `belltrade.trade` | Odrzuć zaproszenie |

### `/balance` (alias: `bal`, `money`)

| Subkomenda | Uprawnienie | Opis |
|------------|-------------|------|
| *(brak)* | `belltrade.balance` | Własne saldo |
| `<gracz>` | `belltrade.balance.others` | Saldo innego gracza |

### `/pay`

| Użycie | Uprawnienie | Opis |
|--------|-------------|------|
| `/pay <gracz> <kwota>` | `belltrade.pay` | Przelew środków |

### `/baltop` (alias: `top`)

| Użycie | Uprawnienie | Opis |
|--------|-------------|------|
| `/baltop [strona]` | `belltrade.baltop` | Ranking najbogatszych |

> Komendy rejestrowane gdy `commands.register-economy: true` w configu.
> **Essentials i Vault nie instalujemy.** Warp/TPA → BellLands. Ekonomia → BellTrade.

---

## Uprawnienia

```yaml
belltrade.*:
  default: op
  children:
    belltrade.use: true
    belltrade.market: true
    belltrade.market.sell: true
    belltrade.trade: true
    belltrade.balance: true
    belltrade.pay: true
    belltrade.admin: true

belltrade.use:
  default: true
belltrade.market:
  default: true
belltrade.market.sell:
  default: true
belltrade.trade:
  default: true
belltrade.balance:
  default: true
belltrade.pay:
  default: true
belltrade.baltop:
  default: true
belltrade.balance.others:
  default: op
belltrade.admin:
  default: op

# Limity (LuckPerms meta lub config)
belltrade.market.limit.10:   # Pro — 10 ofert (przykład)
belltrade.market.limit.50:   # Pro — 50 ofert
belltrade.tax.exempt:        # Pro — brak prowizji market

# Sklep skupu — katalog (BellTrade Pro)
belltrade.pro.shop.catalog:          # Otwarcie ShopCatalogEditorGUI
belltrade.pro.shop.catalog.create:   # Nowe kategorie i przedmioty
belltrade.pro.shop.catalog.delete:   # Usuwanie kategorii i przedmiotów
```

> Free: struktura katalogu (`shop/categories/*.yml`) — tylko ręczna edycja + `/btrade reload`.
> Patrz [`shop-config.md`](shop-config.md).

---

## Konflikty aliasów w ekosystemie

| Alias | Plugin | Uwaga |
|-------|--------|-------|
| `/bc` | BellMarket (BellCoins) | **NIE używać** w BellTrade |
| `/bm`, `/shop` | BellMarket | Osobny sklep premium |
| `/bt` | BellTrade (stary) | Użyj `/btrade` |
| `/ah`, `/market` | BellTrade | Standard branżowy |
