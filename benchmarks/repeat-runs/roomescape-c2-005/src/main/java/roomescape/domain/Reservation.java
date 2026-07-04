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
        this.member = member;
        this.theme = theme;
        this.time = time;
        this.date = date;
    }

    public boolean isPast(LocalDateTime now) {
        return startDateTime().isBefore(now);
    }

    private LocalDateTime startDateTime() {
        return LocalDateTime.of(date, time.getStartAt());
    }

    public boolean isPastSchedule(ReservationTime targetTime, LocalDate targetDate, LocalDateTime now) {
        return LocalDateTime.of(targetDate, targetTime.getStartAt()).isBefore(now);
    }

    public boolean isSameSlot(Theme targetTheme, ReservationTime targetTime, LocalDate targetDate) {
        return theme.getId().equals(targetTheme.getId())
                && time.getId().equals(targetTime.getId())
                && date.equals(targetDate);
    }

    public void changeSchedule(ReservationTime targetTime, LocalDate targetDate) {
        this.time = targetTime;
        this.date = targetDate;
    }

    public boolean isOwnedBy(Member targetMember) {
        if (member == targetMember) {
            return true;
        }
        return member.getId() != null && member.getId().equals(targetMember.getId());
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
