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
        validateStartAt(startAt);
        this.startAt = startAt;
    }

    public ReservationTime(LocalTime startAt, Store store) {
        validateStartAt(startAt);
        validateStore(store);
        this.startAt = startAt;
        this.store = store;
    }

    private void validateStartAt(LocalTime startAt) {
        if (startAt == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 시간은 비어 있을 수 없습니다.");
        }
    }

    private void validateStore(Store store) {
        if (store == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 시간 매장은 비어 있을 수 없습니다.");
        }
    }

    public boolean belongsTo(Store targetStore) {
        return store != null && store.isSameAs(targetStore);
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
}
