# 1단계: JPA 전환

## 목표

- `JdbcTemplate` 기반 Repository를 JPA Repository로 교체한다.
- 도메인 간 연관관계를 객체 그래프로 표현한다.
- 영속성 컨텍스트의 동작을 직접 관찰한다.

## 진행 메모

이 단계는 매핑, 연관관계, 영속성 컨텍스트가 함께 등장하므로 JPA 추가 미션 중 가장 분량이 크다.

## 들어가기 전 자기 진단

- 본인 코드의 Repository에서 가장 자주 등장하는 SQL 패턴은 무엇인가?
- 객체 참조로 옮겼을 때 더 자연스러워지는 곳은 어디인가?

## 1-1. 매핑 변환

다른 클래스에 의존하지 않는 클래스부터 시작한다. 예시는 `Theme`, `ReservationTime`이다.

### 요구사항

- `build.gradle`에서 `spring-boot-starter-jdbc`를 `spring-boot-starter-data-jpa`로 대체한다.
- 엔티티에 `@Entity`, `@Id`, `@GeneratedValue(strategy = IDENTITY)`를 부여한다.
- `JpaRepository<T, Long>` 인터페이스를 작성한다.
- 기존 `JdbcTemplate` 기반 Repository를 제거한다.
- `KeyHolder`, `SimpleJdbcInsert` 같은 `JdbcTemplate` 잔재를 제거한다.

### 권장 설정

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.ddl-auto=create-drop
spring.jpa.defer-datasource-initialization=true
```

### 비교 관찰 포인트

- 시작 시 발행되는 DDL의 차이
- 재시작 시 데이터 보존 여부
- 컬럼명과 타입을 엔티티만으로 제어할 수 있는지 여부

## 확인 과제

- 예약 생성 시 콘솔에 찍히는 `INSERT` SQL이 기존 방탈출 미션과 어떻게 같고 다른가?

## 1-2. 연관관계 매핑

다른 클래스에 의존하는 클래스에 연관관계를 매핑한다. 예를 들어 `Reservation`은 `Member`, `Theme`, `ReservationTime`을 참조한다.

### 요구사항

- `@ManyToOne`과 `@JoinColumn(name = "..._id")`로 객체 참조를 표현한다.
- 단방향 연관관계로 시작한다.
- 양방향 연관관계는 필요한 이유가 생겼을 때만 추가한다.
- 양방향을 시도한다면 연관관계의 주인을 명시한다.
- 양방향을 시도한다면 무한 직렬화 가능성을 검토한다.
- `cascade`, `orphanRemoval`은 필요해질 때까지 적용하지 않는다.
- `cascade`, `orphanRemoval`을 적용한다면 PR 본문에 근거를 적는다.
- 양방향 또는 cascade를 시도했다가 단방향 또는 제거로 후퇴하는 사이클을 의식적으로 한 번 경험하고 기록한다.
