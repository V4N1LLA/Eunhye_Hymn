# 유스케이스: 관리자 감사 로그 조회

## 목표
- 관리자에게 사용자 행동 이벤트 로그와 최근 이벤트 집계를 제공한다.

## 접근 조건
- 관리자(ADMIN) 권한 필요.

## 처리 흐름
1. 필터(`eventType`, `userId`, `hymnId`, `from`, `to`)를 적용해 최신 이벤트를 조회한다.
2. `summaryDays` 기준으로 최근 N일 이벤트 타입별 집계를 계산한다.
3. 로그 목록(`items`)과 집계(`summary`), 페이지네이션(`pagination`)을 함께 반환한다.

## API
- `GET /api/v1/admin/events`
- `GET /api/v1/admin/events/export` (CSV 다운로드)

## 주요 파라미터
- `eventType`: `HYMN_OPENED`, `PART_PLAYED`, `NOTE_SAVED`, `FAVORITE_TOGGLED`
- `userId`, `hymnId`: UUID
- `from`, `to`: ISO-8601 UTC
- `page`, `size`: 페이지네이션 (기본 1/50, 최대 size 200)
- `limit`: 하위 호환 조회 개수 파라미터
- `summaryDays`: 집계 일수(기본 7, 최대 90)

## 응답 예시
```json
{
  "items": [
    {
      "id": "event-uuid",
      "userId": "user-uuid",
      "eventType": "HYMN_OPENED",
      "hymnId": "hymn-uuid",
      "part": "ALL",
      "metadataJson": "{\"source\":\"mobile\"}",
      "createdAt": "2026-02-13T11:40:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "size": 50,
    "total": 2100,
    "totalPages": 42,
    "hasPrevious": false,
    "hasNext": true
  },
  "summary": {
    "fromInclusive": "2026-02-06T00:00:00Z",
    "toExclusive": "2026-02-13T00:00:00Z",
    "total": 210,
    "byType": [
      { "eventType": "HYMN_OPENED", "count": 120 },
      { "eventType": "PART_PLAYED", "count": 50 },
      { "eventType": "NOTE_SAVED", "count": 30 },
      { "eventType": "FAVORITE_TOGGLED", "count": 10 }
    ]
  }
}
```
