package roomescape.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Store;

@DataJpaTest
class StoreRepositoryIntegrationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("매니저가 관리하는 매장을 조회한다")
    void findStoreByManager() {
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER));
        Store store = storeRepository.save(new Store("강남점", manager));

        Store found = storeRepository.findByManager(manager).orElseThrow();

        assertThat(found.getId()).isEqualTo(store.getId());
    }
}
