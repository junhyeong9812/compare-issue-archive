# issue/mailrelay/dkim-selector-rotate-before-dns — 정답

태그: `deploy-order` · `auth`

## 정답
<!-- 질문 1:1 대응 -->

1. **`dkim=fail`(키 없음)이다.** 셀렉터 `s2026` 레코드가 아직 없었다.

2. **이미 발송돼 수신 큐나 전달 중인 메일이 옛 셀렉터로 서명돼 있다.** 지연 전달·재검증(포워딩) 시 실패한다.

3. **보고서에는 실패가 남지만 배달에는 큰 영향이 없다.** 이 도메인은 `p=quarantine`이어서 일부가 스팸함으로 갔다.

## 문제 구조 (추상화 코드)

### 변형 A — 서명 전환과 DNS 등록을 같은 시점에 함
① 문제 코드
```sh
mailrelay dkim set --selector s2026 --key s2026.pem
dns-request add s2026._domainkey TXT "$(cat s2026.pub.txt)"
dns-request del s2025._domainkey
```
② 고친 코드
```sh
dns-request add s2026._domainkey TXT "$(cat s2026.pub.txt)"
until dig +short TXT s2026._domainkey.hanbitpay.co.kr @8.8.8.8 | grep -q p=; do sleep 60; done
mailrelay dkim set --selector s2026 --key s2026.pem
at now + 7 days <<< "dns-request del s2025._domainkey"
```
무엇이 깨졌나: 의존 순서(공개키 게시 → 서명)를 한 작업 안에서 동시에 처리했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 순서 고정 스크립트 | DNS 조회 가능 | 작업 시간 증가 | 없음 | 기본 |
| 두 셀렉터 병행 운용 | 릴레이가 셀렉터 선택 지원 | 설정 복잡 | 혼동 | 정기 교체 |

**결론**: 공개키 게시를 외부에서 확인한 뒤 서명을 바꾸고, 옛 키는 유예 기간 뒤에 지운다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
