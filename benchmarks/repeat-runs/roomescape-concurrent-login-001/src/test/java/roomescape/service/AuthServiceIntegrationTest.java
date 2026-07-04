package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("새 로그인이 발생하면 이전 토큰은 인증에 실패하고 새 토큰만 유효하다")
    void newestLoginInvalidatesPreviousToken() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginRequest request = new LoginRequest("brown@example.com", "password");
        LoginResponse first = authService.login(request);

        LoginResponse second = authService.login(request);

        assertThat(first.accessToken()).isNotEqualTo(second.accessToken());
        assertThatThrownBy(() -> authService.authenticate(first.accessToken()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        LoginMember loginMember = authService.authenticate(second.accessToken());
        assertThat(loginMember.id()).isEqualTo(member.getId());
        assertThat(loginMember.name()).isEqualTo("브라운");
    }
}
