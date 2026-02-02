# Eunhye Hymn API Contract (MVP)

Base URL: `/api/v1`

## 1. Auth
### 1.1 Validate Invite Code
- **POST** `/auth/invite/validate`
- Request:
```json
{
  "inviteCode": "ABC123"
}
```
- Response:
```json
{
  "valid": true,
  "expiresAt": "2025-01-01T00:00:00Z"
}
```

### 1.2 Social Login (Google/Kakao)
- **POST** `/auth/social`
- Request:
```json
{
  "provider": "google",
  "providerAccessToken": "...",
  "inviteCode": "ABC123"
}
```
- Response:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh",
  "expiresIn": 3600,
  "user": {
    "id": "usr_123",
    "role": "member",
    "name": "Grace Kim"
  }
}
```

### 1.3 Refresh Token
- **POST** `/auth/refresh`
- Request:
```json
{
  "refreshToken": "jwt-refresh"
}
```
- Response:
```json
{
  "accessToken": "jwt-access",
  "expiresIn": 3600
}
```

## 2. Hymns
### 2.1 List/Search Hymns
- **GET** `/hymns?query=grace&tag=advent&key=C&tempo=80&page=1&pageSize=20`
- Response:
```json
{
  "items": [
    {
      "id": "hymn_001",
      "number": 123,
      "title": "Amazing Grace",
      "key": "C",
      "tempo": 80,
      "tags": ["advent"],
      "season": "advent"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 200
}
```

### 2.2 Get Hymn Detail
- **GET** `/hymns/{id}`
- Response:
```json
{
  "id": "hymn_001",
  "number": 123,
  "title": "Amazing Grace",
  "key": "C",
  "tempo": 80,
  "tags": ["advent"],
  "season": "advent",
  "scorePdfUrl": "https://s3.../score.pdf",
  "parts": [
    {
      "part": "soprano",
      "audioUrl": "https://s3.../soprano.mp3"
    }
  ]
}
```

### 2.3 Create/Update Hymn (Admin)
- **POST** `/hymns`
- **PUT** `/hymns/{id}`
- Request:
```json
{
  "number": 123,
  "title": "Amazing Grace",
  "key": "C",
  "tempo": 80,
  "tags": ["advent"],
  "season": "advent",
  "scorePdfKey": "scores/amazing-grace.pdf",
  "parts": [
    {
      "part": "soprano",
      "audioKey": "audio/amazing-grace-soprano.mp3"
    }
  ]
}
```
- Response:
```json
{
  "id": "hymn_001"
}
```

### 2.4 Archive Hymn (Admin)
- **DELETE** `/hymns/{id}`
- Response:
```json
{
  "archived": true
}
```

## 3. Invites (Admin)
### 3.1 Create Invite
- **POST** `/invites`
- Request:
```json
{
  "maxUses": 10,
  "expiresAt": "2025-01-01T00:00:00Z"
}
```
- Response:
```json
{
  "inviteCode": "ABC123",
  "expiresAt": "2025-01-01T00:00:00Z",
  "maxUses": 10
}
```

### 3.2 Revoke Invite
- **POST** `/invites/{code}/revoke`
- Response:
```json
{
  "revoked": true
}
```

## 4. Media
### 4.1 Get Signed URL
- **POST** `/media/sign`
- Request:
```json
{
  "objectKey": "scores/amazing-grace.pdf",
  "contentType": "application/pdf"
}
```
- Response:
```json
{
  "signedUrl": "https://s3...",
  "expiresIn": 300
}
```
