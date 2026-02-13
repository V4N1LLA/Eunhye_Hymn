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
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`, `limit`
- 집계: `summaryDays` 기준 최근 N일 이벤트 타입별 합계

## 5. 운영 메모
- 이벤트는 텍스트 기반 `metadataJson`으로 저장한다.
- 감사 화면에서 문제 시간대/사용자/찬양 단위로 필터링해 원인 추적에 활용한다.
