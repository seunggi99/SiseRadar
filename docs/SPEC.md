# SiseRadar — product & technical spec

Source of truth for the build. Pairs with `CLAUDE.md` (agent instructions) and
`docs/BRAND.md` (visual identity).

---

## 1. Concept & goals

A dashboard for Korean apartment transaction prices (아파트 실거래가). Two pillars:

- **Explore (public, no login)** — pick a region and see how prices and volume
  move, compare areas, rank complexes. This pillar carries the frontend polish.
- **Personalize (login)** — watchlist of regions/complexes + alerts on new
  transactions or threshold price moves. This pillar carries backend substance.

Portfolio framing: prove full-stack delivery with a polished frontend and a
backend that owns a real data pipeline (scheduled collection, idempotent
storage, aggregation, auth, caching). Effort target ≈ frontend 6 : backend 4.

Definition of done for the MVP: **a deployed dashboard, fed by real data, with a
public URL**, covering at least Phases 0–2 below.

---

## 2. Features

### Frontend-weighted (the "6")

- Region drill-down: 시/도 → 구/군 → 동 (map-click is a v2 nicety).
- Dashboard KPI cards: this month's average price, transaction count,
  month-over-month change, price per pyeong (평당가).
- Charts: monthly average-price line, volume bar, price distribution by
  area-band, area-vs-price scatter.
- Complex ranking table: sortable, searchable, filterable, paginated.
- Compare mode: several regions side by side.
- Filters: period, area band, price band, build year.
- Details that signal craft: dark mode, responsive, loading skeletons, empty &
  error states.

### Backend-weighted (the "4")

- Collection scheduler (`@Scheduled`): iterate 법정동코드 × recent N months,
  fetch from data.go.kr, persist. **Idempotent** (unique constraint + upsert).
- Aggregation API: monthly average/median, price-per-pyeong, volume, MoM change,
  per-complex aggregates — computed server-side via GROUP BY.
- Caching: cache hot region/period results (in-memory; Redis optional).
- Auth: JWT signup/login, per-user data isolation.
- Watchlist + alerts: when a watched region/complex gets a new transaction or a
  price crosses a threshold, write a notification.
- 법정동코드 master table; indexed, paginated, filterable queries.

---

## 3. Screens

1. **Home / search** — region search, popular regions, nationwide summary.
2. **Region dashboard** — KPI + charts + ranking table (the main screen).
3. **Complex detail** — one apartment's transaction history, per-area trend.
4. **Compare** — multiple regions/complexes at once.
5. **My watchlist** (login) — saved items, alert rules, notifications inbox.
6. **Login / signup.**

---

## 4. Data source — data.go.kr (국토부 아파트 매매 실거래가)

- Base endpoint: `https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade`
- Operation: `getRTMSDataSvcAptTrade`
- Request params:
  - `serviceKey` — issued key (use the **encoding** variant)
  - `LAWD_CD` — 법정동 코드, **first 5 digits** (e.g. `41135` = 성남시 분당구)
  - `DEAL_YMD` — contract year-month `YYYYMM` (data from 2006 onward)
  - `pageNo`, `numOfRows` — pagination
- Example test URL:
  `https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade?serviceKey=KEY&LAWD_CD=41135&DEAL_YMD=202504&pageNo=1&numOfRows=10`
- Response: **XML**. Parse with Jackson XML (`jackson-dataformat-xml`). Fields of
  interest: 거래금액 (만원, comma-separated → strip commas, parse to int; store in
  won or 만원 consistently), 전용면적 (㎡), 층, 건축년도, 법정동, 아파트(단지명),
  년/월/일 (compose into a deal date), 지번.

### Data handling rules

- **Amount:** strip commas; decide a single unit (recommend storing 만원 as int,
  or 원 as long) and convert at the edge for display.
- **Area:** keep ㎡; derive 평 (= ㎡ / 3.305785) for display / per-pyeong.
- **Dedup:** a transaction can be re-reported. Unique key on
  (apt_name, area, floor, deal_date, deal_amount, lawd_cd).
- **Recency:** the most recent 1–2 months keep gaining rows as filings arrive —
  re-collect them on each run.

### Collection strategy (the backend's headline)

- **Backfill (one-off):** seed major regions × last 1–2 years in a batch job,
  chunked to respect the daily call limit.
- **Daily job (`@Scheduled`):** re-collect the last 1–2 months for tracked
  regions early each morning.
- **Idempotency:** upsert against the unique constraint so re-runs are safe.
- **Rate limits:** process region-by-region with backoff; cache responses.

### Gotchas checklist

- Use the **encoding** service key; in Java, avoid double-encoding `+ / =` in the
  key (encode once, or use the encoding key verbatim).
- 0 rows → check `LAWD_CD` (5 digits) and whether that month had any trades.
- New keys can take up to ~1 business day to activate.
- **No lat/lng in the API** → the map requires geocoding (단지명 + 법정동 via
  Kakao address search) cached into a complex table. This is why the map is v2.

---

## 4-A. 집계 지표 정의 (전용면적 기준)

지역 집계는 거래 구성(평형대·단지 믹스)에 휘둘리므로, **전용면적 기준 단위면적가 +
중위값 + 거래량**을 핵심 지표로 삼는다. 모든 지표는 `property_type`/`trade_type`별로 동작.

**면적 기준**
- 면적의 진실 원본은 API가 주는 **전용면적(㎡)** (아파트·오피스텔·연립). 단독·다가구는
  연면적, 상업·산업은 건물면적, 토지는 거래면적을 면적으로 쓴다(이 경우 "전용 기준" 아님).
- 평 환산은 **㎡ ÷ 3.3058** (전용 기준), 화면에 "전용 기준"이라고 명시한다.

**매매(SALE)**
- **단위면적가 = 거래금액 ÷ 전용면적** → `만원/㎡`. **평균과 중위(median)** 둘 다 제공
  (median은 `PERCENTILE_CONT`). 화면 평당가(전용) = 단위면적가 × 3.3058.
- 모든 가격 지표 옆에 **거래량(건수)** 을 함께 반환 — 평균이 튄 게 표본/구성 때문인지 보이게.
- **평형대 구간(전용면적 기준)**: ≤60 소형 / 60–85 중소형 / 85–135 중대형 / >135 대형.
  지역 전체뿐 아니라 구간별로 거래량·평균·중위 단위면적가를 집계.
- **단순 평균 거래가(`avgAmount`)** 는 유지하되 응답·화면에서 **"참고용"** 으로 라벨.
- **전월 대비(`momChangePct`)** 는 평균 거래가가 아니라 **중위 단위면적가** 변화율로 계산.
- **동일 단지 변동률** (`GET /api/stats/complex-change`): 두 시점에 모두 거래된 **같은 건물
  + 같은 평형대**만 골라 단위면적가의 평균 % 변동을 계산 → 구성 편향을 통제한 "진짜 추세".
  매칭 단지 수도 함께 반환.

**전월세(RENT)**
- 거래금액이 없으니 **면적당 보증금 = 보증금 ÷ 전용면적**(평균·중위)으로 단위면적가를 대체.
  평형대 구간 분해 동일 적용. 평균 월세도 함께 제공.

---

## 5. Database schema (initial)

`region` — 법정동 master
- `lawd_cd` (PK, varchar 5), `sido`, `sigungu`, `dong`

`apt_trade` — raw transactions
- `id` (PK), `lawd_cd` (FK), `deal_ymd` (char 6), `apt_name`, `area` (numeric),
  `floor` (int), `build_year` (int), `deal_amount` (bigint), `jibun`,
  `deal_date` (date)
- **unique** (`lawd_cd`, `apt_name`, `area`, `floor`, `deal_date`, `deal_amount`)
- indexes on (`lawd_cd`, `deal_ymd`) and (`apt_name`)

`apt_complex` — complex master (optional, needed for map/detail)
- `id` (PK), `name`, `lawd_cd`, `lat`, `lng` (geocoded, nullable)

`users`
- `id` (PK), `email` (unique), `password_hash`, `created_at`

`watchlist`
- `id` (PK), `user_id` (FK), `type` (REGION | COMPLEX), `ref_id`, `created_at`

`alert_rule`
- `id` (PK), `user_id` (FK), `watchlist_id` (FK),
  `condition` (NEW_TRADE | PRICE_CHANGE_PCT), `threshold` (nullable)

`notification`
- `id` (PK), `user_id` (FK), `message`, `is_read` (bool), `created_at`

Aggregates: start with on-the-fly GROUP BY queries. If read traffic grows, add a
precomputed `monthly_stats` table refreshed by the scheduler.

---

## 6. API endpoints (initial)

Public:
- `GET /api/regions?sido=` — region list / drill-down
- `GET /api/trades?lawdCd=&from=&to=&areaMin=&areaMax=&page=` — transactions
  (filtered, paginated)
- `GET /api/stats/monthly?lawdCd=&from=&to=` — monthly aggregates
  (avg/median/volume/price-per-pyeong/MoM)
- `GET /api/stats/complexes?lawdCd=&ym=` — complex ranking
- `GET /api/stats/compare?lawdCds=11650,41135&from=&to=` — comparison
- `GET /api/complexes/{id}/trades` — complex history

Auth (JWT):
- `POST /api/auth/signup`, `POST /api/auth/login`
- `GET / POST / DELETE /api/watchlist`
- `GET / PUT /api/alerts`
- `GET /api/notifications`, `PATCH /api/notifications/{id}/read`

Internal:
- Collection runs via `@Scheduled`. Optionally expose a **protected**
  `POST /api/internal/collect` so an external cron can trigger collection (useful
  if the backend host sleeps on a free tier).

Document everything with springdoc-openapi (Swagger UI).

---

## 7. Auth, caching, CORS

- JWT bearer auth; hash passwords with BCrypt; isolate all watchlist/alert/
  notification data per user.
- Cache hot `stats` responses (Spring Cache + in-memory, or Redis).
- CORS: allow the local dev origin (`http://localhost:5173`) and the deployed
  frontend origin (the Vercel URL). Read allowed origins from env.

---

## 8. Deployment

- **Frontend → Vercel** (free). Set `VITE_API_BASE_URL` to the backend URL.
- **Backend → Railway** Hobby (always-on, so `@Scheduled` runs) — or **Render**
  free + an external cron (cron-job.org / UptimeRobot) hitting
  `POST /api/internal/collect` to wake it and trigger collection.
- **DB → Railway Postgres / Neon / Supabase** (free Postgres). Avoid Render's
  free Postgres for anything you want to keep (it expires).
- Spring: bind to the platform port via `server.port=${PORT:8080}`. All secrets
  via env (see `.env.example`). Build via Dockerfile or platform auto-detect.
- Decoupled deploy (separate frontend/backend) is preferred over bundling the
  React build into Spring `static/` — it reads as real-world architecture.

---

## 9. Roadmap

- **Phase 0** — scaffold, DB, parse one response into entities/DTOs.
- **Phase 1** — collection scheduler + `/api/trades` + `/api/stats/monthly` +
  Swagger; one region end-to-end.
- **Phase 2** — region dashboard screen (KPI + charts + table), responsive, dark
  mode, wired to real API. **← MVP / deploy target.**
- **Phase 3** — auth + watchlist + alerts.
- **Phase 4** — compare, complex detail, map (geocoding), polish.
- **Phase 5** — deploy, README, live link (do a slimmer version of this at the
  end of Phase 2 too, so there's always a live demo).

## 10. Out of scope for MVP

- Map view (needs geocoding) → v2.
- 전월세 (rent) data, 오피스텔, other building types → later (same API family,
  different operations).
- Email/push notifications → start with an in-app notifications inbox.
