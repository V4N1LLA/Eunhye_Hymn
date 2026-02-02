# Eunhye Hymn 이벤트 정의

## 1. 이벤트 메타데이터 스키마
모든 이벤트는 동일한 엔벨로프를 가진다:
```json
{
  "eventId": "evt_123",
  "eventType": "hymn.viewed",
  "occurredAt": "2025-01-01T00:00:00Z",
  "actor": {
    "userId": "usr_123",
    "role": "member"
  },
  "source": {
    "app": "mobile",
    "version": "1.0.0"
  },
  "payload": {}
}
```

## 2. MVP 이벤트 타입
### 2.1 hymn.viewed
- Payload:
```json
{
  "hymnId": "hymn_001"
}
```

### 2.2 hymn.created
- Payload:
```json
{
  "hymnId": "hymn_001",
  "title": "Amazing Grace"
}
```

### 2.3 hymn.updated
- Payload:
```json
{
  "hymnId": "hymn_001",
  "changedFields": ["tempo", "tags"]
}
```

### 2.4 invite.created
- Payload:
```json
{
  "inviteCode": "ABC123",
  "maxUses": 10
}
```

### 2.5 invite.revoked
- Payload:
```json
{
  "inviteCode": "ABC123"
}
```

### 2.6 auth.login
- Payload:
```json
{
  "provider": "google"
}
```

## 3. 메모
- 이벤트는 감사 로그, 분석, 디버깅에 활용.
- MVP에서는 발행이 no-op이어도 스키마는 고정한다.
