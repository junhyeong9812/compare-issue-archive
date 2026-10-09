# issue/blobgw/presigned-url-temp-credential-expiry — 정답

태그: `auth` · `config`

## 정답
<!-- 질문 1:1 대응 -->

1. **`ExpiredToken`으로 거부된다.**

2. **3월에 장기 접근 키를 없애고 파드 역할로 바꿨다.**

3. **장기 키로 서명해야 하지만, 보안 점검 취지에 맞지 않는다.**

4. **가맹점 어드민의 다운로드 엔드포인트가 로그인 확인 후 5분짜리 URL로 리다이렉트한다.** 메일엔 그 엔드포인트 주소를 보낸다.

## 문제 구조 (추상화 코드)

### 변형 A — 메일에 장기 서명 URL 직접 삽입
① 문제 코드
```java
URL url = presigner.presign(get(bucket, key), Duration.ofDays(7));   // 임시 자격 증명
mail.send(merchant, url);
```
② 고친 코드
```java
mail.send(merchant, "https://admin.hanbitpay.example/statements/" + id);
// GET /statements/{id}: 권한 확인 → presign(5분) → 302
```
무엇이 깨졌나: 서명 URL의 수명을 URL 파라미터만으로 정해진다고 봤다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 리다이렉트 엔드포인트 | 로그인 필요 | 작음 | 가맹점 로그인 불편 | 기본 |
| 장기 키 서명 | 키 보관 | 작음 | 키 유출 위험 | 선택하지 않은 방법 |
| 파일 첨부 | 메일 크기 한도 | 작음 | 개인정보 메일 저장 | 작은 파일 |

**결론**: 링크는 앱 엔드포인트로 보내고 서명 URL은 클릭 시 짧게 발급한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
