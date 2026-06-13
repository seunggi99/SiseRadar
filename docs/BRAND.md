# SiseRadar — brand & visual identity

Pairs with `assets/` (logo, tokens). The aim is a frontend that reads as an
**intentional product**, not an AI default. Spend boldness in one place (the
radar signature) and keep everything else quiet and precise.

---

## Concept

A **radar that scans the market and spots good deals.** Concentric rings + a
sweeping beam + a **house-shaped blip** = "we detected an apartment for you."
This ties the visual identity directly to the product's headline feature
(watchlist alerts = the radar catching a blip).

---

## Signature element

The **radar sweep** is the one memorable flourish. Use it deliberately:

- It animates in the logo mark (slow 360° rotation).
- **Reuse the same sweep as the app's loading indicator** — when data is
  fetching, the radar spins. One motif doing real work, not decoration.
- Respect `prefers-reduced-motion`: freeze the sweep at a fixed angle.

Don't scatter other animations. The sweep + a few restrained hover transitions
is the whole motion budget.

---

## Logo

Files in `assets/`:
- `logo-mark.svg` — the radar mark (animated sweep). Monochrome teal so it works
  on both light and dark backgrounds.
- `favicon.svg` — navy rounded-square tile with the teal mark, for browser tabs.

Lockup: place the mark left of the wordmark **SiseRadar**, with `Sise` in the
text color and `Radar` in teal (deep teal `#0B8276` on light, bright teal
`#39E0CE` on dark). The wordmark is live HTML text in Pretendard — not baked into
the SVG — so it stays crisp and themeable.

Clearspace: keep padding around the mark equal to ~½ the ring diameter. Don't
recolor the blip to anything but teal, don't add glow/shadow, don't place the
light-bg lockup on a busy photo.

---

## Color

| Token | Hex | Role |
|---|---|---|
| Radar Navy | `#0E1B2E` | primary dark / text on light / dark-mode background |
| Navy Soft | `#13243B` | dark-mode surfaces |
| Signal Teal | `#14C2B2` | brand accent, radar sweep & blip |
| Teal Deep | `#0B8276` | teal text/icons on light backgrounds |
| Teal Bright | `#39E0CE` | teal accent on dark backgrounds |
| Teal Tint | `#BDEDE7` | soft fills, pills, highlights |
| Slate | `#5B6B7F` | muted text |
| Mist | `#EAF0F4` | light surface / dividers |
| Cloud | `#F6F9FB` | light-mode page background |
| **상승 Up** | `#E5484D` | **price increase (red — Korean convention)** |
| **하락 Down** | `#2F6FED` | **price decrease (blue — Korean convention)** |

Rules:
- **Up = red, down = blue.** This is the opposite of US markets and is correct
  for a Korean audience. Apply it consistently to deltas, arrows, chart series.
- Keep brand teal **separate** from the up/down semantics so color always means
  one thing.
- Two-to-three colors per view. Teal is the accent; red/blue are reserved for
  price direction; everything else is navy/slate/neutral.

---

## Typography

- **Pretendard** — UI, headings, body (excellent KR + EN). Free; load via
  jsDelivr CDN or self-host.
- **JetBrains Mono** — all numeric/ticker values (prices, %, counts). The mono
  ticker is a deliberate pairing that gives the data a precise, "terminal" feel
  — that contrast is part of the identity, not a neutral default.
- Put `font-variant-numeric: tabular-nums` on every data figure so digits don't
  jitter as values change.
- Two weights only: 400 regular, 500 medium. Sentence case everywhere. No ALL
  CAPS, no heavy 700.

Suggested scale: page title 22–24px / section 18px / card label 13px muted /
body 15–16px / hero ticker 24–28px mono.

---

## UI principles

- Flat surfaces, thin borders (~0.5px), 8–12px corner radius, whitespace instead
  of drop shadows.
- **Dark mode is first-class.** The navy base is the natural dark background and
  suits a radar/data product; build both modes from `assets/tokens.css`.
- KPI cards: muted 13px label, 24px mono value, a small teal left-accent.
- Charts: teal for the primary series; red/blue only when encoding direction.
- Quality floor (build it, don't announce it): responsive to mobile, visible
  keyboard focus, reduced-motion respected, real empty/error/loading states.

---

## Copy / voice

- Plain, specific, conversational. Sentence case. Name things the way a user
  thinks ("관심 단지", "이번 달 평균가") — not by how the system is built.
- Empty states invite action ("관심 지역을 추가하면 새 거래를 레이더가 알려드려요").
- Errors say what happened and how to fix it, in the interface's voice, no
  apology-padding. An action keeps its name through the flow (button "추가" →
  toast "추가됨").

---

## What to avoid (so it doesn't read as templated)

- The generic "big number + gradient accent" hero. Lead with the radar + live
  regional data instead.
- Neon glow on the teal. It's a flat signal color, not a gamer accent.
- Rainbow chart palettes. Color encodes meaning here (brand vs up vs down).
- Over-animating. The sweep is the moment; keep the rest still.
