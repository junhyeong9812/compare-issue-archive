# issue/lbctl/idle-timeout-longer-than-upstream — 정답

태그: `timeout` · `race`

## 정답
<!-- 질문 1:1 대응 -->

1. **백엔드가 이미 닫은 커넥션이라 RST를 받고 LB가 502를 낸다.**

2. **낮에는 커넥션이 5초 안에 다시 쓰여 닫히지 않았다.** 새벽엔 유휴가 길어 백엔드가 먼저 닫았다.

3. **하지 않는다.** 요청을 쓴 뒤의 실패라 멱등 여부를 몰라 그대로 502를 준다.

4. **백엔드 keep-alive(125초) > LB 유휴(120초).** Node.js는 `headersTimeout`도 그보다 크게 둔다.

## 문제 구조 (추상화 코드)

### 변형 A — 백엔드 keep-alive가 LB보다 짧음
① 문제 코드
```javascript
const server = http.createServer(app);   // keepAliveTimeout 기본 5000ms
```
② 고친 코드
```javascript
const server = http.createServer(app);
server.keepAliveTimeout = 125_000;      // LB idle 120s 보다 길게
server.headersTimeout = 126_000;
```
무엇이 깨졌나: 커넥션을 닫는 주체가 LB여야 한다는 전제가 설정에 반영되지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 백엔드 keep-alive 연장 | 앱 설정 | 작음 | 유휴 커넥션 증가 | 기본 |
| LB 유휴 타임아웃 단축 | lbctl 권한 | 작음 | 다른 백엔드 영향 | 백엔드 수정 불가 시 |
| LB 재시도 켜기 | 멱등 요청 | 작음 | POST 중복 | 선택하지 않은 방법 |

**결론**: 닫는 주체를 LB로 고정한다 — 백엔드 keep-alive를 더 길게.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
