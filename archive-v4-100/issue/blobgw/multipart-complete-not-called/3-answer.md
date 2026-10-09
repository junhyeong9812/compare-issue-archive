# issue/blobgw/multipart-complete-not-called — 정답

태그: `silent-failure` · `partial-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **보이지 않는다.** 파트만 있고 객체는 없다.

2. **업로더가 마지막 파트 응답을 받으면 진행률 100%로 성공을 기록했다.** `complete`는 그 뒤였다.

3. **`list-multipart-uploads`에만 보이고 파트 용량만큼 과금된다.** 7일 뒤 정리 규칙을 넣었다.

4. **`complete` 성공 + `head`로 크기 확인 후 성공 기록.** 파트너 통지는 그다음.

## 문제 구조 (추상화 코드)

### 변형 A — 파트 전송 완료를 업로드 완료로 기록
① 문제 코드
```python
for i, part in enumerate(parts, 1):
    s3.upload_part(..., PartNumber=i, Body=part)
job.ok()                          # complete 전
s3.complete_multipart_upload(...)
```
② 고친 코드
```python
etags = [s3.upload_part(..., PartNumber=i, Body=p)["ETag"] for i, p in enumerate(parts, 1)]
s3.complete_multipart_upload(..., MultipartUpload={"Parts": [...]})
if s3.head_object(Bucket=b, Key=k)["ContentLength"] != size:
    raise UploadIncomplete(k)
job.ok()
```
무엇이 깨졌나: 다단계 프로토콜의 중간 응답을 최종 결과로 기록했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| complete+head 후 성공 기록 | 코드 수정 | 작음 | 없음 | 기본 |
| 미완료 업로드 정리 규칙 | 수명 주기 설정 | 작음 | 진행 중 업로드 삭제(기간 짧으면) | 함께 |
| 파트너 쪽 파일 존재 확인 회신 | 파트너 협조 | 중간 | 회신 지연 | 보조 |

**결론**: 성공 기록을 `complete` 확인 뒤로 옮기고, 축출되지 않도록 업로더에 PDB를 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
