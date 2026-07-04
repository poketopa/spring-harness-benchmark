package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class WaitingTest {

    @Test
    @DisplayName("현재 시각보다 이전 대기 대상인지 판단한다")
    void pastWaitingIsDetectedByNow() {
        Waiting waiting = waiting();

        boolean past = waiting.isPast(LocalDateTime.of(2030, 5, 1, 10, 1));

        assertThat(past).isTrue();
    }

    @Test
    @DisplayName("대기 소유 회원인지 판단한다")
    void ownedWaitingIsDetectedByMember() {
        Member member = member();
        Waiting waiting = new Waiting(
                member,
                theme(),
                time(),
                LocalDate.of(2030, 5, 1),
                createdAt()
        );

        assertThat(waiting.isOwnedBy(member)).isTrue();
    }

    @Test
    @DisplayName("대기 소유 회원이 아니면 false를 반환한다")
    void differentMemberIsNotWaitingOwner() {
        Waiting waiting = waiting();

        assertThat(waiting.isOwnedBy(new Member("샐리", "sally@example.com", "password"))).isFalse();
    }

    @Test
    @DisplayName("대기를 승인하면 같은 회원과 슬롯을 가진 예약을 만든다")
    void approveWaitingCreatesReservation() {
        Member member = member();
        Theme theme = theme();
        ReservationTime time = time();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Waiting waiting = new Waiting(member, theme, time, date, createdAt());

        Reservation reservation = waiting.approve();

        assertThat(reservation.getMember()).isEqualTo(member);
        assertThat(reservation.getTheme()).isEqualTo(theme);
        assertThat(reservation.getTime()).isEqualTo(time);
        assertThat(reservation.getDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("대기 회원, 테마, 시간, 날짜, 생성 시각은 비어 있을 수 없다")
    void nullWaitingFieldsAreRejected() {
        assertThatThrownBy(() -> new Waiting(null, theme(), time(), LocalDate.of(2030, 5, 1), createdAt()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Waiting(member(), null, time(), LocalDate.of(2030, 5, 1), createdAt()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Waiting(member(), theme(), null, LocalDate.of(2030, 5, 1), createdAt()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Waiting(member(), theme(), time(), null, createdAt()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Waiting(member(), theme(), time(), LocalDate.of(2030, 5, 1), null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private Waiting waiting() {
        return new Waiting(member(), theme(), time(), LocalDate.of(2030, 5, 1), createdAt());
    }

    private Member member() {
        return new Member("코니", "cony@example.com", "password");
    }

    private Theme theme() {
        return new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
    }

    private ReservationTime time() {
        return new ReservationTime(LocalTime.of(10, 0));
    }

    private LocalDateTime createdAt() {
        return LocalDateTime.of(2030, 1, 1, 10, 0);
    }
}
