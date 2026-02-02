# Eunhye Hymn Event Types

## 1. Event Metadata Schema
All events share a common envelope:
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

## 2. MVP Event Types
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

## 3. Notes
- Events are used for audit, analytics, and debugging.
- Event publication can be a no-op in MVP; ensure schema is stable for future.
