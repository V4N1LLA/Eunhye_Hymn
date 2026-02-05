# 유스케이스: 관리자 에셋 프리사인 업로드

## 목표
- 관리자 권한으로 PDF 및 음원 파일을 스토리지에 직접 업로드한다.
- API는 프리사인 URL만 발급하고 파일 바이트는 중계하지 않는다.

## 접근 조건
- 관리자(ADMIN) 권한 필요.

## 처리 흐름
1. 관리자 요청으로 프리사인 URL을 발급한다.
2. 클라이언트가 스토리지에 직접 PUT 업로드한다.
3. 업로드 완료 후 확인 API로 메타데이터를 저장한다.

## 상세 흐름
### 1) 프리사인 발급
- `POST /admin/assets/presign`
- hymnId, type, part, filename, contentType을 전달한다.
- 응답으로 uploadUrl, publicUrl, objectKey를 받는다.
- part가 없으면 ALL로 처리한다.

### 2) 직접 업로드
- 응답의 uploadUrl로 파일을 PUT 업로드한다.
- 업로드 중에는 API를 거치지 않는다.

### 3) 업로드 확인
- `POST /admin/assets/confirm`
- publicUrl, objectKey, checksum, version을 포함해 저장한다.
- objectKey는 반드시 `hymns/`로 시작해야 한다.
- objectKey는 `hymns/{hymnId}/{type}/{part}/` 형식을 따라야 한다.
- 동일 hymnId/type/part 조합은 최신 업로드만 유지한다.

## 기대 결과
- PDF/음원 파일이 스토리지에 저장되고, API에는 메타데이터만 기록된다.
