# issue/dnsops/negative-cache-before-record-created — 정답

태그: `stale-read` · `dns`

## 정답
<!-- 질문 1:1 대응 -->

1. **최대 1시간 뒤(SOA minimum 3600초)에 찾는다.**

2. **그 세 대가 같은 캐시 리졸버 노드를 썼고, 그 노드에만 '없음'이 캐시됐다.**

3. **캐시된 것은 새 레코드가 아니라 '없음' 응답이고, 그 수명은 SOA minimum이 정한다.**

4. **레코드 생성 → 모든 리졸버 노드에서 `dnsops flush` → 조회 확인** 순서. 내부 존 SOA minimum은 300초로 낮췄다.

## 문제 구조 (추상화 코드)

### 변형 A — 레코드 생성 전 연결 시험
① 문제 코드
```sh
dig api.cardx.partner.hanbit.internal   # 등록 전 확인 → NXDOMAIN 캐시
dnsops record add api.cardx.partner A 10.40.2.15 --ttl 60
```
② 고친 코드
```sh
dnsops record add api.cardx.partner A 10.40.2.15 --ttl 60
dnsops flush api.cardx.partner.hanbit.internal --all-resolvers
dnsops check api.cardx.partner.hanbit.internal --all-resolvers
```
무엇이 깨졌나: '없음' 확인도 캐시를 만든다는 점을 몰랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 생성 후 flush | dnsops 권한 | 작음 | flush 누락 | 기본 |
| SOA minimum 하향 | 존 설정 | 작음 | 리졸버 부하 소폭 증가 | 함께 |
| IP 직접 지정 | 없음 | 작음 | IP 변경 시 장애 | 선택하지 않은 방법 |

**결론**: 부정 캐시 시간을 줄이고, 오픈 절차를 생성→flush→확인으로 고정한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
