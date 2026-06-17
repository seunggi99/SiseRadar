# SiseRadar

> 공공데이터 기반 아파트 실거래가 대시보드 

## 로컬에서 원격(Railway) Postgres 백필

기본 로컬 실행은 H2 파일 DB(`default` 프로파일)를 쓴다. 로컬 머신에서 **원격 Railway
Postgres로 직접 수집/백필**하려면 `railway` 프로파일로 실행한다.

1. `.env`(루트, gitignore됨)에 접속 정보를 넣는다 — `.env.example` 참고:
   ```
   RAILWAY_DB_URL=jdbc:postgresql://HOST:PORT/DB?sslmode=require
   RAILWAY_DB_USERNAME=...
   RAILWAY_DB_PASSWORD=...
   DATA_GO_KR_SERVICE_KEY=...   # 수집에 필요
   ```
   - `url`은 **반드시 JDBC 형식**(`jdbc:postgresql://...`). Railway가 주는 `postgresql://user:pass@host...`
     원형은 그대로 못 쓰니 호스트/포트/DB만 떼어 위 형식으로 바꾸고, user/password는 분리 주입.
     (형식이 틀리면 부팅 시 `RailwayDatasourceGuard`가 즉시 막는다.)
2. 실행 (`bootRun`은 루트 `.env`를 자동 주입):
   ```
   cd backend && ./gradlew bootRun --args='--spring.profiles.active=railway'
   ```
   - 이 프로파일은 `ddl-auto: update`라 원격에 스키마를 생성/갱신한다.
3. 백필은 기존 내부 수집 엔드포인트로 트리거 (예: `POST /api/internal/collect?lawdCd=11110&recentMonths=24`, 인증 토큰 필요).

> `application-railway.yml`에는 실제 접속값을 하드코딩하지 않는다 — 전부 환경변수.

