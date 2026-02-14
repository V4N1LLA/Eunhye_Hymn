# Eunhye Hymn 운영 Runbook (Staging)

## 1. 목적
- 스테이징 배포/장애 대응 절차를 표준화한다.
- 담당자가 바뀌어도 동일한 순서로 복구 가능하도록 한다.

## 2. 전제 조건
- GitHub 저장소 접근 권한
- AWS 접근 권한 (ECR/EC2/CloudWatch)
- EC2 SSH 접속 가능

## 3. 배포 체크리스트
- [ ] `develop` 최신 반영
- [ ] DB 마이그레이션 변경 유무 확인
- [ ] 환경 변수 파일 최신화 (`DB_*`, `JWT_*`, `INVITE_CODE`, `S3_*`)
- [ ] 롤백 기준 버전(이전 이미지 태그) 확인
- [ ] 사전 점검 스크립트 통과
  - `.\scripts\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn [-AwsProfile <profile>]`
- [ ] GitHub Actions 필수 Secrets 등록 확인
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`

## 4. 배포 절차
1. `develop`에 변경 머지
2. GitHub Actions 배포 워크플로 실행
3. EC2에서 컨테이너 상태 확인
   - `docker ps`
   - `docker logs <api_container>`
4. 헬스체크
   - `curl http://<EC2_HOST>/api/v1/ping`

## 5. 장애 대응
### 5.1 증상 확인
- API 5xx 지속
- 컨테이너 재시작 반복
- `/api/v1/ping` 실패

### 5.2 1차 대응
1. 최근 배포 커밋/이미지 태그 확인
2. 이전 안정 태그로 롤백
3. 헬스체크 재확인
4. 사용자 공지 필요 여부 판단

### 5.3 사후 조치
- 원인 분석(RCA) 문서화
- 재발 방지 항목 등록
- 관련 문서 업데이트
  - `docs/runbook.md`
  - `docs/changelog-dev.md`

## 6. 민감 정보 운영 원칙
- 민감 정보는 Git에 커밋하지 않는다.
- `.env`는 로컬 전용으로 사용하고, 실제 값은 Secrets Manager 또는 GitHub Actions Secrets로 관리한다.
- 로그/PR 본문에 토큰, 비밀번호, 키 전체값을 남기지 않는다.

## 7. 정기 점검 (권장)
- 주 1회 스테이징 배포 리허설
- 월 1회 시크릿 교체 상태 점검
- 월 1회 롤백 시나리오 점검
