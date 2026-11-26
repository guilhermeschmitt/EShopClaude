# Web Interface Guidelines review (Step D)

Reviewed with the `web-design-guidelines` skill (Vercel Web Interface Guidelines, pinned commit 434b7f9) against
every storefront page: `App.vue`, `components/*.vue`, `views/*.vue`, `styles/*.css`, `index.html`.

## Fixed (high / medium)

| Location | Finding | Fix |
|---|---|---|
| `styles/base.css` `html` | Sticky header could cover focused elements and anchors | `scroll-padding-top` = header height + 16px |
| `views/HomeView.vue` add to cart | Async "added" state not announced (only the button text changed) | Visually hidden `role="status"` message: "<name> added to cart." |
| `styles/base.css` `h1–h3`, `p` | Long product names / descriptions had no break handling | `overflow-wrap: break-word` |
| `views/*.vue` empty and error states | Straight `"` and `'` in copy | Curly quotes and apostrophes (“ ” ’) |
| `views/HomeView.vue` search input | No meaningful `name` | `name="q"` (matches the URL param) |
| `styles/base.css` controls | No `touch-action`, default tap highlight | `touch-action: manipulation`, tap highlight set intentionally to transparent (buttons have their own active state) |
| `components/AppHeader.vue` brand | Brand name could be auto-translated | `translate="no"` |

Fixed earlier in Steps B and C and re-checked here: skip link, visible focus on every control (including vue-base fields),
`prefers-reduced-motion`, no `transition: all`, tabular numbers, `Intl` dates and prices, `color-scheme` and
`theme-color`, destructive action with an undo window, empty and error states with a next step, `…` in loading labels.

## Intentional exceptions

| Rule | Why it stays |
|---|---|
| Pager pages should be links | `storefront/04` pins them as `EButton` buttons (its checks use `getByRole('button', { name: 'Page 2' })`); the page still lives in the URL, so deep links and back/forward work. |
| Title Case for headings and buttons | The design plan sets sentence case ("Add to cart", "Place order"), matching the specs' copy. |
| Preload critical fonts | Fonts are self-hosted through `@fontsource-variable` with `font-display: swap` and hashed file names; there is no layout shift worth a hand-maintained preload. |
| Placeholders | The search field has a visible label ("Search products"); vue-base's `EInputText` has no placeholder prop. |

## Low (not changed)

- vue-base toasts use a left accent bar; they are library internals.
