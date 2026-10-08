# 하객 명단 발음 안내가 일부 이름에서 틀리게 나옵니다

하객 명단 인쇄 기능은 이탈리아식 이름 옆에 영어 화자가 읽기 쉬운 발음 표기를 붙여 줍니다
(`romanizer.py`의 `romanize` / `guide`).

## 기대한 것
- `Cesare` → `chesare`, `Carla` → `karla` 처럼 대부분의 이름은 잘 나옵니다.
- `Marc`는 `mark`로, `Isaac Bianchi`는 `Isaak Bianki`로 나와야 합니다.

## 실제로 일어난 것
- `Marc`가 `march`로, `Isaac Bianchi`가 `Isaach Bianki`로 인쇄되었습니다.
- 사회자가 그대로 읽어서 하객분들이 당황하셨습니다. 다른 이름에도 비슷한 문제가 있는지는 확인하지 못했습니다.

## 재현
```
bash test.sh
```

test.sh가 통과하도록 고쳐 주세요. 테스트 파일은 수정하지 마세요.
