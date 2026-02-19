# Eunhye Hymn API 계약 (MVP)

Base URL: `/api/v1`
모든 응답은 기본적으로 아래 envelope를 사용한다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

## 1. 인증

### 1.1 초대 코드 검증
- `POST /auth/invite/validate`
- 요청 `data` 예시:
```json
{
  "code": "ABC123"
}
```
- 응답 `data` 예시:
```json
{
  "valid": true
}
```

### 1.2 소셜 로그인 (Kakao)
- `POST /auth/social`
- 요청 `data` 예시:
```json
{
  "provider": "kakao",
  "token": "social-token",
  "inviteCode": "ABC123"
}
```
- 응답 `data` 예시:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh",
  "newUser": false
}
```

### 1.3 토큰 갱신
- `POST /auth/refresh`
- 요청 `data`: `{ "refreshToken": "jwt-refresh" }`
- 응답 `data`: `{ "accessToken": "...", "refreshToken": "..." }`

### 1.4 로그아웃
- `POST /auth/logout`
- 요청 `data`: `{ "refreshToken": "jwt-refresh" }`
- 응답 `data`: `null`

### 1.5 Admin ID/PW 로그인
- `POST /auth/admin/login`
- 요청 `data` 예시:
```json
{
  "loginId": "owner",
  "password": "your-admin-password"
}
```
- 응답 `data` 예시:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh",
  "newUser": false
}
```

### 1.6 Admin ID/PW 변경 (관리자 토큰 필요)
- `POST /admin/auth/password`
- 요청 `data` 예시:
```json
{
  "currentPassword": "current-password",
  "newLoginId": "owner2",
  "newPassword": "new-password-456!"
}
```
- 응답 `data` 예시:
```json
{
  "loginId": "owner2",
  "updatedAt": "2026-02-15T14:00:00Z"
}
```

### 1.7 사용자 회원가입
- `POST /auth/signup`
- 요청 `data` 예시:
```json
{
  "loginId": "member.one",
  "password": "password-123!",
  "inviteCode": "ABC123"
}
```
- 응답 `data` 예시:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh",
  "newUser": true
}
```

### 1.8 사용자 로그인
- `POST /auth/login`
- 요청 `data` 예시:
```json
{
  "loginId": "member.one",
  "password": "password-123!"
}
```
- 응답 `data` 예시:
```json
{
  "accessToken": "jwt-access",
  "refreshToken": "jwt-refresh",
  "newUser": false
}
```

### 1.9 DEV 로그인 (개발/테스트 전용, 운영 앱 미사용)
- `POST /auth/dev/login`

## 2. 찬양

### 2.1 찬양 목록 (공개)
- `GET /hymns`
- 응답 `data`: `[{ id, title, number, tags }]`

### 2.2 찬양 상세 (인증 필요)
- `GET /hymns/{id}`
- 응답 `data` 예시:
```json
{
  "id": "hymn-uuid",
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true,
  "lastOpenedAt": "2025-01-01T00:00:00Z",
  "assets": [
    {
      "id": "asset-uuid",
      "type": "PNG",
      "part": "ALL",
      "url": "https://cdn.example/hymns/.../PNG/ALL/score.png",
      "checksum": null,
      "version": "v1"
    },
    {
      "id": "asset-uuid-2",
      "type": "MIDI",
      "part": "S",
      "url": "https://cdn.example/hymns/.../MIDI/S/soprano.mid",
      "checksum": null,
      "version": "v1"
    }
  ]
}
```

### 2.3 찬양 관리 (관리자)
- `POST /admin/hymns` (생성)
- `PATCH /admin/hymns/{id}` (수정)
- `DELETE /admin/hymns/{id}` (삭제)
- `GET /admin/hymns` (전체 목록)

## 3. 에셋 관리 (관리자)

### 3.1 프리사인 발급
- `POST /admin/assets/presign`
- 요청 `data` 예시:
```json
{
  "hymnId": "hymn-uuid",
  "type": "PNG",
  "part": "ALL",
  "filename": "score.png",
  "contentType": "image/png"
}
```
- 응답 `data` 예시:
```json
{
  "uploadUrl": "https://s3-presigned-url",
  "publicUrl": "https://cdn.example/hymns/hymn-uuid/PNG/ALL/uuid-score.png",
  "objectKey": "hymns/hymn-uuid/PNG/ALL/uuid-score.png"
}
```

### 3.2 업로드 확인
- `POST /admin/assets/confirm`
- 요청 `data` 예시:
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

### 3.3 에셋 삭제
- `DELETE /admin/assets/{id}`

## 4. 사용자/초대코드 관리 (관리자)

### 4.1 사용자 관리
- `GET /admin/users`
- `POST /admin/users`
- `PATCH /admin/users/{id}`
- `DELETE /admin/users/{id}` (soft-delete: `status=DISABLED`)

### 4.2 초대코드 관리
- `POST /admin/invite-codes`
- `GET /admin/invite-codes`
- `DELETE /admin/invite-codes/{code}`

### 4.3 감사 로그/분석
- `GET /admin/events`
- `GET /admin/events/export` (CSV 다운로드)
- `POST /admin/events/export-jobs` (비동기 대용량 CSV 작업 생성, 202 Accepted)
- `GET /admin/events/export-jobs/{jobId}` (작업 상태 조회)
- `GET /admin/events/export-jobs/{jobId}/download` (완료 작업 다운로드)
- `GET /admin/events/export-jobs/metrics` (비동기 export 운영 지표)
- 지원 쿼리:
  - `eventType`: `HYMN_OPENED` | `PART_PLAYED` | `NOTE_SAVED` | `FAVORITE_TOGGLED`
  - `userId`, `hymnId`: UUID
  - `from`, `to`: ISO-8601 UTC
  - `page`: 페이지 번호 (기본 1)
  - `size`: 페이지 크기 (기본 50, 최대 200)
  - `limit`: 하위 호환 조회 개수 파라미터(미지정 시 `page/size` 사용)
  - `summaryDays`: 최근 집계 일수 (기본 7, 최대 90)
  - `days`: 운영 지표 집계 일수 (기본 7, 최대 90, `GET /admin/events/export-jobs/metrics` 전용)
- 비동기 export 스냅샷 규칙:
  - `POST /admin/events/export-jobs`에서 `to`를 생략하면 서버가 작업 생성 시각을 `toExclusive`로 고정한다.
  - 실행 대기 중 신규 유입 이벤트는 해당 작업 결과에서 제외된다.
- 응답 `data` 예시:
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

- `GET /admin/events/export` 응답:
  - `Content-Type: text/csv`
  - `Content-Disposition: attachment; filename="admin-events-*.csv"`
  - 보안: CSV 셀 값이 수식(`=`, `+`, `-`, `@`)으로 시작하면 이스케이프 처리

- `POST /admin/events/export-jobs` 응답 `data` 예시:
```json
{
  "id": "job-uuid",
  "status": "QUEUED",
  "exportLimit": 50000,
  "rowCount": null,
  "fileName": null,
  "errorMessage": null,
  "createdAt": "2026-02-14T05:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "statusUrl": "/admin/events/export-jobs/job-uuid",
  "downloadUrl": "/admin/events/export-jobs/job-uuid/download",
  "downloadable": false
}
```

- `GET /admin/events/export-jobs/metrics` 응답 `data` 예시:
```json
{
  "windowDays": 7,
  "fromInclusive": "2026-02-07T05:00:00Z",
  "toExclusive": "2026-02-14T05:00:00Z",
  "jobs": {
    "total": 24,
    "queued": 1,
    "running": 0,
    "completed": 21,
    "failed": 2,
    "failureRatePercent": 8.7
  },
  "processing": {
    "measuredJobs": 23,
    "averageSeconds": 14.8,
    "p95Seconds": 39.0
  },
  "cleanup": {
    "runCount": 7,
    "deletedJobs": 43
  }
}
```

## 5. 사용자 개인 영역

### 5.1 프로필
- `GET /me/profile`

### 5.2 즐겨찾기
- `GET /me/favorites/{hymnId}`
- `POST /me/favorites/{hymnId}` (toggle)

### 5.3 메모
- `GET /me/hymns/{hymnId}/note`
- `PUT /me/hymns/{hymnId}/note`
- 요청 `data`: `{ "content": "메모 내용" }`

### 5.4 히스토리
- `GET /me/history`

## 6. 이벤트

### 6.1 사용자 이벤트 기록
- `POST /events`
- 단건 또는 배열 요청 허용
- 이벤트 필드: `eventType`, `hymnId`, `part`, `metadataJson`

## 7. 시스템

### 7.1 헬스 체크
- `GET /ping`
- 응답 `data` 예시:
```json
{
  "ok": true
}
```
