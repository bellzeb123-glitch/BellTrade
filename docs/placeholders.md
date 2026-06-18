# BellTrade — PlaceholderAPI

Wymaga **PlaceholderAPI** na serwerze. Rejestracja automatyczna przy starcie
(gdy `integrations.placeholderapi: true` w `config.yml`).

Identyfikator: `%belltrade_<param>%`

| Placeholder | Opis | Przykład |
|-------------|------|----------|
| `balance` | Sformatowane saldo gracza | `$1 250 Coins` |
| `balance_raw` | Saldo jako liczba | `1250.0` |
| `currency` | Nazwa waluty z configu | `Coins` |
| `currency_symbol` | Symbol waluty | `$` |
| `listings` | Aktywne oferty gracza na rynku | `3` |
| `inflation` | Indeks inflacji (%) | `+12.5` |
| `economy_status` | `STABLE`, `INFLATION`, `DEFLATION` | `STABLE` |

## Przykłady

```
&7Saldo: &f%belltrade_balance%
&7Oferty: &f%belltrade_listings%
&7Ekonomia: &f%belltrade_economy_status% (&f%belltrade_inflation%&7%)
```

## LuckPerms / TAB

Placeholdery działają wszędzie tam, gdzie PAPI jest obsługiwane (TAB, scoreboard, chat format BellChat).
