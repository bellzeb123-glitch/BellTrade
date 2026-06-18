# BellTrade — BuiltByBit / SpigotMC listing (Free)

> Skopiuj i dostosuj przy publikacji. Wersja: **1.26.1.2**

---

## Tytuł

**BellTrade** — Player Market, Secure Trade & Smart Economy (No Vault)

## Krótki opis (tagline)

Kompletna ekonomia serwerowa: rynek P2P, bezpieczny trade GUI, inteligentny skup — bez Essentials i Vault.

---

## Opis główny

**BellTrade** to plugin ekonomii dla **Paper/Purpur 1.21.x** (Java 21), zaprojektowany dla ekosystemu **Bell** — ale działa samodzielnie na każdym serwerze survival.

### Co dostajesz?

**🏪 Rynek graczy** — wystaw przedmiot przez GUI lub `/market`, inni kupują jednym kliknięciem. Prowizja, limity ofert, wygasanie — wszystko w configu.

**🤝 Bezpieczny trade** — wymiana przedmiotów i pieniędzy w oknie GUI. Obie strony muszą potwierdzić. Ochrona przed scamem (odległość, disconnect, opcjonalnie BellLands trust).

**💰 Inteligentny skup** — serwer skupuje przedmioty i reguluje podaż pieniędzy. Ceny dynamiczne (podaż, popyt, inflacja). Kategorie: rudy, uprawy, mob drops, drewno — edytowalne YAML.

**🪙 Własna waluta** — **bez Vault, bez EssentialsX**. SQLite, `/balance`, `/pay`, `/baltop`, API dla innych pluginów.

**⚙️ Panel admina** — statystyki, zdrowie ekonomii, edytor cen sklepu, give/take/set salda, zmiana nazwy waluty z GUI.

**🌍 EN + PL** — pełne tłumaczenie, `/btrade lang pl`.

### Dlaczego bez Vault?

Pełna kontrola nad ekonomią, brak konfliktów z Essentials, prosty deploy na serwerach Bell (BellLands + BellTrade + BellChat).

### BellTrade Pro (addon)

Adaptive pricing, analytics, edytor katalogu z GUI, Discord webhooks — osobny płatny addon.  
Dokumentacja: `Bell-Ecosystem/docs/belltrade-pro/` · Listing BBB: `promo-bbb.md`

---

## Komendy (skrót)

| Komenda | Opis |
|---------|------|
| `/btrade` | Menu główne |
| `/market` | Rynek |
| `/sellshop` | Sklep skupu |
| `/trade <gracz>` | Handel |
| `/balance`, `/pay`, `/baltop` | Ekonomia |

---

## Uprawnienia

`belltrade.use`, `belltrade.market`, `belltrade.trade`, `belltrade.shop`, `belltrade.admin` — domyślnie `true` dla graczy (admin: op).

---

## Wymagania

- Paper / Purpur **1.21.x**
- **Java 21+**
- Opcjonalnie: PlaceholderAPI, BellLands, LuckPerms

**Nie instaluj:** EssentialsX, Vault (nie są potrzebne).

---

## Instalacja

1. Wrzuć `BellTrade-1.26.1.2.jar` do `plugins/`
2. Uruchom serwer — powstanie `plugins/BellTrade/`
3. Edytuj `config.yml`, `shop/categories/*.yml` jeśli chcesz
4. `/btrade reload`

---

## PlaceholderAPI

`%belltrade_balance%`, `%belltrade_listings%`, `%belltrade_inflation%` — patrz `docs/placeholders.md`.

---

## Support / Links

- Dokumentacja w repo: `docs/architecture.md`, `docs/commands.md`
- Ekosystem Bell: BellLands (claimy, TPA), BellMarket (sklep premium)

**Autor:** Bellzeb | **Licencja:** Free — All Rights Reserved
