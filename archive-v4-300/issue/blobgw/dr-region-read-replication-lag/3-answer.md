# issue/blobgw/dr-region-read-replication-lag — 정답

태그: `stale-read` · `replication`

## 정답
<!-- 질문 1:1 대응 -->

1. **404다.** 몇 분 뒤 재시도하면 받는다.

2. **2023년 DR 훈련에서 파트너용 엔드포인트를 보조 리전으로 바꾸고 원복을 빠뜨렸다.**

3. **`head`의 `replication-status: COMPLETED`.**

4. **파트너 엔드포인트를 주 리전으로 되돌리고**, 보조 리전 읽기가 필요하면 복제 완료 확인 뒤 통지한다.

## 문제 구조 (추상화 코드)

### 변형 A — 업로드 직후 통지
① 문제 코드
```python
blob.put(key, data)
notify(partner, key)
```
② 고친 코드
```python
blob.put(key, data)
wait_until(lambda: blob.head(key)["replication-status"] == "COMPLETED", timeout=600)
notify(partner, key)
```
무엇이 깨졌나: 읽는 쪽 리전과 쓰는 쪽 리전이 다르다는 사실이 통지 로직에 없었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 읽기 리전 원복 | 파트너 설정 | 작음 | DR 시 다시 변경 | 기본 |
| 복제 완료 후 통지 | 상태 조회 | 작음 | 통지 지연 | 보조 리전 필수 시 |
| 파트너 재시도 안내 | 파트너 협조 | 작음 | 파트너마다 다름 | 선택하지 않은 방법 |

**결론**: DR 전환 항목에 원복 확인을 넣고, 통지는 읽는 리전 기준으로 한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
