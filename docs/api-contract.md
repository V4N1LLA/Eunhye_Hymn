# Eunhye Hymn API 계약 (MVP)

Base URL: `/api/v1`

## 1. 인증
### 1.1 초대 코드 검증
- **POST** `/auth/invite/validate`
- 요청:
```json
{
  "inviteCode": "ABC123"
}
```
- 응답:
```json
{
  "valid": true,
  "expiresAt": "2025-01-01T00:00:00Z"
}
```

### 1.2 소셜 로그인 (Google/Kakao)
- **POST** `/auth/social`
- 요청:
```json
{
  "provider": "google",
  "providerAccessToken": "...",
  "inviteCode": "ABC123"
}
```
- 응답:
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

### 1.3 토큰 갱신
- **POST** `/auth/refresh`
- 요청:
```json
{
  "refreshToken": "jwt-refresh"
}
```
- 응답:
```json
{
  "accessToken": "jwt-access",
  "expiresIn": 3600
}
```

## 2. 찬송가
### 2.1 찬송가 목록/검색
- **GET** `/hymns?query=grace&tag=advent&key=C&tempo=80&page=1&pageSize=20`
- 응답:
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

### 2.2 찬송가 상세
- **GET** `/hymns/{id}`
- 응답:
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

### 2.3 찬송가 생성/수정 (관리자)
- **POST** `/hymns`
- **PUT** `/hymns/{id}`
- 요청:
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
- 응답:
```json
{
  "id": "hymn_001"
}
```

### 2.4 찬송가 보관 처리 (관리자)
- **DELETE** `/hymns/{id}`
- 응답:
```json
{
  "archived": true
}
```

## 3. 초대 코드 (관리자)
### 3.1 초대 코드 생성
- **POST** `/invites`
- 요청:
```json
{
  "maxUses": 10,
  "expiresAt": "2025-01-01T00:00:00Z"
}
```
- 응답:
```json
{
  "inviteCode": "ABC123",
  "expiresAt": "2025-01-01T00:00:00Z",
  "maxUses": 10
}
```

### 3.2 초대 코드 폐기
- **POST** `/invites/{code}/revoke`
- 응답:
```json
{
  "revoked": true
}
```

## 4. 미디어
### 4.1 서명 URL 발급
- **POST** `/media/sign`
- 요청:
```json
{
  "objectKey": "scores/amazing-grace.pdf",
  "contentType": "application/pdf"
}
```
- 응답:
```json
{
  "signedUrl": "https://s3...",
  "expiresIn": 300
}
```
