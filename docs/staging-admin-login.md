# Staging Admin Login (Kakao)

Staging URL:
- `http://13.209.200.12/`

## What To Enter On Admin Login Page

- **Kakao Access Token**: Kakao OAuth *access token* string (starts with `Bearer` 없이 토큰 값만).
- **Invite Code**: 신규 사용자(처음 로그인)일 때만 필요. 기존 사용자면 비워도 됨.
  - 운영자 1인만 쓰는 환경이라면, 서버에서 운영자 allowlist를 설정하면 초대코드 없이도 운영자 계정 생성/로그인이 가능하도록 구성할 수 있음.

현재 스테이징 DB에 생성해둔 초대코드:
- `STAGE-BHG8VM`

## How To Get A Kakao Access Token

1) Kakao Developers 콘솔에서 해당 앱으로 이동
2) **카카오 로그인** 활성화 + (필요하면) **동의항목**에서 `프로필`, `이메일` 권한 설정
3) Kakao Developers의 **도구**에서 토큰을 발급받아 **Access Token** 값을 복사

주의:
- **ID Token이 아니라 Access Token** 입니다.
- 토큰 앞에 `Bearer `는 붙이지 말고, 토큰 문자열만 입력합니다.

## Operator(관리자) 1인만 허용하기

API 환경변수로 운영자 allowlist를 설정할 수 있습니다:
- `ADMIN_KAKAO_SUBJECTS`: 카카오 사용자 ID 목록 (comma-separated)
- `ADMIN_ENFORCE_ADMIN_ONLY=true`: allowlist에 없는 사용자는 로그인 자체를 차단

스테이징 DB에서 본인 카카오 ID(provider_subject)를 확인하는 예:
```sql
select provider, provider_subject, email, user_id, created_at
from auth_identities
order by created_at desc
limit 10;
```

