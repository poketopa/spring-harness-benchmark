package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Store;
import roomescape.dto.StoreRequest;
import roomescape.dto.StoreResponse;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreResponse create(StoreRequest request) {
        Store store = storeRepository.save(new Store(request.name()));
        return StoreResponse.from(store);
    }
}
