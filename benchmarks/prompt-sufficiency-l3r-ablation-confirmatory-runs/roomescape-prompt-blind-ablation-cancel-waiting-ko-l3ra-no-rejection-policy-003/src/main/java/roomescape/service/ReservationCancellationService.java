package roomescape.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class ReservationCancellationService {

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final MemberRepository memberRepository;
    private final WaitingPromotionService waitingPromotionService;

    public ReservationCancellationService(
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository,
            MemberRepository memberRepository,
            WaitingPromotionService waitingPromotionService
    ) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
        this.memberRepository = memberRepository;
        this.waitingPromotionService = waitingPromotionService;
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member member = findMember(loginMember);
        Reservation reservation = reservationRepository.findByIdAndMember(reservationId, member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));

        Optional<Waiting> firstWaiting = waitingRepository.findFirstByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                reservation.getTheme(),
                reservation.getTime(),
                reservation.getDate()
        );

        reservationRepository.delete(reservation);
        reservationRepository.flush();

        firstWaiting.ifPresent(waitingPromotionService::promote);
    }

    private Member findMember(LoginMember loginMember) {
        return memberRepository.findById(loginMember.id())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }
}
