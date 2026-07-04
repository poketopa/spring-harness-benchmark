package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.dto.StoreRequest;
import roomescape.dto.StoreResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    public StoreService(StoreRepository storeRepository, MemberRepository memberRepository) {
        this.storeRepository = storeRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public StoreResponse create(StoreRequest request) {
        Member manager = memberRepository.findById(request.managerId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 매니저입니다."));
        Store store = new Store(request.name(), manager);
        return StoreResponse.from(storeRepository.save(store));
    }
}
