# issue/tokensvc/refresh-rotation-race — 정답

태그: `concurrency` · `session`

## 정답
<!-- 질문 1:1 대응 -->

1. **두 번째 요청이 재사용으로 판정돼 R2까지 전부 폐기되고 로그아웃된다.**

2. **탈취된 리프레시 토큰을 공격자와 정상 사용자가 함께 쓰면 둘 중 하나가 재사용을 일으킨다.** 그때 전부 폐기해 피해를 막는다.

3. **간격과 무관하게 같은 R1이면 재사용이다.** 동시 요청이 대개 수십 ms 안이었다.

4. **액세스 토큰(30분)이 만료된 상태에서 첫 화면이 API 3~4개를 동시에 불렀기 때문이다.**

5. **앱**: 갱신을 단일 비행(single-flight)으로 묶는다.\
**서버**: 직전 회전 후 10초 이내 재사용은 같은 R2를 반환. 10초 밖은 기존대로 폐기.

## 문제 구조 (추상화 코드)

### 변형 A — 요청마다 갱신
① 문제 코드
```ts
async function call(req) {
  let r = await fetch(req);
  if (r.status === 401) { await refresh(); r = await fetch(req); }
  return r;
}
```
② 고친 코드
```ts
let inflight: Promise<void> | null = null;
async function call(req) {
  let r = await fetch(req);
  if (r.status === 401) {
    inflight ??= refresh().finally(() => (inflight = null));
    await inflight; r = await fetch(req);
  }
  return r;
}
```
무엇이 깨졌나: 보안 규칙(재사용 탐지)이 정상 클라이언트의 동시성을 공격과 구분하지 못했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 앱 single-flight | 앱 배포 | 작음 | 구버전 앱 | 기본 |
| 서버 10초 유예 | 서버 수정 | 작음 | 유예 내 탈취 | 함께 |
| 회전 끄기 | 정책 | 작음 | 보안 약화 | 선택하지 않은 방법 |

**결론**: 동시 갱신은 클라이언트에서 묶고, 서버는 짧은 유예로 경합을 흡수한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
