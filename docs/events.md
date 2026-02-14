# Eunhye Hymn 이벤트 정의

## 1. 목적
- 모바일/웹 사용자 행동 이벤트를 수집해 감사 로그, 분석, 디버깅에 활용한다.

## 2. 저장 스키마 (`events`)

```json
{
  "id": "uuid",
  "userId": "uuid",
  "eventType": "HYMN_OPENED",
  "hymnId": "uuid|null",
  "part": "S|A|T|B|ALL|null",
  "metadataJson": "{...}|null",
  "createdAt": "2026-02-13T11:40:00Z"
}
```

## 3. 이벤트 타입 (MVP)
- `HYMN_OPENED`: 찬양 상세 열람
- `PART_PLAYED`: 파트 재생
- `NOTE_SAVED`: 메모 저장
- `FAVORITE_TOGGLED`: 즐겨찾기 토글

## 4. 수집 API

### 4.1 사용자 이벤트 기록
- `POST /api/v1/events`
- 인증 필요
- 단건 또는 배열 요청 허용

요청 예시(배열):
```json
[
  {
    "eventType": "HYMN_OPENED",
    "hymnId": "hymn-uuid",
    "part": "ALL",
    "metadataJson": "{\"source\":\"mobile\"}"
  },
  {
    "eventType": "PART_PLAYED",
    "hymnId": "hymn-uuid",
    "part": "S",
    "metadataJson": "{\"speed\":\"1.0\"}"
  }
]
```

### 4.2 관리자 감사 로그 조회
- `GET /api/v1/admin/events`
- 관리자 권한 필요
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`
- 페이지네이션: `page`, `size` (`limit` 하위 호환)
- 집계: `summaryDays` 기준 최근 N일 이벤트 타입별 합계
  - 허용 범위: `1~90` (기본값 7)
  - 관리자 UI는 숫자 입력 + 프리셋(`1/7/30/60/90`)을 지원

### 4.3 관리자 감사 로그 CSV
- `GET /api/v1/admin/events/export`
- 관리자 권한 필요
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`
- 제한: `limit` (서버 상한 적용)

### 4.4 관리자 감사 로그 비동기 CSV (대용량)
- `POST /api/v1/admin/events/export-jobs`
  - 비동기 내보내기 작업 생성(202 Accepted)
  - 필터: `eventType`, `userId`, `hymnId`, `from`, `to`
  - `limit`: 기본 20,000 / 최대 100,000
- `GET /api/v1/admin/events/export-jobs/{jobId}`
  - 작업 상태 조회
  - 상태: `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`
- `GET /api/v1/admin/events/export-jobs/{jobId}/download`
  - 완료(`COMPLETED`) 작업 CSV 다운로드
  - 미완료/실패 작업은 `409` 반환

### 4.5 비동기 export 결과 정리 정책
- 완료(`COMPLETED`) 또는 실패(`FAILED`) 상태의 작업은 스케줄러가 주기적으로 정리한다.
- 기본 정책
  - 보관 일수: 7일
  - 정리 스케줄: 매일 03:15 UTC
- 설정 값
  - `EVENT_EXPORT_JOB_RETENTION_DAYS` (기본 7)
  - `EVENT_EXPORT_JOB_CLEANUP_CRON` (기본 `0 15 3 * * *`)
  - `EVENT_EXPORT_JOB_CLEANUP_ZONE` (기본 `UTC`)

## 5. 인덱스/성능 메모
- 관리자 조회 패턴 최적화를 위해 아래 인덱스를 사용한다.
  - `idx_events_user_created` (`user_id`, `created_at`) - 기존
  - `idx_events_created_at_desc` (`created_at DESC`)
  - `idx_events_event_type_created` (`event_type`, `created_at DESC`)
  - `idx_events_hymn_created` (`hymn_id`, `created_at DESC`)
- 운영 시 확인 항목
  - 관리자 이벤트 조회 응답 시간이 증가하면 `EXPLAIN ANALYZE`로 인덱스 사용 여부 확인
  - 이벤트 테이블 급증 시 CSV `limit` 정책과 백필/아카이빙 정책을 함께 점검
  - 대용량 비동기 export는 `event_export_jobs` 테이블 크기/완료율/실패율과 정리 삭제량을 함께 모니터링

