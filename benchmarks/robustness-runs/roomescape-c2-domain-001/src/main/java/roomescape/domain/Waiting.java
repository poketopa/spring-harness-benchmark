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
                name = "uk_waiting_member_theme_time_date",
                columnNames = {"member_id", "theme_id", "time_id", "date"}
        )
})
public class Waiting {

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Waiting() {
    }

    public Waiting(Member member, Theme theme, ReservationTime time, LocalDate date, LocalDateTime createdAt) {
        validateRequired(member, theme, time, date, createdAt);
        this.member = member;
        this.theme = theme;
        this.time = time;
        this.date = date;
        this.createdAt = createdAt;
    }

    private void validateRequired(
            Member member,
            Theme theme,
            ReservationTime time,
            LocalDate date,
            LocalDateTime createdAt
    ) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 회원은 비어 있을 수 없습니다.");
        }
        if (theme == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 테마는 비어 있을 수 없습니다.");
        }
        if (time == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 시간은 비어 있을 수 없습니다.");
        }
        if (date == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 날짜는 비어 있을 수 없습니다.");
        }
        if (createdAt == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 생성 시각은 비어 있을 수 없습니다.");
        }
    }

    public boolean isPast(LocalDateTime now) {
        return startDateTime().isBefore(now);
    }

    private LocalDateTime startDateTime() {
        return LocalDateTime.of(date, time.getStartAt());
    }

    public boolean isOwnedBy(Member targetMember) {
        if (member == targetMember) {
            return true;
        }
        return member.getId() != null && member.getId().equals(targetMember.getId());
    }

    public Reservation approve() {
        return new Reservation(member, theme, time, date);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
