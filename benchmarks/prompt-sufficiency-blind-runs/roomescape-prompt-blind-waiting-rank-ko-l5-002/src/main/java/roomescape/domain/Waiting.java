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
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_waiting_member_reservation",
                columnNames = {"member_id", "reservation_id"}
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
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Waiting() {
    }

    public Waiting(Member member, Reservation reservation, LocalDateTime createdAt) {
        this.member = member;
        this.reservation = reservation;
        this.createdAt = createdAt;
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

    public Reservation getReservation() {
        return reservation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
