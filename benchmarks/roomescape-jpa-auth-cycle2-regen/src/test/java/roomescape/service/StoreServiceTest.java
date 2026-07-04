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
import roomescape.dto.StoreRequest;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;

class StoreServiceTest {

    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AdminAuthorizationService adminAuthorizationService = mock(AdminAuthorizationService.class);
    private final StoreService storeService = new StoreService(
            storeRepository,
            memberRepository,
            adminAuthorizationService
    );

    @Test
    @DisplayName("이미 매장을 가진 매니저로 매장을 생성할 수 없다")
    void createStoreForManagerWithStoreThrowsManagerAlreadyHasStore() {
        LoginMember admin = new LoginMember(1L, "어드민");
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        StoreRequest request = new StoreRequest("강남점", 2L);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(storeRepository.existsByManager(manager)).thenReturn(true);

        assertThatThrownBy(() -> storeService.create(admin, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MANAGER_ALREADY_HAS_STORE));
    }

    @Test
    @DisplayName("매니저가 아닌 회원으로 매장을 생성할 수 없다")
    void createStoreForNonManagerThrowsInvalidInput() {
        LoginMember admin = new LoginMember(1L, "어드민");
        Member member = new Member("브라운", "brown@example.com", "password");
        StoreRequest request = new StoreRequest("강남점", 2L);
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> storeService.create(admin, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
