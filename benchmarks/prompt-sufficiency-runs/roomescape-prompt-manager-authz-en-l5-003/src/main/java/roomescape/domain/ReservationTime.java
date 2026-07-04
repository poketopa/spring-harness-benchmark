package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalTime;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
public class ReservationTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalTime startAt;

    protected ReservationTime() {
    }

    public ReservationTime(LocalTime startAt) {
        validateStartAt(startAt);
        this.startAt = startAt;
    }

    private void validateStartAt(LocalTime startAt) {
        if (startAt == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 시간은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public LocalTime getStartAt() {
        return startAt;
    }
}
