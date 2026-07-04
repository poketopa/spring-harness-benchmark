package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    protected ReservationTime() {
    }

    public ReservationTime(LocalTime startAt) {
        this(null, startAt);
    }

    public ReservationTime(Store store, LocalTime startAt) {
        validateStartAt(startAt);
        this.store = store;
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

    public Store getStore() {
        return store;
    }

    public boolean belongsTo(Store targetStore) {
        return store != null && store.hasSameId(targetStore);
    }
}
