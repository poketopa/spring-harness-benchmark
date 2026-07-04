package roomescape.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    @Query("""
            select count(w) + 1
            from Waiting w
            where w.theme = :theme
              and w.time = :time
              and w.date = :date
              and (
                    w.createdAt < :#{#waiting.createdAt}
                    or (w.createdAt = :#{#waiting.createdAt} and w.id < :#{#waiting.id})
              )
            """)
    int calculateRank(
            @Param("waiting") Waiting waiting,
            @Param("theme") Theme theme,
            @Param("time") ReservationTime time,
            @Param("date") LocalDate date
    );
}
