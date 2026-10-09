# issue/notifyhub/device-token-dedup-created-at — 정답

태그: `silent-failure` · `data-semantics`

## 정답
<!-- 질문 1:1 대응 -->

1. **아무 단말도 받지 못한다.** 남은 토큰 t2는 앱이 지워진 태블릿 것이고, 지금 쓰는 휴대폰의 t1은 정리 작업이 지웠다.

2. **플랫폼은 앱이 지워진 단말의 토큰을 바로 무효로 돌려주지 않는다.** 수 일 뒤 무효 응답이 오기 시작했지만 그때는 이미 문의가 쌓였다.

3. **없었다.** 토큰이 한 개뿐이라 정리 대상이 아니었다. 단말을 여러 대 쓰다 재설치한 고객만 걸렸다 — 약 2만 명 중 3.4%.

4. **고객의 현재 앱 토큰과 서버에 남은 토큰을 비교한다.** 앱 진단 화면의 토큰이 서버에 없으면 정리 로직 쪽이다.

## 문제 구조 (추상화 코드)

### 변형 A — 정리 기준 시각을 잘못 고름
① 문제 코드
```sql
DELETE FROM device_token t
 WHERE t.id NOT IN (SELECT id FROM device_token x WHERE x.member_id = t.member_id
                    ORDER BY created_at DESC LIMIT 1);
```
② 고친 코드
```sql
DELETE FROM device_token t
 WHERE t.id NOT IN (SELECT id FROM device_token x WHERE x.member_id = t.member_id
                    ORDER BY updated_at DESC LIMIT 3);      -- 마지막 등록 기준, 단말 3개 유지
```
무엇이 깨졌나: "최신"을 행 생성 시각으로 정의했다 — 토큰은 같은 행에서 갱신되는 값이었다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| updated_at 기준 + 3개 유지 | 갱신 시각 신뢰 | 작음 | 다중 단말 과다 발송 | 기본 |
| 무효 응답 받을 때만 삭제 | 플랫폼 응답 처리 | 작음 | 쓰레기 토큰 누적 | 함께 적용 |

**결론**: 토큰 정리는 마지막 등록 시각 기준으로 하고, 삭제는 플랫폼의 무효 응답을 주 신호로 삼는다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
