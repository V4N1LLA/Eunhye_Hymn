# Staging Admin Login (Kakao)

Staging URL:
- `http://13.209.200.12/`

## What To Enter On Admin Login Page

- **Kakao Access Token**: Kakao OAuth *access token* string (starts with `Bearer` 없이 토큰 값만).
- **Invite Code**: 신규 사용자(처음 로그인)일 때만 필요. 기존 사용자면 비워도 됨.

현재 스테이징 DB에 생성해둔 초대코드:
- `STAGE-BHG8VM`

## How To Get A Kakao Access Token

1) Kakao Developers 콘솔에서 해당 앱으로 이동
2) **카카오 로그인** 활성화 + (필요하면) **동의항목**에서 `프로필`, `이메일` 권한 설정
3) Kakao Developers의 **도구**에서 토큰을 발급받아 **Access Token** 값을 복사

주의:
- **ID Token이 아니라 Access Token** 입니다.
- 토큰 앞에 `Bearer `는 붙이지 말고, 토큰 문자열만 입력합니다.

