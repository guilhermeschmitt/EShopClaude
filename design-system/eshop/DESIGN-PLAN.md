# EShop storefront — design plan

Works inside [MASTER.md](MASTER.md). Written with the `frontend-design` skill: plan, review against the brief, revise, build.

## Subject, audience, job

- **Subject:** a small merch shop (24 products in Apparel, Bags, Drinkware, Stationery) with no product photography.
- **Audience:** someone browsing on a phone or laptop who wants to find a thing, see its price, and buy it in a few taps.
- **Primary job:** make products and their prices scannable, and make the cart → checkout → order path obvious.

## Brief (what this plan is checked against)

1. Components come from `@guilherme/vue-base`; colours change only through variables.
2. Every page is fully responsive (checked at 390 px and 1440 px).
3. Numbers and small text are highly readable.
4. Work within the ui-ux-pro-max design system (MASTER.md).
5. PROJECT.md §6 behaviour is unchanged: same routes, calls, states and `data-testid`s.

## First draft

- Colour: emerald primary, orange accent, mint background, category tints.
- Type: Rubik headings and prices, Nunito Sans body.
- Layout: centered page title; filter bar; grid of identical product cards (category label in small caps above the
  name, description, price, button); cart/checkout/order as one full-width table card with the total in the last row.
- Principle: clean, friendly, conversion-focused.

## Review against the brief — what changed and why

| Draft choice | Problem | Revision |
|---|---|---|
| Identical white product cards with a caps category label above the name | The SaaS-card kit plus the caps-eyebrow tell; with no photos every card looks the same | **Category block:** each card opens with a colour tile in its category's tint, carrying the category icon large. This is the one bold element, and it encodes information (which aisle the product is in). The category becomes a sentence-case chip inside the tile, not a caps eyebrow. |
| Centered page titles | Generic hero treatment; centered text fights the left-aligned grid and tables | Everything is left-aligned on one edge. Titles sit with a short line of context to their right/below ("24 products", "3 items", "Placed Nov 26, 2026, 3:04 PM"). |
| Total as the last row of a wide table | On a 390 px phone a 5-column table overflows or squeezes numbers below readability | **Line lists** become stacked line cards under 640 px (name on top, unit price × quantity = line total in a 3-column number row). From 960 px a **receipt summary** panel sits on the right, sticky, with the total set large in Rubik tabular figures. |
| Mint `#ECFDF5` page background from the palette | Saturated mint behind white cards washes the category tints out | Toned to `#F3F8F5`, so the category blocks are the only saturated areas. |
| Orange accent used for prices | Two loud colours competing with the category blocks; orange prices read as "sale" | Prices stay in the text colour, set heavier and larger. Orange is reserved for the cart badge. |
| Hover lift on every card and button | Motion everywhere reads as template | Lift only on product cards (the thing you choose). Buttons and rows change colour only. No entrance animations. |
| Small 12 px meta text (category, table headers) | Fails brief #3 | 14 px floor everywhere; table headers 14 px semibold in secondary text, not caps. |

## Final plan

### Colour (named)

| Name | Hex | Role |
|---|---|---|
| Ink | `#10231D` | Text, prices |
| Slate | `#475569` | Secondary text |
| Emerald | `#047857` | Primary actions, links, focus |
| Leaf wash | `#F3F8F5` | Page background |
| Paper | `#FFFFFF` | Cards, header |
| Ember | `#C2410C` | Cart badge only |

Category tints/inks and dark mode: MASTER.md §3.

### Type

- **Rubik** for headings, prices, totals, quantities, order numbers (all with tabular, lining figures).
- **Nunito Sans** for body, labels and descriptions. 14 px floor; body 16 px.
- Scale (1.25 ratio, rounded to rem): 14 · 16 · 18 · 20 · 24 · 30/36 (page titles, fluid).

### Layout

Header (all pages): sticky, white, 1 px bottom border. Brand at left; "My orders" and the cart at right. Under 640 px
"My orders" collapses to an icon button with an accessible name.

```
Home ≥ 960                                   Home < 640
┌───────────────────────────────────────┐    ┌───────────────────┐
│ EShop                 My orders  [🛒3] │    │ EShop      [≡] [🛒]│
├───────────────────────────────────────┤    ├───────────────────┤
│ Products   24 products                │    │ Products           │
│ [Category ▾] [Search……………] [Search]   │    │ 24 products        │
│ ┌──────┐┌──────┐┌──────┐┌──────┐      │    │ [Category ▾]       │
│ │▓tile▓││▓tile▓││▓tile▓││▓tile▓│      │    │ [Search…] [Search] │
│ │name  ││name  ││name  ││name  │      │    │ ┌────┬────────────┐│
│ │desc  ││desc  ││desc  ││desc  │      │    │ │tile│name  $19.90││
│ │$19.90││$22.50││$49.90││$17.00│      │    │ │    │desc        ││
│ │[Add] ││[Add] ││[Add] ││[Add] │      │    │ │    │[Add to cart]││
│ └──────┘└──────┘└──────┘└──────┘      │    │ └────┴────────────┘│
│        [1] [2] [3]                    │    │  [1] [2] [3]       │
└───────────────────────────────────────┘    └───────────────────┘

Cart / checkout / order ≥ 960                Cart < 640
┌───────────────────────────────────────┐    ┌───────────────────┐
│ Cart   3 items                        │    │ Cart  3 items      │
│ ┌──────────────────────┐ ┌──────────┐ │    │ ┌────────────────┐ │
│ │Product  Unit Qty Line│ │ Summary  │ │    │ │Name        [🗑]│ │
│ │■ Tee   19.90 -1+ 19.9│ │ Items  3 │ │    │ │$19.90  [-]1[+] │ │
│ │■ Mug   12.00 -2+ 24.0│ │ ──────── │ │    │ │        $19.90  │ │
│ └──────────────────────┘ │ Total    │ │    │ └────────────────┘ │
│                          │ $43.90   │ │    │ ┌ Summary ───────┐ │
│                          │[Checkout]│ │    │ │Total   $43.90  │ │
│                          └──────────┘ │    │ │[Checkout]      │ │
└───────────────────────────────────────┘    └───────────────────┘
```

Alignment: everything left-aligned; numbers right-aligned in their columns. Content max-width 1200 px.

### Principles

1. **The aisle colour is the image.** The category block is the only saturated, decorative element in the app.
2. **Numbers are set, not just printed.** Rubik, tabular figures, right-aligned, never below 16 px for money.
3. **One primary action per screen**: Add to cart (per card), Checkout, Place order, Browse products.
4. **Same shape at every width.** Tables become line cards on phones instead of scrolling sideways.
5. **Plain words.** "Add to cart", "Checkout", "Place order", "Back to my orders"; empty states say what to do next.

## Page by page

| Page | Changes |
|---|---|
| Shell (`App.vue`, `AppHeader.vue`) | Token theme, fonts, sticky header, 1200 px container with responsive gutters, focus ring, reduced motion |
| Home `/` | Title + result count, filter toolbar, category-block product cards (horizontal under 640 px), skeletons shaped like cards, empty state with a "Clear search" action |
| Cart `/cart` | Line list (table ≥ 640, cards < 640), receipt summary with Checkout, 44 px quantity controls, empty state with Browse products |
| Checkout `/checkout` | Read-only line list + receipt summary with Place order and Back to cart |
| My orders `/orders` | Table ≥ 640, order cards < 640, whole row clickable via its link, empty state with Browse products |
| Order detail `/orders/:id` | Back link on top (always shown), title with order number, "Placed …" line, line list + receipt summary. No status badge: Ordering has no status field, so one would be invented. |

## Behaviour that must not change

Routes, service calls, loading/empty/error states, disabled rules (one add/adjust at a time, Place order once) and
every `data-testid` stay exactly as in the storefront specs 01–13.
