# 3단계: 예약 대기 기능

## 목표

- 예약 대기 요청 기능을 구현한다.
- 예약 대기 취소 기능을 구현한다.
- 내 예약 목록에 예약 대기를 함께 포함한다.
- 중복 예약을 방지한다.
- 내 예약 대기가 몇 번째인지 표시한다.

## 학습 포인트

이 단계에서는 `Waiting` 도메인이 `Member`, `Theme`, `ReservationTime`을 참조하면서 N+1과 JPQL을 본격적으로 다룬다.

## 들어가기 전 자기 진단

- 예약 대기 도메인을 별도 엔티티로 만들 것인가?
- 기존 `Reservation`에 `status` 컬럼만 추가할 것인가?
- 첫 직감의 근거는 무엇인가?

## 요구사항

### API

```http
POST /waitings
DELETE /waitings/{id}
GET /reservations-mine
```

- `POST /waitings`: 예약 대기를 요청한다.
- `DELETE /waitings/{id}`: 예약 대기를 취소한다.
- `GET /reservations-mine`: 예약 목록과 예약 대기 목록을 함께 응답한다.
- 예약 대기 항목의 상태는 `N번째 예약대기` 형태로 표시한다.

### 도메인 규칙

- 같은 테마, 날짜, 시간에 중복 예약을 방지한다.
- 심화 요구사항으로 내 예약 대기가 몇 번째인지 표시한다.

## 3-1. N+1과 Fetch Join 비교

### 관찰 시나리오

- `GET /reservations-mine`에서 본인 예약 N개와 본인 대기 M개를 조회한다.
- 각 항목의 `getTheme().getName()`, `getTime().getStartAt()`을 응답 DTO로 변환할 때 SQL이 몇 번 나가는지 확인한다.
- 같은 작업을 fetch join 또는 `@EntityGraph`로 묶었을 때 SQL이 어떻게 달라지는지 확인한다.
- join이 합쳐질 때 row 중복이 어떻게 처리되는지 확인한다.

### 기록할 것

1. 시도한 코드
2. 예측 SQL
3. 실제 SQL
4. 예측과 실제가 다른 이유

## 3-2. JPQL

N번째 대기 계산은 메서드 이름 쿼리로 풀리지 않으므로 JPQL을 사용한다.

```jpql
SELECT new ...WaitingWithRank(
    w,
    (SELECT COUNT(w2)
     FROM Waiting w2
     WHERE w2.theme = w.theme
       AND w2.date = w.date
       AND w2.time = w.time
       AND w2.id < w.id)
)
FROM Waiting w
WHERE w.memberId = :memberId
```

LMS 힌트를 그대로 사용하거나 직접 작성해 풀어도 된다.
