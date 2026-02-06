# Eunhye Hymn API 계약 (MVP)

Base URL: `/api/v1`
모든 경로는 위 Base URL 기준 상대 경로다.

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
  "refreshToken": "jwt-refresh-rotated"
}
```

### 1.4 로그아웃
- **POST** `/auth/logout`
- 요청:
```json
{
  "refreshToken": "jwt-refresh"
}
```
- 응답:
```json
{
  "success": true
}
```

### 1.5 DEV 로그인 (개발 환경 전용)
- **POST** `/auth/dev/login`
- 요청:
```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "role": "USER",
  "displayName": "개발 사용자"
}
```
- 응답:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh"
}
```

## 2. 찬송가
### 2.1 찬송가 목록
- **GET** `/hymns`
- 공개 엔드포인트 (인증 불필요)
- 응답:
```json
[
  {
    "id": "hymn_001",
    "title": "Amazing Grace",
    "number": "123",
    "tags": "advent"
  }
]
```

### 2.2 찬송가 상세
- **GET** `/hymns/{id}`
- 인증 필요
- 응답:
```json
{
  "id": "hymn_001",
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true,
  "lastOpenedAt": "2025-01-01T00:00:00Z",
  "assets": [
    {
      "id": "asset_001",
      "type": "PDF",
      "part": null,
      "url": "https://s3.../score.pdf",
      "checksum": null,
      "version": "v1"
    }
  ]
}
```

### 2.3 찬송가 관리 (관리자)
- 관리자 권한 필요

#### 2.3.1 찬송가 생성
- **POST** `/admin/hymns`
- 요청:
```json
{
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true
}
```
- 응답:
```json
{
  "id": "hymn_001",
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true
}
```

#### 2.3.2 찬송가 수정
- **PATCH** `/admin/hymns/{id}`
- 요청:
```json
{
  "title": "Amazing Grace (수정)",
  "enabled": false
}
```
- 응답:
```json
{
  "id": "hymn_001",
  "title": "Amazing Grace (수정)",
  "number": "123",
  "tags": "advent",
  "enabled": false
}
```

#### 2.3.3 찬송가 전체 목록 (관리자)
- **GET** `/admin/hymns`
- 응답:
```json
[
  {
    "id": "hymn_001",
    "title": "Amazing Grace",
    "number": "123",
    "tags": "advent",
    "enabled": true
  }
]
```

## 3. 내 정보
### 3.1 즐겨찾기 토글
- **POST** `/me/favorites/{hymnId}`
- 인증 필요
- 응답:
```json
{
  "favorite": true
}
```

### 3.2 노트 조회
- **GET** `/me/hymns/{hymnId}/note`
- 인증 필요
- 응답:
```json
{
  "content": "메모 내용"
}
```

### 3.3 노트 저장
- **PUT** `/me/hymns/{hymnId}/note`
- 인증 필요
- 요청:
```json
{
  "content": "메모 내용"
}
```
- 응답:
```json
{
  "content": "메모 내용"
}
```

### 3.4 최근 열람 기록
- **GET** `/me/history`
- 인증 필요
- 응답:
```json
[
  {
    "id": "hymn_001",
    "title": "Amazing Grace",
    "number": "123",
    "tags": "advent",
    "lastOpenedAt": "2025-01-01T00:00:00Z"
  }
]
```

## 4. 초대 코드 (관리자)
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

## 5. 미디어
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

### 4.2 관리자 에셋 업로드 프리사인
- 관리자 권한 필요

#### 4.2.1 업로드 URL 발급
- **POST** `/admin/assets/presign`
- part가 없으면 ALL로 처리
- 요청:
```json
{
  "hymnId": "hymn_001",
  "type": "PDF",
  "part": "S",
  "filename": "score.pdf",
  "contentType": "application/pdf"
}
```
- 응답:
```json
{
  "uploadUrl": "https://s3.../presigned",
  "publicUrl": "https://cdn.../hymns/hymn_001/PDF/S/score.pdf",
  "objectKey": "hymns/hymn_001/PDF/S/score.pdf"
}
```

#### 4.2.2 업로드 확인
- **POST** `/admin/assets/confirm`
- objectKey는 `hymns/{hymnId}/{type}/{part}/` 형식을 따라야 함
- 요청:
```json
{
  "hymnId": "hymn_001",
  "type": "PDF",
  "part": "S",
  "publicUrl": "https://cdn.../hymns/hymn_001/PDF/S/score.pdf",
  "objectKey": "hymns/hymn_001/PDF/S/score.pdf",
  "checksum": "abc123",
  "version": "v1"
}
```
- 응답:
```json
{
  "assetId": "asset_001",
  "hymnId": "hymn_001",
  "type": "PDF",
  "part": "S",
  "url": "https://cdn.../hymns/hymn_001/PDF/S/score.pdf",
  "objectKey": "hymns/hymn_001/PDF/S/score.pdf"
}
```

## 6. 이벤트
### 6.1 이벤트 기록
- **POST** `/events`
- 인증 필요
- 단건 또는 배열 허용
- 요청(단건):
```json
{
  "eventType": "HYMN_OPENED",
  "hymnId": "hymn_001",
  "part": "S",
  "metadataJson": "{\"device\":\"ios\"}"
}
```
- 요청(배열):
```json
[
  {
    "eventType": "PART_PLAYED",
    "hymnId": "hymn_001",
    "part": "A",
    "metadataJson": null
  }
]
```
- 응답:
```json
{
  "success": true
}
```
