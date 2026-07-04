package roomescape.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndReservation(Member member, Reservation reservation);

    @EntityGraph(attributePaths = {"reservation", "reservation.theme", "reservation.time"})
    List<Waiting> findAllByMember(Member member);

    @Query("""
            select count(w)
            from Waiting w
            where w.reservation = :reservation
              and (
                    w.createdAt < :createdAt
                    or (w.createdAt = :createdAt and w.id <= :waitingId)
              )
            """)
    int rankOf(
            @Param("reservation") Reservation reservation,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("waitingId") Long waitingId
    );
}
