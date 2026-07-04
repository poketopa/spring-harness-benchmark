package roomescape.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    @EntityGraph(attributePaths = {"theme", "time"})
    List<Waiting> findAllByMemberOrderByDateAscTimeStartAtAscRequestedAtAscIdAsc(Member member);

    @Query("""
            select count(waiting) + 1
            from Waiting waiting
            where waiting.theme = :theme
              and waiting.time = :time
              and waiting.date = :date
              and (
                    waiting.requestedAt < :requestedAt
                    or (waiting.requestedAt = :requestedAt and waiting.id < :waitingId)
              )
            """)
    int rankOf(
            @Param("theme") Theme theme,
            @Param("time") ReservationTime time,
            @Param("date") LocalDate date,
            @Param("requestedAt") LocalDateTime requestedAt,
            @Param("waitingId") Long waitingId
    );
}
