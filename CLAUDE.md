# SiseRadar — agent handoff (read this first)

> 시장을 스캔해서 좋은 거래를 포착하는 레이더. 한국 아파트 실거래가 분석 대시보드.

This file is the entry point for the coding agent. Read it fully, then read
`docs/SPEC.md` (product + technical spec) and `docs/BRAND.md` (visual identity)
before writing code.

## Context & goal

This is a **portfolio project**. The author is an experienced Java/Spring
backend developer who wants to demonstrate that they can ship a full-stack
product, with a **frontend that looks intentional** (built with heavy AI
assistance) **and a backend with real substance**. Target effort split is
roughly **frontend 6 : backend 4** — the frontend must be polished, the backend
must not be a thin proxy.

The single most valuable outcome is a **deployed, working dashboard fed by real
data, with a live URL**. Optimize for finishing and deploying the MVP, not for
feature breadth.

## How you (the agent) should work

- Before writing code for a new task, restate your plan in a few bullets and
  wait for confirmation. Use plan mode if available. Don't jump straight to code.
- Work in **small, verifiable increments** — one phase (or one sub-task within a
  phase) at a time. Never scaffold the whole app in one shot.
- After each increment, **build and run it, and check it against real data**.
  Fix compile/runtime errors before moving on.
- End each phase at a **working state and let the author commit** (git is the
  safety net). Keep `.env` out of git.
- **Don't fabricate external API behavior.** Hit the live data.go.kr endpoint
  early (key in `.env`) and parse a real response before building on top of it.
- Ask before large architectural changes, adding heavy dependencies, or
  deviating from `docs/SPEC.md`. Match `docs/BRAND.md` for anything visual.
- Prefer clear, idiomatic code the author can review over clever one-liners.

## What it does (one paragraph)

Public users explore Korean apartment transaction data by region: price trends,
volume, price-per-pyeong, complex rankings, charts, filters. Logged-in users add
regions/complexes to a watchlist and get notified when a new transaction appears
or a price moves past a threshold. Data comes from Korea's public data portal
(data.go.kr) and is **collected, stored, and aggregated by our own backend** —
not proxied live.

## Tech stack

- Backend: **Java 21**, Spring Boot 3, Spring Web, Spring Data JPA, Spring
  Security (JWT), Spring Scheduler, PostgreSQL, Jackson XML (parse the data.go.kr
  XML), springdoc-openapi (Swagger). **Gradle** build. H2 for local dev is fine.
- Frontend: **Node 20+**, React + Vite + TypeScript, TailwindCSS, Recharts
  (charts), TanStack Query (data fetching), React Router. Kakao Maps SDK is
  **v2 only**. (Vite dev server runs on `http://localhost:5173`.)
- Deploy: frontend → Vercel; backend → Railway (or Render); DB → Railway
  Postgres / Neon / Supabase. Docker Compose for local. No CI/CD required.

## Proposed repo structure

```
siseradar/
  backend/            # Spring Boot (Gradle, Java 21)
  frontend/           # React + Vite + TS
  docs/SPEC.md        # full spec — source of truth
  docs/BRAND.md       # visual identity
  assets/             # logo + design tokens (copy tokens into the frontend)
  .env.example        # required env vars
  .gitignore          # must exclude .env, build output, node_modules
```

Copy `assets/tokens.css` into the frontend global styles and merge
`assets/tailwind.config.js` into the frontend Tailwind config. Use
`assets/logo-mark.svg` and `assets/favicon.svg` as-is; `assets/logo-mark-128.png`
and `assets/logo-tile-128.png` are ready-made 128px raster versions.

## Non-negotiable conventions

1. **Korean market color semantics:** price **up = red (`#E5484D`)**,
   **down = blue (`#2F6FED`)** — the opposite of US convention. Never use the
   brand teal to signal up/down; teal is brand/accent only.
2. **Typography:** Pretendard for UI/body, JetBrains Mono for all numeric/ticker
   values, with `font-variant-numeric: tabular-nums` on data figures.
3. **Dark mode is first-class**, not an afterthought. Build both modes from the
   tokens. The navy base doubles as the dark background.
4. **Flat & precise:** thin borders (~0.5px), 8–12px radius, whitespace instead
   of shadows. The radar sweep is the one signature flourish (see BRAND.md) —
   reuse it as the loading indicator. Don't scatter extra animation.
5. **Secrets via env only.** The data.go.kr service key, DB creds, and JWT
   secret must come from environment variables (see `.env.example`). Never
   commit a key.
6. **Repo hygiene:** create a `.gitignore` that excludes `.env`, build output
   (`/build`, `/target`, `dist/`), and `node_modules/`. Real secrets live in
   `.env` (gitignored); `.env.example` keeps placeholders only.

## data.go.kr in one screen

- Base endpoint: `https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade`
- Operation: `getRTMSDataSvcAptTrade`
- Params: `serviceKey`, `LAWD_CD` (법정동 코드 앞 5자리), `DEAL_YMD` (YYYYMM),
  `pageNo`, `numOfRows`. Data exists from 2006 onward.
- Response is **XML** (parse with Jackson XML). Key fields: 거래금액(만원, 콤마
  포함 → strip & parse int), 전용면적, 층, 건축년도, 법정동, 아파트, 년/월/일.
- Gotchas: use the **encoding** service key; in Java watch for double-encoding of
  `+ / =`. Recent months keep updating (신고 지연) so re-collect them. Respect the
  daily call limit — collect region-by-region with backoff. The API has **no
  lat/lng**, so the map needs geocoding → that's why the map is v2.

## Build order (ship the MVP first)

- **Phase 0** — scaffold backend + frontend, set up DB and `.gitignore`, parse
  one `getRTMSDataSvcAptTrade` response into entities/DTOs.
- **Phase 1 (backend MVP)** — collection scheduler + `/api/trades` and
  `/api/stats/monthly` + Swagger. Get one region (e.g. `41135`) working
  end-to-end against the live API.
- **Phase 2 (frontend MVP)** — the region dashboard screen (KPI cards + charts +
  ranking table), responsive, dark mode. Wire to real API.
- **Phase 3** — JWT auth + watchlist + in-app alerts.
- **Phase 4** — compare view, complex detail, map (geocoding), design polish.
- **Phase 5** — deploy (Vercel + Railway + Postgres), write the README, get the
  live link.

After each phase, run it against real data before moving on. Prefer small,
verifiable steps over generating everything at once.

See `docs/SPEC.md` for the full DB schema, endpoint list, and deployment detail.

## Git & commits

- Branch: solo project — work on `main`, commit often. Feature branches optional.
- Commit at the end of each phase (and at any working checkpoint). One logical
  change per commit; don't bundle unrelated changes.
- Never commit `.env` or any secret. Verify with `git status` before committing.
- Message format (Conventional Commits): `type(scope): subject`
  - types: `feat` `fix` `chore` `docs` `refactor` `test` `style` `build`
  - scopes: `backend` `frontend` `api` `db` `infra` `brand`
  - subject: imperative, lowercase, ~50 chars (e.g. "add collection scheduler")
- Examples:
  - `chore: scaffold backend and frontend`
  - `feat(backend): add apt_trade collection scheduler`
  - `feat(api): add /api/stats/monthly aggregation`
  - `feat(frontend): region dashboard with KPI cards and charts`
  - `fix(backend): handle comma-separated 거래금액 parsing`
  - `docs: add deployment notes to README`