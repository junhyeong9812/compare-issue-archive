# issue/devbind/biometric-key-invalidated — 정답

태그: `platform` · `error-handling`

## 정답
<!-- 질문 1:1 대응 -->

1. **영구 무효가 된다.** 그 키로는 서명할 수 없다.

2. **앱이 서명 실패를 처리하지 않고 빈 서명을 보냈다.** 서버는 이를 미등록 기기로 분류했다.

3. **다른 사람 지문을 추가해 로그인할 수 있게 된다.** 끄지 않는다.

4. **앱 삭제 후 재설치를 안내했다.** 그러면 기기 바인딩이 또 늘었다.

5. **`KeyPermanentlyInvalidatedException`을 잡아 생체 재등록 화면으로 보내고, PIN 확인 후 새 키를 만든다.** 서버는 서명 없는 요청을 별도 오류 코드로 돌려준다.

## 문제 구조 (추상화 코드)

### 변형 A — 예외 미처리
① 문제 코드
```kotlin
val sig = try { sign(challenge) } catch (e: Exception) { ByteArray(0) }
api.bioLogin(sig)
```
② 고친 코드
```kotlin
val sig = try { sign(challenge) } catch (e: KeyPermanentlyInvalidatedException) {
    return navigate(ReEnrollBiometric(requirePin = true))
}
api.bioLogin(sig)
```
무엇이 깨졌나: 플랫폼의 의도된 보안 동작을 오류로만 뭉뚱그려 사용자 안내가 틀어졌다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 무효화 감지 후 재등록 | 앱 수정 | 작음 | 구버전 앱 | 기본 |
| 서버 오류 코드 분리 | 서버 수정 | 작음 | 없음 | 함께 |
| 무효화 설정 끄기 | 앱 수정 | 작음 | 보안 약화 | 선택하지 않은 방법 |

**결론**: 보안 동작이 만드는 실패는 별도 경로로 안내한다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
