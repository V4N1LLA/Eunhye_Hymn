# Current Project Update (2026-02-20)

- 작성일: 2026-02-20
- 기준 브랜치: `develop`
- 기준 커밋: `6a00b68`

## 1) 현재 상태 요약

프로젝트는 기능 구현 완료 후 운영 안정화/검증 고도화 단계다.

- API
  - 인증: Kakao + ID/PW + JWT 회전
  - 검증: 초대코드 + SMS + 탈퇴
  - 개인화: 프로필/개인정보 변경 요청/즐겨찾기/메모/히스토리
  - 운영: 감사 로그 + 동기/비동기 CSV export + 운영 지표
  - AI: `POST /ai/hymn-recommendations` (Gemini Flash-Lite)
- Admin
  - 찬양/에셋/사용자/초대코드/감사로그 관리
  - 개인정보 변경 요청 승인/반려
  - AI 추천 화면 추가
- Mobile
  - 인증 플로우: `Login -> InviteCode -> Phone -> SMS -> Home`
  - 찬양 탐색/상세/AI 추천/개인화/오프라인 캐시

## 2) 운영/배포 관점

- 로컬 검증: Go
- 스테이징 운영 점검: Conditional Go
  - 자동 배포/게이트/리허설 스크립트는 준비 완료
  - 수동 스모크 및 운영 지표 정례화는 계속 필요
- 스토어 배포: readiness workflow 구성 완료(실배포는 운영 승인 후)

## 3) 우선순위 작업

### P0
1. 스테이징 수동 스모크 결과 누적 자동화
2. 인증/AI 호출 실패율 운영 알림 기준 확정
3. 운영 시크릿 정기 로테이션 점검

### P1
1. Mobile store release 리허설(Android/iOS) 정례화
2. AI 추천 품질 피드백 루프(추천 사유 품질/응답 지연/비용) 보강

## 4) 빠른 명령

```powershell
.\scripts\local-bootstrap.ps1
.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554
.\scripts\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner <operator> -AutoLogin -WaitForCompletion
```
