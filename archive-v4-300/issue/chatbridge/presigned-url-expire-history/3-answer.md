# issue/chatbridge/presigned-url-expire-history — 정답

태그: `design` · `storage`

## 정답
<!-- 질문 1:1 대응 -->

1. **이미지가 깨진다(403).** URL이 만료됐다.

2. **상담 중에는 업로드 직후 보기 때문이다.**

3. **안 된다.** 영수증·신분증 사진이 있어 긴 URL은 유출 위험이 크다.

4. **분쟁 조정에서 고객 제출 증빙을 다시 봐야 했는데 열리지 않았다.** 객체는 저장소에 있어 키를 찾아 수동 복구했다.

5. **메시지에는 `attachment_key`, 화면은 열람 시 `GET /attachments/{key}`로 권한 확인 후 5분 URL 발급.** 기존 메시지의 URL에서 키를 추출해 이관했다.

## 문제 구조 (추상화 코드)

### 변형 A — URL 저장
① 문제 코드
```java
String url = storage.presign(key, Duration.ofMinutes(15));
message.setBody(url);
```
② 고친 코드
```java
message.setAttachmentKey(key);
// 열람 시
authz.check(viewer, message);
return storage.presign(message.attachmentKey(), Duration.ofMinutes(5));
```
무엇이 깨졌나: 일시적인 접근 수단(서명 URL)을 영구 데이터로 저장했다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 키 저장 + 열람 시 발급 | API 추가 | 작음 | 없음 | 기본 |
| 긴 서명 유효 시간 | 설정 | 작음 | 유출 위험 | 선택하지 않은 방법 |
| 프록시 경유 다운로드 | 서버 대역폭 | 중간 | 부하 | 민감 첨부 |

**결론**: 저장할 것은 자원의 식별자이고, 접근 수단은 열람 시점에 만든다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
