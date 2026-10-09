# issue/mailrelay/tls-floor-raise-deferred-bounce — 정답

태그: `environment-drift` · `compatibility`

## 정답
<!-- 질문 1:1 대응 -->

1. **5일 뒤 반송 메일로 드러난다.** 그 전까지는 `deferred` 큐에 있을 뿐 오류로 올라오지 않는다.

2. **주요 메일 서비스만 시험했다.** 지난 30일 수신 도메인별 TLS 버전 분포를 미리 뽑았다면 TLS 1.0 도메인 7곳이 보였다.

3. **기회적 TLS에서는 협상 실패 시 평문으로 보낸다** — 연결은 되지만 보안 점검 의도와 어긋난다. 그래서 도메인별 예외로 좁혔다.

## 문제 구조 (추상화 코드)

### 변형 A — 전역 TLS 정책 일괄 변경
① 문제 코드
```sh
postconf -e "smtp_tls_security_level=encrypt" "smtp_tls_mandatory_protocols=>=TLSv1.2"
postfix reload
```
② 고친 코드
```sh
mailrelay tls-report --days 30 | awk '$2<"TLSv1.2"'        # 영향 도메인 먼저 확인
postconf -e "smtp_tls_policy_maps=hash:/etc/postfix/tls_policy"   # 예외 도메인만 may
postconf -e "smtp_tls_security_level=encrypt" "smtp_tls_mandatory_protocols=>=TLSv1.2"
postfix reload; mailrelay alert set --deferred-age 2h
```
무엇이 깨졌나: 상대 시스템 호환성을 보지 않고 전역 정책을 바꿨고, 일시 실패 큐의 장기 체류를 경보하지 않았다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 도메인별 정책 맵 | 예외 목록 관리 | 작음 | 목록 낡음 | 기본 |
| deferred 체류 경보 | 경보 채널 | 작음 | 소음 | 항상 |
| 일괄 상향 후 대응 | 없음 | 장애 비용 | 5일 지연 | 선택하지 않음 |

**결론**: 전역 TLS 정책 변경 전에 수신 도메인 분포를 보고, deferred 체류 시간에 경보를 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
