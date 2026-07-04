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
    private final ManagerAuthorizationService managerAuthorizationService = new ManagerAuthorizationService(
            authenticatedMemberService,
            storeRepository
    );

    @Test
    @DisplayName("매니저가 관리하는 매장을 조회한다")
    void findManagedStore() {
        // given
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member manager = new Member("브라운", "brown@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(manager);
        when(storeRepository.findByManager(manager)).thenReturn(Optional.of(store));

        // when
        Store found = managerAuthorizationService.findManagedStore(loginMember);

        // then
        assertThat(found).isSameAs(store);
    }

    @Test
    @DisplayName("일반 회원이면 매니저 인가에 실패한다")
    void userThrowsForbidden() {
        // given
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member user = new Member("브라운", "brown@example.com", "password");
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(user);

        // when & then
        assertThatThrownBy(() -> managerAuthorizationService.findManagedStore(loginMember))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("관리하는 매장이 없으면 매니저 인가에 실패한다")
    void managerWithoutStoreThrowsForbidden() {
        // given
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member manager = new Member("브라운", "brown@example.com", "password", Role.MANAGER);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(manager);
        when(storeRepository.findByManager(manager)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> managerAuthorizationService.findManagedStore(loginMember))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
