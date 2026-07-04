package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Waiting;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class WaitingRankService {

    private final WaitingRepository waitingRepository;

    public WaitingRankService(WaitingRepository waitingRepository) {
        this.waitingRepository = waitingRepository;
    }

    public int calculate(Waiting waiting) {
        long previousWaitingCount = waitingRepository.countEarlierWaitings(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        );
        return Math.toIntExact(previousWaitingCount + 1);
    }
}
