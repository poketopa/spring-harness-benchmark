package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerAuthorizationServiceIntegrationTest {

    @Autowired
    private ManagerAuthorizationService managerAuthorizationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("매니저가 관리 중인 매장을 조회한다")
    void authorizeManagedStore() {
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER));
        Store store = storeRepository.save(new Store("강남점", manager));

        Store authorized = managerAuthorizationService.authorizeStore(new LoginMember(manager.getId(), manager.getName()));

        assertThat(authorized.getId()).isEqualTo(store.getId());
    }

    @Test
    @DisplayName("일반 회원의 매니저 권한 확인은 인가 실패로 처리한다")
    void userCannotAuthorizeManagerStore() {
        Member user = memberRepository.save(new Member("회원", "user@example.com", "password", Role.USER));

        assertThatThrownBy(() -> managerAuthorizationService.authorizeStore(new LoginMember(user.getId(), user.getName())))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
