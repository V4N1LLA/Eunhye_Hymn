# 유스케이스 문서

이 폴더는 실제 구현된 API/화면 흐름 기준으로 핵심 유스케이스를 정리한다.

## 문서 작성 규칙
- 제목: 유스케이스 이름
- 주요 사용자: 관리자/멤버/시스템
- 사전 조건: 인증/권한/입력 조건
- 처리 흐름: 정상/예외 케이스
- 관련 데이터: 영향 받는 엔티티/테이블
- API 참조: 관련 엔드포인트

## 문서 목록

### 인증/공통
- `auth-jwt.md`: JWT 인증 개요
- `dev-login.md`: 개발용 로그인
- `refresh-token.md`: 리프레시 토큰 재발급
- `ping.md`: 헬스체크

### 찬양/개인화
- `list-hymns.md`: 찬양 목록 조회
- `get-hymn-detail.md`: 찬양 상세 조회
- `toggle-favorite.md`: 즐겨찾기 토글
- `save-note.md`: 노트 저장/조회
- `get-history.md`: 최근 열람 기록 조회
- `ai-hymn-recommendations.md`: AI 찬송 추천

### 운영/관리자
- `admin-manage-hymns.md`: 관리자 찬양 관리
- `presigned-upload.md`: 관리자 에셋 프리사인 업로드
- `admin-list-events.md`: 관리자 감사 로그/분석 및 CSV export
- `record-events.md`: 이벤트 기록

## 참고
- API 계약 최신본: `docs/api-contract.md`
- 데이터 모델 최신본: `docs/data-model.md`
