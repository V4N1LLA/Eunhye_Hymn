# Eunhye Hymn 모노레포

교회 내부 전용 찬송가/악보/파트 연습 음원 관리 앱을 위한 모노레포입니다.
웹 관리자, 모바일 클라이언트, 단일 API, 인프라, 문서를 한곳에서 관리하여 유지보수성과
AI 보조 개발 효율을 높이는 것을 목표로 합니다.

## 저장소 구조
- `apps/`
  - `apps/api`: 백엔드 API 서비스 (구현은 아직 없음)
  - `apps/admin`: 관리자 웹 앱 (구현은 아직 없음)
  - `apps/mobile`: 모바일 앱 (Flutter) (구현은 아직 없음)
- `infra/`
  - `infra/docker`: 로컬 개발/컨테이너 오케스트레이션
  - `infra/aws`: AWS 인프라 정의
- `docs/`
  - `docs/requirements.md`: MVP 요구사항
  - `docs/architecture.md`: 아키텍처 및 Clean Architecture 경계 규칙
  - `docs/api-contract.md`: API 계약 정의
  - `docs/data-model.md`: DB 스키마와 인덱스
  - `docs/events.md`: 이벤트 정의 및 메타데이터 스키마
  - `docs/dev-guide.md`: 로컬 개발 가이드
  - `docs/runbook.md`: 스테이징 운영/배포 런북
  - `docs/prompts`: AI 프롬프트 템플릿
  - `docs/usecases`: 유스케이스 문서
  - `docs/admin`: 관리자 문서
  - `docs/mobile`: 모바일 문서
- `.github/workflows`: CI/CD 워크플로

## 참고
- 교회 내부 전용 서비스입니다.
- 인증: 초대 코드 + Google/Kakao 로그인, JWT 액세스/리프레시 토큰 사용.
- 미디어: PDF 악보 및 MP3 파트 음원은 S3에서 제공.

---

제안하는 원자적 커밋 메시지:
- `chore: scaffold api db integration`
