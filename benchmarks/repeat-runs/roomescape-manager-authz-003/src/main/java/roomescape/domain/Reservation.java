package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_reservation_theme_time_date",
                columnNames = {"theme_id", "time_id", "date"}
        )
})
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id")
    private ReservationTime time;

    @Column(nullable = false)
    private LocalDate date;

    protected Reservation() {
    }

    public Reservation(Member member, Theme theme, ReservationTime time, LocalDate date) {
        validateRequired(member, theme, time, date);
        this.member = member;
        this.theme = theme;
        this.time = time;
        this.date = date;
    }

    private void validateRequired(Member member, Theme theme, ReservationTime time, LocalDate date) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 회원은 비어 있을 수 없습니다.");
        }
        if (theme == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 테마는 비어 있을 수 없습니다.");
        }
        validateSchedule(date, time);
    }

    private void validateSchedule(LocalDate date, ReservationTime time) {
        if (time == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 시간은 비어 있을 수 없습니다.");
        }
        if (date == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "예약 날짜는 비어 있을 수 없습니다.");
        }
    }

    public boolean isPast(LocalDateTime now) {
        return startDateTime(date, time).isBefore(now);
    }

    private LocalDateTime startDateTime(LocalDate date, ReservationTime time) {
        return LocalDateTime.of(date, time.getStartAt());
    }

    public boolean isOwnedBy(Member targetMember) {
        if (member == targetMember) {
            return true;
        }
        return member.getId() != null && member.getId().equals(targetMember.getId());
    }

    public boolean isManagedBy(Store store) {
        return theme.getStore() != null && theme.getStore().getId().equals(store.getId());
    }

    public boolean isPastSchedule(LocalDate date, ReservationTime time, LocalDateTime now) {
        return startDateTime(date, time).isBefore(now);
    }

    public boolean hasSchedule(LocalDate date, ReservationTime time) {
        if (!this.date.equals(date)) {
            return false;
        }
        if (this.time == time) {
            return true;
        }
        return this.time.getId() != null && this.time.getId().equals(time.getId());
    }

    public void changeSchedule(LocalDate date, ReservationTime time) {
        validateSchedule(date, time);
        this.date = date;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Theme getTheme() {
        return theme;
    }

    public ReservationTime getTime() {
        return time;
    }

    public LocalDate getDate() {
        return date;
    }
}
