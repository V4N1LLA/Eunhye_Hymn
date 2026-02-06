# 안정화 작업 이력

## 날짜
- 2026-02-06

## 변경 요약
1. 충돌(AA) 3건을 정리하고 테스트 초기화 로직을 보강했다.
2. `apps/api/.env.example`와 개발 문서 내용을 일치시켰다.
3. API base path를 `/api/v1`로 통일했다.
4. Windows 환경에서 `JAVA_HOME` 경로에 공백이 있을 때 `gradlew.bat`가 실패하던 문제를 수정했다.
5. `AdminHymnApiTest`의 목록 검증 취약 로직을 수정했고 전체 테스트 통과를 확인했다.
6. GitHub Actions API CI에서 시스템 `gradle` 직접 호출을 제거하고 wrapper(`./gradlew`)만 사용하도록 통일했다.

## 커밋 해시 목록
- `4edde28` 정리: 충돌 상태 파일 정리 및 테스트 초기화 보강
- `dd8d208` docs: API 로컬 실행 문서와 환경변수 예제 정합화
- `0e5868a` fix(config): API base path를 /api/v1로 통일
- `87735b7` fix(build): Windows JAVA_HOME 공백 경로에서 gradlew.bat 실행 오류 수정
- `4250456` fix(test): AdminHymnApiTest 목록 조회 실패 원인 수정
- `01c767c` ci: Gradle 실행 경로를 wrapper 기반으로 통일

## 검증 결과 (단건/전체/CI)
- 단건: `.\gradlew.bat test --no-daemon --tests "*AdminHymnApiTest*"` 실행 성공
- 전체: `.\gradlew.bat test --no-daemon --stacktrace` 실행 성공
- CI: `.github/workflows/api-ci.yml`에서 `run: gradle ...` 0건, `run: ./gradlew ...` 1건 확인

## 남은 TODO
- 없음 (현재 작업 범위 기준)
