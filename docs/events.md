# BellTrade — Publiczne eventy

Wszystkie eventy w pakiecie `pl.bell.trade.event`. Inne pluginy mogą nasłuchiwać
przez `BellTradeAPI` bez importowania wewnętrznych managerów.

---

## `MarketListingEvent`

Gracz wystawił przedmiot na rynek.

| Pole | Typ | Opis |
|------|-----|------|
| `seller` | `Player` | Sprzedawca |
| `listing` | `Listing` | Utworzona oferta |
| `cancelled` | `boolean` | Ustaw true aby anulować |

---

## `MarketPurchaseEvent`

Gracz kupił ofertę z rynku.

| Pole | Typ | Opis |
|------|-----|------|
| `buyer` | `Player` | Kupujący |
| `seller` | `UUID` | Sprzedawca (może offline) |
| `listing` | `Listing` | Oferta |
| `price` | `double` | Cena brutto |
| `tax` | `double` | Pobrana prowizja |
| `cancelled` | `boolean` | Anuluj transakcję |

---

## `TradeCompleteEvent`

Zakończono bezpośredni handel między graczami.

| Pole | Typ | Opis |
|------|-----|------|
| `playerA` | `Player` | Gracz 1 |
| `playerB` | `Player` | Gracz 2 |
| `session` | `TradeSession` | Pełna sesja (itemy + kwoty) |

---

## `ShopSellEvent`

Gracz sprzedał przedmiot do serwerowego sklepu skupu.

| Pole | Typ | Opis |
|------|-----|------|
| `player` | `Player` | Sprzedawca |
| `itemKey` | `ItemKey` | Klucz przedmiotu |
| `amount` | `int` | Ilość |
| `basePrice` | `double` | Cena bazowa z YAML |
| `finalPrice` | `double` | Cena po PriceEngine |
| `cancelled` | `boolean` | Anuluj sprzedaż |

---

## `BellTradeReloadEvent`

Plugin przeładowany (`/belltrade reload` lub startup).

Brak pól — sygnał do addonów (BellTrade Pro) do odświeżenia cache.

---

## `BalanceChangeEvent`

Zmiana salda gracza (market, trade, shop, pay, admin).

| Pole | Typ | Opis |
|------|-----|------|
| `uuid` | `UUID` | Gracz |
| `oldBalance` | `double` | Saldo przed |
| `newBalance` | `double` | Saldo po |
| `delta` | `double` | Różnica (+/-) |
| `reason` | `String` | Np. `market-buy`, `shop-sell`, `pay`, `admin-give` |

---

## Przykład (BellTrade Pro — Discord webhook)

```java
@EventHandler
public void onShopSell(ShopSellEvent e) {
    if (e.getFinalPrice() < 10_000) return;
    discord.sendLargeTransaction(e.getPlayer(), e.getItemKey(), e.getFinalPrice());
}
```
