# issue/remitx/mtls-intermediate-ca-missing — 정답

태그: `tls` · `environment-drift`

## 정답
<!-- 질문 1:1 대응 -->

1. **구 이미지 서버의 OS 신뢰 저장소에 새 중간 CA가 없었다.**

2. **헬스 체크가 은행 연결을 보지 않았다.** 외부 의존 상태를 준비 상태 점검에 넣었다.

3. **은행을 사칭한 서버에 송금 요청을 보낼 수 있다.** 하지 않는다.

## 문제 구조 (추상화 코드)

① 문제 코드
```sh
# OS 기본 신뢰 저장소 사용
```
② 고친 코드
```sh
keytool -importcert -alias bank-int-2026 -file bank-int.pem -keystore app-truststore.p12
# 실행 옵션: -Djavax.net.ssl.trustStore=app-truststore.p12
```
무엇이 깨졌나: 외부 신뢰 체인이 서버 이미지마다 달랐다.

## 방안 비교

| 방안 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|
| 앱 전용 신뢰 저장소 | 작음 | 갱신 관리 | 기본 |
| 검증 해제 | 없음 | 사칭 | 선택하지 않음 |

**결론**: 외부 상대의 신뢰 체인은 앱 설정으로 고정하고 이미지를 통일한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
