# issue/mailrelay/spf-lookup-limit-permerror — 정답

태그: `environment-drift` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **`permerror`다.** 펼친 include가 13회 조회를 요구한다. X는 permerror를 fail로 처리해 스팸함으로 보냈다.

2. **permerror 처리 정책이 수신 서버마다 다르다.** 관대한 서버는 DKIM pass로 보완하거나 permerror를 중립으로 둔다. 우리 고객의 약 18%가 쓰는 메일 서비스가 엄격했다.

3. **DMARC 정렬이 SPF에만 맞춰져 있었다.** DKIM 서명 도메인이 릴레이 기본 도메인이라 From 도메인과 정렬되지 않았다 — 그래서 SPF 실패가 그대로 DMARC 실패가 됐다.

4. **수신 측 헤더의 `Authentication-Results`를 본다.** `spf=permerror`가 보이면 이 사건이다. 릴레이 로그에 `250`이 아니면 전송 단계 문제다.

## 문제 구조 (추상화 코드)

### 변형 A — 조회 수 확인 없이 include 추가
① 문제 코드
```sh
# DNS 변경 요청: 설문 벤더 발송 허용
TXT hanbitpay.co.kr "v=spf1 ... include:survey.vendor ~all"
```
② 고친 코드
```sh
# 1) 변경 전 조회 수 계산 (CI 검사)
spf-count hanbitpay.co.kr --with "include:survey.vendor" | awk '$1>9{exit 1}'
# 2) 벤더 발송은 하위 도메인으로 분리
TXT survey.hanbitpay.co.kr "v=spf1 include:survey.vendor -all"
```
무엇이 깨졌나: DNS 레코드를 문자열로만 검토했다 — 수신 서버가 실제로 평가하는 조회 수는 펼쳐야 보인다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 하위 도메인 분리 | 벤더가 발신 도메인 설정 지원 | 도메인 관리 | 벤더 미지원 시 불가 | 기본 |
| SPF 평탄화(IP 나열) | 벤더 IP 고정 | 갱신 자동화 필요 | 벤더 IP 변경 시 실패 | 벤더가 하위 도메인 미지원 |
| DKIM 정렬로 보완 | 서명 도메인 = From | 키 관리 | SPF 실패 자체는 남음 | 함께 적용 |

**결론**: SPF 변경은 조회 수 계산을 CI 검사로 묶고, 외부 벤더 발송은 하위 도메인으로 떼어 낸다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
