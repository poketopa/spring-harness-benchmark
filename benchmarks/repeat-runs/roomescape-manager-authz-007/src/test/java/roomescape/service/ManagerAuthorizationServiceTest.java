package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreRepository;

class ManagerAuthorizationServiceTest {

    private final AuthenticatedMemberService authenticatedMemberService = mock(AuthenticatedMemberService.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ManagerAuthorizationService managerAuthorizationService =
            new ManagerAuthorizationService(authenticatedMemberService, storeRepository);

    @Test
    @DisplayName("매니저 회원의 관리 매장을 조회한다")
    void findManagedStore() {
        LoginMember loginMember = new LoginMember(1L, "매니저");
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(manager);
        when(storeRepository.findByManager(manager)).thenReturn(Optional.of(store));

        Store result = managerAuthorizationService.findManagedStore(loginMember);

        assertThat(result).isEqualTo(store);
    }

    @Test
    @DisplayName("일반 회원은 매장 관리 권한을 가질 수 없다")
    void userCannotFindManagedStore() {
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member user = new Member("브라운", "brown@example.com", "password", Role.USER);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(user);

        assertThatThrownBy(() -> managerAuthorizationService.findManagedStore(loginMember))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
