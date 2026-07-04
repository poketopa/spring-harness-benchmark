package roomescape.service;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
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
        Member manager = findManager(request.managerId());
        validateManagerHasNoStore(manager);
        Store store = new Store(request.name(), manager);
        return saveStore(store);
    }

    private Member findManager(Long managerId) {
        return memberRepository.findById(managerId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private void validateManagerHasNoStore(Member manager) {
        if (storeRepository.existsByManager(manager)) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "이미 매장을 관리 중인 매니저입니다.");
        }
    }

    private StoreResponse saveStore(Store store) {
        try {
            return StoreResponse.from(storeRepository.saveAndFlush(store));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "이미 매장을 관리 중인 매니저입니다.");
        }
    }

    public List<StoreResponse> findAll() {
        return storeRepository.findAll()
                .stream()
                .map(StoreResponse::from)
                .toList();
    }
}
