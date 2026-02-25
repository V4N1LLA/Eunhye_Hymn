# Eunhye Hymn API 계약

Base URL: `/api/v1`

기본 응답 envelope:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

## 1) 인증/계정

### 1.1 초대코드 검증
- `POST /auth/invite/validate`
- 인증 없음(로그인 후 호출 시 현재 사용자에 초대코드 확정)
- 요청:
  ```json
  { "code": "ABC123" }
  ```
- 응답:
  ```json
  { "valid": true }
  ```

### 1.2 소셜 로그인 (Kakao)
- `POST /auth/social`
- 요청:
  ```json
  {
    "provider": "kakao",
    "token": "social-token",
    "inviteCode": "ABC123"
  }
  ```
- 응답:
  ```json
  {
    "accessToken": "jwt-access",
    "refreshToken": "jwt-refresh",
    "newUser": false
  }
  ```

### 1.3 사용자 계정 회원가입/로그인
- `POST /auth/signup`
  ```json
  {
    "loginId": "qa.user@example.com",
    "password": "password-123!",
    "inviteCode": "ABC123"
  }
  ```
- `POST /auth/login`
  ```json
  {
    "loginId": "qa.user@example.com",
    "password": "password-123!"
  }
  ```
- `loginId`는 영문/숫자/`._-` 조합 또는 이메일 형식(3~100자)을 지원
- 공통 응답:
  ```json
  {
    "accessToken": "jwt-access",
    "refreshToken": "jwt-refresh",
    "newUser": false
  }
  ```

### 1.4 관리자 로그인/비밀번호 변경
- `POST /auth/admin/login`
  ```json
  {
    "loginId": "owner",
    "password": "your-admin-password"
  }
  ```
- `POST /admin/auth/password` (관리자 토큰 필요)
  ```json
  {
    "currentPassword": "current-password",
    "newLoginId": "owner2",
    "newPassword": "new-password-456!"
  }
  ```

### 1.5 토큰 갱신/로그아웃
- `POST /auth/refresh`
  ```json
  { "refreshToken": "jwt-refresh" }
  ```
  응답:
  ```json
  { "accessToken": "jwt-access", "refreshToken": "jwt-refresh-2" }
  ```
- `POST /auth/logout`
  ```json
  { "refreshToken": "jwt-refresh" }
  ```
  응답 `data`: `null`

### 1.6 SMS 인증
- `POST /auth/sms/request` (인증 필요)
  ```json
  { "phoneNumber": "01012345678" }
  ```
  응답:
  ```json
  {
    "verificationId": "uuid",
    "expiresInSeconds": 300,
    "cooldownSeconds": 30
  }
  ```
- `POST /auth/sms/verify` (인증 필요)
  ```json
  {
    "verificationId": "uuid",
    "code": "123456"
  }
  ```
  응답:
  ```json
  {
    "verified": true,
    "completed": true
  }
  ```

### 1.7 회원 탈퇴
- `POST /auth/withdraw` (인증 필요)
- `DELETE /me/account` (인증 필요, `POST /auth/withdraw`와 동일 처리)
- 요청 본문 없음
- 응답 `data`: `null`

### 1.8 DEV 로그인 (개발 전용)
- `POST /auth/dev/login`

## 2) 찬양/AI

### 2.1 찬양 목록 (공개)
- `GET /hymns`
- 응답:
  ```json
  [
    { "id": "uuid", "title": "Amazing Grace", "number": "123", "tags": "grace,comfort" }
  ]
  ```

### 2.2 찬양 상세 (인증 필요)
- `GET /hymns/{id}`
- 응답 예시:
  ```json
  {
    "id": "hymn-uuid",
    "title": "Amazing Grace",
    "number": "123",
    "tags": "grace,comfort",
    "enabled": true,
    "lastOpenedAt": "2026-02-20T00:00:00Z",
    "assets": [
      {
        "id": "asset-uuid",
        "type": "PNG",
        "part": "ALL",
        "url": "https://cdn.example/hymns/.../PNG/ALL/score.png",
        "checksum": null,
        "version": "v1"
      }
    ]
  }
  ```

### 2.3 AI 찬송 추천 (인증 필요)
- `POST /ai/hymn-recommendations`
- 요청:
  ```json
  {
    "situation": "주일 새벽 예배, 차분한 묵상 분위기",
    "maxResults": 3
  }
  ```
- 응답:
  ```json
  {
    "items": [
      {
        "id": "hymn-uuid",
        "number": "101",
        "title": "찬송 제목",
        "tags": "grace,comfort",
        "reason": "상황에 맞는 분위기와 가사 주제"
      }
    ],
    "requestedMaxResults": 3,
    "candidateCount": 25
  }
  ```

## 3) 관리자 API

### 3.1 찬양 관리
- `GET /admin/hymns`
- `POST /admin/hymns`
- `PATCH /admin/hymns/{id}`
- `DELETE /admin/hymns/{id}`

### 3.2 에셋 관리
- `POST /admin/assets/presign`
  ```json
  {
    "hymnId": "hymn-uuid",
    "type": "PNG",
    "part": "ALL",
    "filename": "score.png",
    "contentType": "image/png"
  }
  ```
- `POST /admin/assets/confirm`
  ```json
  {
    "hymnId": "hymn-uuid",
    "type": "MIDI",
    "part": "S",
    "publicUrl": "https://cdn.example/hymns/hymn-uuid/MIDI/S/uuid.mid",
    "objectKey": "hymns/hymn-uuid/MIDI/S/uuid.mid",
    "checksum": "abc123",
    "version": "v1"
  }
  ```
- `DELETE /admin/assets/{id}`

### 3.3 사용자/초대코드
- 사용자:
  - `GET /admin/users`
  - `POST /admin/users`
  - `PATCH /admin/users/{id}`
  - `DELETE /admin/users/{id}` (soft delete)
  - Extended admin-user contract:
    - `GET /admin/users` supports optional query: `churchName`
    - `GET /admin/users` response includes:
      - `primaryEmail`
      - `profile` (`churchName`, `name`, `group`, `gender`, `updatedAt`)
      - `verification` (`phoneNumber`, `phoneVerifiedAt`, `phoneVerified`, `updatedAt`)
      - `identities[]` with plain `email` (plus masked fields)
    - `PATCH /admin/users/{id}` accepts:
      - `displayName`
      - `churchName`
      - `name`
      - `group`
      - `gender`
      - `phoneNumber`
- 초대코드:
  - `GET /admin/invite-codes`
  - `POST /admin/invite-codes`
  - `DELETE /admin/invite-codes/{code}`

### 3.4 개인정보 변경 요청 심사
- `GET /admin/profile-change-requests`
  - 쿼리 `status=PENDING|APPROVED|REJECTED` (생략 시 `PENDING`)
- `PATCH /admin/profile-change-requests/{id}`
  ```json
  { "action": "APPROVE" }
  ```
  ```json
  { "action": "REJECT", "rejectReason": "정보 확인 필요" }
  ```

### 3.5 감사 로그/분석
- `GET /admin/events`
- `GET /admin/events/export` (동기 CSV)
- `POST /admin/events/export-jobs` (비동기 작업 생성, `202 Accepted`)
- `GET /admin/events/export-jobs/{jobId}`
- `GET /admin/events/export-jobs/{jobId}/download`
- `GET /admin/events/export-jobs/metrics`

지원 쿼리(일부):
- `eventType`, `userId`, `hymnId`, `from`, `to`
- `page`, `size`, `limit`
- `summaryDays` (`GET /admin/events`)
- `days` (`GET /admin/events/export-jobs/metrics`)

## 4) 내 정보 API (`/me`)

### 4.1 프로필
- `GET /me/profile`
- `PUT /me/profile`
  ```json
  {
    "churchName": "은혜교회",
    "name": "홍길동",
    "group": "청년A",
    "gender": "UNKNOWN"
  }
  ```
- 응답 예시:
  ```json
  {
    "userId": "uuid",
    "role": "USER",
    "displayName": "Kakao User",
    "churchName": "은혜교회",
    "name": "홍길동",
    "group": "청년A",
    "gender": "UNKNOWN",
    "profileCompleted": true,
    "profileUpdatedAt": "2026-02-20T12:00:00Z",
    "inviteVerified": true,
    "phoneVerified": true,
    "verified": true
  }
  ```

### 4.2 개인정보 변경 요청
- `POST /me/profile-change-requests`
  ```json
  {
    "churchName": "은혜교회",
    "name": "홍길동",
    "group": "청년B",
    "gender": "UNKNOWN"
  }
  ```
- `GET /me/profile-change-requests/latest`
- 응답 예시:
  ```json
  {
    "id": "uuid",
    "status": "PENDING",
    "churchName": "은혜교회",
    "name": "홍길동",
    "group": "청년B",
    "gender": "UNKNOWN",
    "requestedAt": "2026-02-20T12:30:00Z",
    "reviewedBy": null,
    "reviewedAt": null,
    "rejectReason": null
  }
  ```

### 4.3 즐겨찾기/메모/히스토리
- `GET /me/favorites/{hymnId}`
- `POST /me/favorites/{hymnId}`
- `GET /me/hymns/{hymnId}/note`
- `PUT /me/hymns/{hymnId}/note`
  ```json
  { "content": "메모 내용" }
  ```
- `GET /me/history`

## 5) 이벤트/시스템

### 5.1 이벤트 기록
- `POST /events` (인증 필요)
- 단건 또는 배열 payload 허용

### 5.2 헬스 체크
- `GET /ping`
  ```json
  { "ok": true }
  ```
