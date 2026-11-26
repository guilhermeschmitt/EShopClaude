# EShop design system — MASTER

Global source of truth for the storefront's look. Page files in `pages/` override this file for one page only.
The tokens are implemented in [`src/vue-front/storefront/src/styles/tokens.css`](../../src/vue-front/storefront/src/styles/tokens.css),
which remaps `@guilherme/vue-base`'s `--e-*` variables. Components come from vue-base; never hard-code a hex value in a view.

## 1. Source

Generated with the `ui-ux-pro-max` skill from its local databases (Python was not available, so the CSV rows were
read directly instead of through `search.py --design-system`):

| Database | Row | What it gave |
|---|---|---|
| `products.csv` | #3 E-commerce | Style "Vibrant & Block-based", palette focus "brand primary + success green", "high visual hierarchy" |
| `ui-reasoning.csv` | #3 E-commerce | Effect "card hover lift (200 ms)"; anti-patterns "flat design without depth", "text-heavy pages" |
| `colors.csv` | #3 E-commerce | Primary `#059669`, accent `#EA580C`, background `#ECFDF5`, muted text `#475569`, border `#A7F3D0`, destructive `#DC2626` |
| `typography.csv` | #40 E-commerce Clean | Headings Rubik, body Nunito Sans |
| `styles.csv` | #6 Vibrant & Block-based, #12 Flat Design | Block layout, high contrast, 200–300 ms colour-shift hovers; flat surfaces |
| `ux-guidelines.csv` | #9, #22, #23, #36, #66, #76, #118 | Reduced motion, touch targets, 8 px target spacing, 4.5:1 contrast, contextual badge announcements |

## 2. Direction

**"Market stall"**: a calm emerald storefront where each product category owns a colour block. The product has no
photography, so the category blocks carry the "vibrant & block-based" energy; everything around them (chrome, tables,
forms) stays flat and quiet so prices and quantities read first.

- The skill's vibrant style is tempered for a shop: one bright block per product, never neon, never animated backgrounds.
- Depth only where it signals interaction: cards lift on hover, summaries sit on a raised surface.
- Numbers are the content. Prices, quantities, totals and order numbers get their own type treatment (§4).

## 3. Colour

The skill's palette was adjusted only where it failed WCAG AA 4.5:1 for text:
`#059669` with white text is 3.8:1, so the button primary is one step darker; `#EA580C` is 3.6:1 on white, so text
uses `#C2410C`.

### Light (default)

| Role | Token (`--e-color-*`) | Value | Contrast |
|---|---|---|---|
| Page background | `bg` | `#F3F8F5` | — |
| Surface (cards) | `surface` | `#FFFFFF` | — |
| Surface hover | `surface-hover` | `#EAF3EE` | — |
| Border | `border` | `#D5E4DC` | — |
| Border strong | `border-strong` | `#A9C5B7` | — |
| Text | `text` | `#10231D` | 16.4:1 on surface |
| Text secondary | `text-secondary` | `#475569` | 7.6:1 on surface, 7.1:1 on bg |
| Primary (buttons, links, focus) | `primary` | `#047857` | 5.5:1 white on it |
| Primary hover / active | `primary-hover` / `-active` | `#065F46` / `#064E3B` | — |
| Primary subtle | `primary-subtle` | `#E3F6EC` | — |
| Accent (cart badge, "new") | `secondary` | `#C2410C` | 5.2:1 white on it |
| Success | `success` | `#15803D` | 5.0:1 |
| Danger | `danger` | `#DC2626` | 4.8:1 |

### Dark (`prefers-color-scheme: dark`, or `data-theme="dark"`)

| Role | Value |
|---|---|
| bg / surface / raised | `#0B1411` / `#111D19` / `#172722` |
| border / border strong | `#24382F` / `#335046` |
| text / text secondary | `#ECF5F0` / `#A8B8B0` (8.4:1 on surface) |
| primary (with dark text `#04241A`) | `#34D399` |
| accent | `#FB923C` |

### Category blocks

Each category gets a tint (tile background) and an ink (icon + label). Inks reach 4.5:1 on their tints.

| Category | Icon (Material Symbols) | Tint | Ink | Dark tint | Dark ink |
|---|---|---|---|---|---|
| Apparel | `checkroom` | `#EEF0FF` | `#4338CA` | `#1E1F45` | `#A5B4FC` |
| Bags | `backpack` | `#FFF0E6` | `#B4400B` | `#3A2113` | `#FDBA74` |
| Drinkware | `local_cafe` | `#E4F5FA` | `#0E7490` | `#0F2D36` | `#67E8F9` |
| Stationery | `edit_note` | `#FBEEFB` | `#A21CAF` | `#371A39` | `#F0ABFC` |
| Anything else | `inventory_2` | `#EDF1EF` | `#475569` | `#1C2925` | `#A8B8B0` |

## 4. Typography

| Use | Font | Size / weight | Notes |
|---|---|---|---|
| Display (page titles) | Rubik | `clamp(1.75rem, 1.4rem + 1.5vw, 2.25rem)` / 700 | `letter-spacing: -0.02em` |
| Section / card titles | Rubik | 1.125–1.25rem / 600 | |
| Body | Nunito Sans | 1rem / 400, line-height 1.55 | |
| Small / meta | Nunito Sans | **0.875rem minimum** / 600 | No text below 14 px anywhere, including badges and table headers |
| Prices, totals | Rubik | 1.125rem (card), 1.5–1.75rem (totals) / 600 | `tabular-nums lining-nums` |
| Quantities, order ids | Rubik | 1rem / 600 | `tabular-nums` |

Rules:
- Every number uses the `.num` utility (`font-variant-numeric: tabular-nums lining-nums`), so columns align.
- Numeric table columns are right-aligned; their headers are right-aligned too.
- Uppercase labels are avoided; when used (category chip) they keep 0.875 rem and 0.04 em tracking.
- Fonts are self-hosted (`@fontsource-variable/rubik`, `@fontsource-variable/nunito-sans`) with system fallbacks.

## 5. Space, radius, elevation

- Spacing: vue-base's 4 px scale (`--e-space-1` … `--e-space-16`). Page gutter 16 px on phones, 24 px from 640 px, 32 px from 1024 px.
- Content width: 1200 px max.
- Radius: `sm 6px`, `md 10px`, `lg 14px`, `xl 20px` (category tiles).
- Elevation: cards are flat with a 1 px border. Hover lift = `translateY(-2px)` + `--e-shadow-md`. Summary panels use `--e-shadow-sm`.

## 6. Layout and responsiveness

Mobile first. Breakpoints: **640 px** (sm), **960 px** (md), **1200 px** (lg).

| Pattern | < 640 px | ≥ 640 px | ≥ 960 px |
|---|---|---|---|
| Header | Brand + orders icon + cart icon, 56 px | Brand + text links | same |
| Product grid | 1 column, horizontal card (tile left) | 2–3 columns, vertical card | 4 columns |
| Filters | Stacked, full width | Category + search in one row | same |
| Line lists (cart, checkout, order) | Stacked line cards | Table | Table + summary sidebar (sticky) |
| Orders list | Stacked row cards | Table | same |

No horizontal page scroll at 320 px or wider.

## 7. Components (vue-base mapping)

| Need | Component | Notes |
|---|---|---|
| Buttons | `EButton` | One `type="primary"` per view. Height ≥ 44 px on touch. |
| Cards / panels | `ECard` | Product cards, summary panels, line lists |
| Empty states | `EEmptyState` | Always followed by one primary action |
| Filters | `ESelect`, `EInputText` | Visible labels |
| Icons | `EIcon` (Material Symbols Outlined) | Decorative icons `aria-hidden`; icon-only buttons have a label |
| Loading | `ECard loading` / `ESkeleton` | Skeletons match the final layout |
| Errors | `EToast` via `showError` | Unchanged behaviour |

## 8. Interaction and motion

- Transitions 150–200 ms, `--e-ease`. Colour shifts on hover; lift only on product cards.
- `prefers-reduced-motion: reduce` removes transforms and transitions.
- Focus: 2 px `--e-color-primary` ring with 2 px offset on every interactive element.
- Targets ≥ 44 × 44 px on touch, ≥ 8 px apart.
- The cart badge has a contextual accessible name ("Cart, 3 items"), never a bare number.

## 9. Anti-patterns

- Raw hex values in views, or overriding vue-base component internals beyond tokens.
- Text under 14 px, grey-on-grey, numbers in proportional figures.
- Neon colours, animated background patterns, scroll-snap (from the vibrant style, rejected for a shop).
- Tables that scroll horizontally on phones.
- Hover-only affordances.
