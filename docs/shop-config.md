# BellTrade — Konfiguracja sklepu skupu

## Podział Free vs Pro

| Operacja | Free | Pro |
|----------|------|-----|
| Sprzedaż przedmiotów graczom (`/sellshop`) | ✅ | ✅ |
| Dynamiczne ceny (`PriceEngine`) | ✅ uproszczony | ✅ `AdaptivePriceEngine` |
| **Edycja cen** istniejących pozycji | YAML lub Admin GUI (Sesja 5) | ✅ + `BulkPriceEditorGUI` |
| **Dodawanie / usuwanie kategorii** | tylko YAML | ✅ `ShopCatalogEditorGUI` |
| **Dodawanie / usuwanie przedmiotów** | tylko YAML | ✅ `ShopCatalogEditorGUI` |
| Blacklista skupu | YAML / `config.yml` | YAML + GUI |

> **Decyzja projektowa:** zarządzanie strukturą katalogu (kategorie, lista przedmiotów)
> jest funkcją **BellTrade Pro**. Wersja Free pozostaje w pełni funkcjonalna przez pliki YAML.

---

## Free — pliki na serwerze

```
plugins/BellTrade/shop/
├── blacklist.yml
└── categories/
    ├── ores.yml
    ├── crops.yml
    └── … (dowolne pliki .yml)
```

Po pierwszym starcie plugin kopiuje domyślne pliki z JAR. **Nie są nadpisywane** przy aktualizacji.

### Format kategorii (`shop/categories/nazwa.yml`)

```yaml
id: ores                    # unikalny identyfikator (slug)
icon: IRON_INGOT            # ikona w GUI kategorii
name: "&6Rudy & Minerały"   # nazwa wyświetlana (& kolory)
items:
  IRON_INGOT:
    base-price: 8.0         # cena bazowa (przed PriceEngine)
    min-price: 2.0          # dolny limit dynamicznej ceny
    max-price: 30.0         # górny limit
```

### Dodanie kategorii

1. Utwórz nowy plik w `shop/categories/`, np. `tools.yml`.
2. Wypełnij `id`, `icon`, `name`, `items`.
3. `/btrade reload`

### Usunięcie kategorii

Usuń plik `.yml` z `shop/categories/` → `/btrade reload`

### Dodanie przedmiotu

Dopisz blok pod `items:` w wybranej kategorii → `/btrade reload`

### Usunięcie przedmiotu

Usuń wpis z `items:` → `/btrade reload`

### Blacklista

`shop/blacklist.yml`:

```yaml
materials:
  - BEDROCK
  - SPAWNER
```

Dodatkowo: `config.yml` → `shop.blacklist: [MATERIAL, …]`

---

## Pro — `ShopCatalogEditorGUI` (planowany, repo `BellTrade-Pro`)

Dostęp z rozbudowanego Admin GUI (slot **Sell shop → Katalog**). Wymaga licencji Pro.

### Planowany flow

**Kategorie**
- Lista kategorii → **Nowa kategoria** (nazwa, ikona z ręki)
- Klik kategorii → edycja nazwy / ikony
- **Usuń kategorię** (z potwierdzeniem; zapis do YAML)

**Przedmioty w kategorii**
- **Dodaj z ręki** — trzymany przedmiot → `base-price` / `min` / `max` na czacie lub w pod-GUI
- Klik istniejącego → edycja cen lub **usuń z katalogu**
- Zapis atomowy do `shop/categories/<id>.yml` + `ShopConfigManager.reload()`

### Uprawnienia (plan)

| Node | Opis |
|------|------|
| `belltrade.pro.shop.catalog` | Otwarcie edytora katalogu |
| `belltrade.pro.shop.catalog.create` | Nowe kategorie i przedmioty |
| `belltrade.pro.shop.catalog.delete` | Usuwanie kategorii i przedmiotów |

### Integracja z Free

Pro rozszerza Free — **nie zastępuje** `ShopConfigManager`. Zapis idzie do tych samych plików YAML,
więc serwer bez Pro nadal czyta katalog po reload; edycja struktury wymaga Pro lub ręcznego YAML.

---

## Powiązane

- [`architecture.md`](architecture.md) — `BulkPriceEditorGUI`, Free/Pro split
- [`roadmap.md`](roadmap.md) — Sesja 5 (Free: edytor cen), Pro-1 (katalog)
