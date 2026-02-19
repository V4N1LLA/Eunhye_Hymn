# 관리자 문서

관리자 웹(`apps/admin`)의 핵심 운영 기능과 화면 기준을 정리한다.

## 1. 화면/기능 범위

- 로그인
  - 운영자 ID/PW 로그인(기본)
  - localhost 전용 Dev 로그인 보조
- 대시보드: 요약 카드/운영 진입
- 찬양 관리: 목록/생성/수정/삭제, 검색/필터, 활성화 토글
- 에셋 관리: Presign -> Upload -> Confirm, 삭제
- 사용자 관리: 생성/조회/수정/삭제(soft-delete)
- 초대코드 관리: 생성/조회/비활성화
- 감사 로그: 조회/집계/CSV export(동기/비동기) + 운영 지표

주요 라우트:
- `/`
- `/hymns`, `/hymns/new`, `/hymns/:id/edit`
- `/assets/upload`
- `/users`
- `/invite-codes`
- `/events`

## 2. 에셋 업로드(프리사인)

관리자 웹 에셋 업로드는 3단계로 처리한다.

1. Presign (`POST /api/v1/admin/assets/presign`)
2. Upload (S3 presigned URL PUT)
3. Confirm (`POST /api/v1/admin/assets/confirm`)

규칙:

- `part` 미지정 시 `ALL`
- objectKey 접두사: `hymns/{hymnId}/{type}/{part}/...`
- Confirm 시 동일 hymn/type/part 조합의 최신 항목 기준으로 갱신

## 3. 감사 로그/분석

경로: `/events`

### 3.1 조회/집계

- API: `GET /api/v1/admin/events`
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`
- 페이지네이션: `page`, `size` (`limit` 하위 호환)
- 집계: `summaryDays` (1~90)

### 3.2 CSV 내보내기

- 동기: `GET /api/v1/admin/events/export`
- 비동기 대용량:
  - `POST /api/v1/admin/events/export-jobs`
  - `GET /api/v1/admin/events/export-jobs/{jobId}`
  - `GET /api/v1/admin/events/export-jobs/{jobId}/download`

### 3.3 운영 지표

- API: `GET /api/v1/admin/events/export-jobs/metrics`
- 확인 항목: queued/running/completed/failed, 실패율, 평균/p95 처리시간, cleanup 삭제량

## 4. 운영 체크 포인트

1. 로그인 실패 시 세션 초기화 후 재로그인
2. 이벤트 조회 시 기간 필터(`from/to`)와 `summaryDays`를 분리해서 사용
3. 대용량 추출은 비동기 export 경로를 우선 사용
4. 스테이징 점검 결과는 `docs/staging-smoke-log.md`에 누적

## 5. 관련 문서

- `docs/api-contract.md`
- `docs/events.md`
- `docs/runbook.md`
- `docs/staging-smoke-checklist.md`
- `docs/staging-admin-login.md`
