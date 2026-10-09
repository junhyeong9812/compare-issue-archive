# issue/ssogw/saml-clock-skew-login-loop — 정답

태그: `environment-drift` · `fail-closed`

## 정답
<!-- 질문 1:1 대응 -->

1. **거절되고 로그인 화면으로 돌아간다.** 노드가 보기에 어서션의 `NotBefore`는 95초 뒤의 미래다. 허용 오차 60초를 넘으므로 "아직 유효하지 않은 어서션"으로 거절한다.\
`ssogw`는 사용자에게 오류 페이지 대신 로그인 페이지로 다시 보내므로, 사용자는 "튕겼다"고만 느낀다.
   > **NotBefore / NotOnOrAfter** — SAML 어서션이 유효한 시간 구간의 시작·끝. 검증하는 쪽의 시계로 비교한다.

2. **LB가 요청을 노드 4대에 나눠 보내기 때문이다.** 재시도가 다른 노드로 가면 성공한다.\
세션 고정(sticky)은 로그인 이후에만 걸려서, 로그인 콜백은 매번 무작위 노드로 갔다 — 대략 4번 중 1번 실패했다.

3. **노드가 느리면 `NotBefore`에, 노드가 빠르면 `NotOnOrAfter`에 걸린다.**\
빠른 쪽은 어서션 유효 기간(5분)을 넘을 만큼 빨라야 걸리므로 훨씬 늦게 드러난다. 이번 사건은 느린 쪽이었다.

4. **동기화 대상 서버 목록이 폐기된 사내 NTP 주소였다.**\
데몬은 떠 있었지만 연결 가능한 서버가 없어 동기화를 하지 못했고, 하드웨어 시계가 몇 주 동안 조금씩 밀렸다.\
`chronyc tracking`의 `Reference ID`가 비어 있고 오프셋이 수십 초였다 — 서비스 상태 점검은 "active"만 봤다.

5. **증상만 가리기 때문이다.** 시계가 계속 밀리면 언젠가 300초도 넘는다.\
또 허용 오차는 탈취된 어서션을 재사용할 수 있는 창이기도 해서, 늘리면 보안이 약해진다.

6. **실패 로그에 노드 이름, 노드 현재 시각, 어서션 `NotBefore`, 둘의 차이를 남긴다.**\
이번에는 이 정보가 없어 IdP 측 로그와 LB 로그를 시각으로 맞춰 보며 반나절을 썼다. 노드별 시각 오프셋 메트릭과 경보(5초 초과)도 함께 둔다.

## 문제 구조 (추상화 코드)

### 변형 A — 노드 시계로 유효 구간 검사
① 문제 코드
```java
Instant now = Instant.now();                         // 노드 시계 (95초 늦음)
if (now.plus(SKEW).isBefore(a.notBefore())) return redirectToLogin();   // 이유 기록 없음
```
② 고친 코드
```java
Instant now = Instant.now();
if (now.plus(SKEW).isBefore(a.notBefore())) {
    log.warn("saml not-yet-valid node={} now={} notBefore={} diff={}s",
             nodeName, now, a.notBefore(), Duration.between(now, a.notBefore()).toSeconds());
    return redirectToLogin();
}
```
무엇이 깨졌나: 검증은 올바르게 거절했지만(fail-closed), 이유가 남지 않아 노드 시계 문제를 찾는 데 오래 걸렸다.

### 변형 B — 시간 동기화를 데몬 상태로만 점검
① 문제 코드
```sh
systemctl is-active chronyd     # active → 정상으로 판단
```
② 고친 코드
```sh
off=$(chronyc tracking | awk '/System time/ {print $4}')
awk -v o="$off" 'BEGIN { exit (o < 5 && o > -5) ? 0 : 1 }' || alert "clock offset ${off}s"
```
무엇이 깨졌나: 프로세스가 살아 있음을 기능이 동작함으로 오인했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
