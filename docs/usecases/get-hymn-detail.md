# 유스케이스: 찬송가 상세 조회

## 목표
- 인증된 사용자에게 찬송가 상세와 자산 정보를 제공한다.

## 접근 조건
- 인증 필요.

## 처리 흐름
1. 찬송가를 조회한다.
2. 연결된 자산(PNG/MIDI)을 조회한다.
3. `user_hymn_state.last_opened_at`을 현재 시간으로 갱신한다.

## 응답 예시
```json
{
  "id": "hymn_001",
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true,
  "lastOpenedAt": "2025-01-01T00:00:00Z",
  "assets": [
    { "id": "asset_001", "type": "PNG", "part": "ALL", "url": "https://..." }
  ]
}
```
