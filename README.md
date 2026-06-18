# SiseRadar (시세레이더)

> 한국 부동산의 국토부 실거래가를 지역별로 수집·집계하는 분석 대시보드. 단순 평균이 놓치는 **거래 구성 편향을 걷어낸 시세 추세**를 보여준다.   

**데모**: https://siseradar.vercel.app

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React_18-61DAFB?style=flat-square&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript_5-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite_5-646CFF?style=flat-square&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_v3-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![Recharts](https://img.shields.io/badge/Recharts-22B5BF?style=flat-square&logo=chartdotjs&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-000000?style=flat-square&logo=vercel&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)
![Built with Claude Code](https://img.shields.io/badge/Built_with-Claude_Code-D97757?style=flat-square&logo=anthropic&logoColor=white)

<table>
  <tr>
    <td align="center"><b>지역 대시보드 (KPI·차트·랭킹·AI 시장 요약)</b><br><img src="https://github.com/user-attachments/assets/63d555d7-f3a0-47c3-96b4-9e72f67e36b8" width="320" /></td>
    <td align="center"><b>지도 — 시세·상승률 색 모드</b><br><img src="https://github.com/user-attachments/assets/6785937d-4ed4-409a-a77b-35b9eb09a5cf" width="320" /></td>
    <td align="center"><b>지역 평균가 추이 비교</b><br><img src="https://github.com/user-attachments/assets/cd75ad67-4998-4d88-a5dc-e77226b48168" width="320" /></td>
  </tr>
</table>

---

## 개요

**AI 페어 프로그래밍(Claude Code)으로 기획부터 배포까지 약 21시간(실작업 약 12시간)으로 단독으로 완성한 프로젝트입니다.**   
구현 속도는 AI로 끌어올리되, 지표 정의와 집계 SQL 설계, 막힌 디버깅·배포 트러블슈팅처럼 사람이 판단해야 할 지점은 직접 파고들었습니다.    
그 덕에 짧은 기간에도 공공데이터를 실제로 수집·집계하는, 라이브 URL로 동작하는 완성도까지 끌어올렸습니다.

| 직접 판단·개입한 지점 | AI로 구현한 부분 |
| --- | --- |
| 집계 지표 정의(중위 단위면적가, 동일단지 변동률), 분석 SQL(`PERCENTILE_CONT`·동일단지 매칭) 설계, 멱등 수집 모델, **`@Lob` Postgres 버그·배포 트러블슈팅** | React UI·차트·폼 마크업, 컴포넌트 스타일, 단순 CRUD·DTO 와이어링, 보일러플레이트 |

> **핵심 난제.** 지역 전체 평균은 거래 구성(평형·단지 믹스)에 휘둘려 "대형이 많이 팔린 달"을 "오른 달"로 착시하게 만든다. 이 구성 편향을 걷어낸 추세 지표를 정의하고 SQL로 구현하는 것이 출발점이었다.

---

## 핵심 요약

- **무엇.** 국토부 실거래가(data.go.kr)를 백엔드가 직접 수집·저장·집계해 지역별 시세·거래량·평당가를 시각화 (라이브 프록시가 아니라 자체 DB에 적재)
- **왜 어려운가.** 단순 평균은 거래 구성에 휘둘려 추세를 왜곡한다. 그래서 평균 대신 **중위 단위면적가**와 **동일단지 변동률**을 핵심 지표로 설계
- **결과.** 같은 건물·평형대만 매칭해 "같은 물건이 실제로 얼마 올랐나"를 보여주고, 표본(매칭 단지 수)까지 투명하게 노출

#### 단순 평균 변동률 vs 동일단지 변동률 — 같은 지역, 다른 추세 측정

| 측정 방식 | 단순 평균 변동률 *(참고용)* | 동일단지 변동률 *(핵심 지표)* |
| --- | --- | --- |
| **무엇을 비교** | 이번 달 전체 평균 vs 지난달 전체 평균 | 두 시점에 **모두 거래된** 같은 건물+같은 평형대 셀만 매칭 |
| **구성 편향** | 대형 평형이 많이 팔리면 "상승" 착시 발생 | 구성 효과 제거 → 동일 물건의 실제 변동만 측정 |
| **표본 투명성** | 평균이 튄 이유가 안 보임 | 매칭된 단지 수 반환 · 표본 부족 시 `hasData=false`로 0% 착시 차단 |

화면의 KPI와 전월대비(MoM)조차 단순 평균가가 아니라 중위 단위면적가의 변화율로 계산해, "참고용 평균"과 "추세 지표"를 의도적으로 분리했다.

---

## AI 협업 방식

**워크플로우**
- **설계 먼저, 구현은 위임.** `CLAUDE.md`와 `docs/SPEC.md`를 단일 진실 공급원으로 두고 지표 정의·스키마·컨벤션을 문서화한 뒤, 그 명세대로 구현
- **작게 쪼개고 실데이터로 검증.** 매 단계 live data.go.kr 응답을 파싱해 확인한 뒤에만 다음으로 진행 (Conventional Commits로 단계별 커밋)

**직접 판단·설계한 핵심**
- **구성 편향 통제 지표.** 지역 평균(`avgAmount`)은 응답에서 "참고용"으로 명시 라벨, MoM은 중위 단위면적가(`PERCENTILE_CONT(0.5)`) 변화율, 추세는 동일단지 변동률(건물+평형대 매칭, 12개월 vs 직전 12개월)로 계산하도록 SQL 설계
- **유형 혼합 차단.** 모든 집계를 `(property_type, trade_type)`로 스코프해 매매 거래가와 전월세 보증금이 안 섞이게 하고, 단위면적가는 `COALESCE(deal_amount, deposit) / area`로 통일
- **멱등 수집.** `dedupKey`(유형·거래·법정동·일자·단지·면적·층·금액) UNIQUE 제약 + run 내 중복 제거로, 신고 지연에 따른 최근 달 재수집에도 안전. null은 `-`로 정규화해 NULL 비교 함정 회피
- **수집 아키텍처 전환 판단.** 로컬→원격 Railway DB 직수집이 느린 걸 보고, 배포 앱이 DB 옆에서(co-located) 직접 수집하도록 방향을 틀어 원격 왕복을 제거

**막혔을 때 직접 추적해 해결한 사례**
- **지도 마커 깜빡임.** AI의 추측성 패치를 멈추고 로깅으로 근본 원인을 규명: 지도를 팬하면 bbox 쿼리의 `queryKey`가 바뀌며 `data`가 잠깐 `undefined`가 돼 마커 전체가 제거·재생성(`kept:0`)되는 쿼리 blanking이었고, `keepPreviousData`로 처방
- **AI 요약과 카드 수치 불일치.** 요약(+35.2%)과 동일단지 카드(+41.4%)가 어긋난 걸 수치까지 짚어 포착. 원인은 화면마다 비교 기간·집계가 달랐던 것이라, "최근 12개월 vs 직전 12개월" 고정 윈도 단일 지표로 정의해 지도 마커·카드·AI 요약이 같은 계산을 쓰게 통일
- **`@Lob` Postgres LOB 버그.** `RegionInsight`의 요약·`basisJson`을 `@Lob`로 매핑하면 Postgres가 large-object OID로 저장해 *"Large Objects may not be used in auto-commit mode"* 로 읽기가 깨짐을 규명. `columnDefinition = "text"`로 근본 수정하고(트랜잭션만 붙이는 우회는 orphaned LOB 누적이라 기각) 캐시 read/write를 방어
- **지오코딩 동명 지역 오매칭.** 다른 동에 같은 단지명이 오배치될 위험을 선제 인지하고 검증을 설계: 역지오코딩 추가 호출 대신 Kakao 검색 응답의 `region_2depth_name`(시군구)이 기대 시군구와 일치하는지만 확인해(2콜→1콜) 비국내(중·일·북) 좌표나 동명 오매칭을 `FAILED`로 차단

**대규모 수집을 운영하며 — 파일럿 검증 후 무중단 위임, 쿼터·인프라 제약에 맞춰 설계 변경**

서울 25개 구와 성남 분당을 대상으로, 부동산 전 유형 매매(10년)·전월세(가용분) 약 **500만 건**의 실거래를 직접 수집·집계했다.

- **분당으로 먼저 검증.** 성남 분당구를 부동산 전 유형 매매/전월세(8개 유형 × 매매/전월세 = 12개 데이터셋) · 최근 10년(2016~2026, 121개월) 범위로 먼저 수집해, 파싱·멱등 저장·집계 파이프라인이 실데이터에서 맞는지 확인한 뒤 서울 25구로 확대.
- **서울은 무중단으로 위임.** 멱등·재개 가능한 수집을 백그라운드로 돌리고(끊겨도 적재된 곳은 건너뛰고 이어감) 진행·실패만 주기적으로 점검. 그 과정에서 부딪힌 운영 현실에 맞춰 설계를 세 번 바꿨다.

| 부딪힌 문제 | 진단 | 설계 변경 |
| --- | --- | --- |
| 로컬→원격 DB 직수집이 한 달치에 5분+ | 행마다 로컬↔Railway 프록시 왕복 + `@GeneratedValue(IDENTITY)`로 JDBC 배치 무력화 | 배포 앱이 DB 옆에서(co-located) 수집하도록 전환. 외부는 트리거만, 쓰기는 같은 네트워크 — 병목이 DB 왕복에서 data.go.kr fetch로 이동 |
| 적재 중 쓰기만 500, 읽기는 정상 | `psql` 쓰기 프로브로 "No space left on device" 확인, Postgres 볼륨 풀 | Railway 볼륨 증설 후 멱등 재개 |
| 전월세·토지 API가 먼저 429 | 거래 많은 유형이 `numOfRows=100` 페이지네이션으로 일일 한도(API·거래유형별 개별)를 빨리 소진 | `numOfRows` 100→1000(호출 ~1/10) + 429 graceful(80줄 스택 대신 한 줄로 줄여 Railway 로그 rate-limit 폭주 차단) + 유형·거래 필터로 쿼터 남은 것만 골라 수집 |

일일 쿼터가 **유형·거래별로 따로** 소진되므로 "아파트 전월세만 막혀도 아파트 매매는 계속 수집"처럼 남은 쿼터만 골라 쓰도록 수집 엔드포인트를 일반화했다.

---

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어 / 런타임 | Java 21 · Node 20+ / TypeScript 5 |
| 백엔드 | Spring Boot 3.3 (Web · Data JPA · Security · Scheduler) · Gradle |
| 실거래가 파싱 | Jackson XML (`jackson-dataformat-xml`) — data.go.kr XML 응답 |
| 인증 | JWT (`jjwt` 0.12, HMAC-SHA256) · BCrypt · Stateless |
| DB | PostgreSQL (prod / Railway) · H2 file (로컬 dev) |
| 집계 | JPA + 네이티브 SQL (`GROUP BY`, `PERCENTILE_CONT`, 동일단지 매칭) |
| API 문서 | springdoc-openapi (Swagger UI) |
| AI 요약 | Google Gemini `gemini-2.5-flash` (실패·쿼터 시 템플릿 폴백) |
| 지오코딩 | Kakao Local API (단지·지역 좌표, 캐시) |
| 프론트엔드 | React 18 · Vite 5 · TailwindCSS 3 · React Router 6 |
| 데이터 페칭 | TanStack Query 5 |
| 차트 / 지도 | Recharts 2 · Kakao Maps JS SDK v2 (MarkerClusterer) |
| 디자인 | Pretendard(UI) · JetBrains Mono(수치, `tabular-nums`) · 다크모드 first-class |
| 배포 | 프론트 → Vercel · 백엔드 → Railway · DB → Railway Postgres |

---

## 핵심 기능

- **지역 드릴다운 대시보드.** 시/도 → 구/군 선택 시 KPI 카드(중위 단위면적가·거래량·MoM·평당가), 월별 추세 차트(1/3/6/12개월 버킷), 정렬·검색·페이지네이션 단지 랭킹
- **동일단지 변동률 패널** *(이 프로젝트의 핵심)* — 평형·단지 믹스 효과를 제거한 진짜 추세 + 매칭 단지 수
- **평형대 구간 분해.** 전용면적 ≤60 / 60–85 / 85–135 / >135 구간별 거래량·중위 단위면적가
- **전유형·매매/전월세 지원.** 아파트·오피스텔·연립 등 12종 수집, 화면은 주거 3종 × 매매/전월세 선택
- **지도 시각화.** 지역 버블(전체 거래) + 단지 마커(지오코딩)를 줌 레벨로 전환, 시세 모드(teal 순차)와 상승률 모드(빨강/파랑 발산)를 토글, 클러스터링
- **AI 시장 요약.** 동일 집계를 근거로 Gemini가 지역 시세를 줄글로 요약, 24h 캐시 + 근거 변경 감지 재생성
- **로그인·관심목록·알림.** JWT 인증, 지역/단지 관심 등록, 새 거래(NEW_TRADE)나 전월 대비 가격 변동(PRICE_CHANGE_PCT)이 임계치를 넘으면 인앱 알림
- **크래프트 디테일.** 다크모드, 반응형, 로딩 스켈레톤·빈/에러 상태, 레이더 스윕 로딩 인디케이터(`prefers-reduced-motion` 준수), 한국 시장 색 규칙(상승=빨강 `#E5484D`, 하락=파랑 `#2F6FED`)

---

## 핵심 로직 & 데이터 모델 (요약)

```
real_estate_transaction          # 실거래 원본 — dedupKey UNIQUE (멱등 수집)
  │  모든 집계는 (property_type × trade_type)로 분리 → 매매가/전월세 보증금 안 섞임
  └  단위면적가(만원/㎡) = COALESCE(deal_amount, deposit) / area

집계 (on-the-fly GROUP BY + PERCENTILE_CONT, (유형·거래)별)
  ├─ 월별 통계   : 평균/중위 거래가 · 중위 단위면적가 · 거래량 · MoM(중위 단위면적가 기준)
  ├─ 평형대 구간 : ≤60 / 60–85 / 85–135 / >135 별 거래량·중위 단위면적가
  ├─ 단지 랭킹   : 거래량·평균/최고가·평당가(전용)
  └─ 동일단지 변동률 : 최근 12개월 vs 직전 12개월, 같은 건물+평형대만 매칭 (avg%·median%·matched)

users ──< watchlist ──< alert_rule          # JWT·BCrypt, 사용자별 데이터 격리
                            └ NEW_TRADE / PRICE_CHANGE_PCT  →  notification (인앱)

region_insight     # Gemini 요약 캐시: 24h TTL + basisJson(근거 수치) 변경 감지 재생성
complex_geocode / region_centroid   # Kakao 지오코딩 캐시 (시군구 토큰 검증, @Async 워커)
```

**수집 파이프라인.** `@Scheduled`(기본 매일 04:00)가 설정 지역 × 최근 N개월 × 유형을 순회하며 data.go.kr XML을 가져와 페이지네이션·백오프로 적재한다. 신고 지연을 고려해 최근 달을 매일 재수집하되 `dedupKey` UNIQUE 제약으로 멱등을 보장한다. 새 거래가 삽입되면 곧바로 `AlertEvaluationService`가 관심목록 알림을 평가한다.

---

## 로컬 실행

```bash
# 0) 환경변수 — .env.example 복사 후 채우기 (.env는 gitignore됨)
cp .env.example .env
#   DATA_GO_KR_SERVICE_KEY (encoding 키) · JWT_SECRET 필수
#   KAKAO_REST_API_KEY / VITE_KAKAO_MAP_KEY (지도) · GEMINI_API_KEY (비우면 템플릿 폴백)

# 1) 백엔드 — 로컬은 H2 파일 DB (default 프로파일), bootRun이 루트 .env 자동 주입
cd backend && ./gradlew bootRun        # http://localhost:8080 · Swagger: /swagger-ui.html

# 2) 프론트엔드
cd frontend && npm install && npm run dev   # http://localhost:5173
```

> 원격 Railway Postgres로 직접 수집/백필하려면 `railway` 프로파일로 실행한다(상세는 아래 "프로젝트 구조"의 `application-railway.yml`).
> 예: `./gradlew bootRun --args='--spring.profiles.active=railway'` 후 `POST /api/internal/collect?lawdCd=11110&recentMonths=24`.

## 프로젝트 구조

```
backend/                       # Java 21 · Spring Boot 3.3 · Gradle
  src/main/java/com/siseradar/
    collect/                   # 수집 — 스케줄러·data.go.kr·Kakao 클라이언트·멱등 저장
    web/                       # 공개 API — StatsService(집계)·Trade/Region/Stats 컨트롤러
    insight/                   # AI 요약 — Gemini 클라이언트·InsightService·근거(Basis)
    map/                       # 지도 — 버블/마커·@Async 지오코딩 워커
    auth/ watchlist/ alert/ notification/   # JWT·관심목록·알림 규칙·인앱 알림
    domain/ repository/ config/             # 엔티티·JPA(네이티브 SQL)·보안/CORS/Railway
frontend/                      # React 18 · Vite 5 · TS · Tailwind
  src/pages/                   # Dashboard · Compare · Map · Watchlist · Login
  src/components/              # KpiCard · TrendChart · 랭킹표 · 동일단지 패널 · InsightCard · RadarSpinner
  src/api/                     # client · TanStack Query 훅 · 타입
  src/lib/                     # 색/테마/포맷/가격스케일/Kakao 지도 유틸
docs/SPEC.md  docs/BRAND.md    # 제품·기술 명세 / 시각 아이덴티티 (소스 of truth)
assets/                        # 디자인 토큰 · 로고 · 파비콘
```
