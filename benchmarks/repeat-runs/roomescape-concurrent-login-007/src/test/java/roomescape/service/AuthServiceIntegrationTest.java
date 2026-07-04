package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.repository.MemberRepository;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("로그인 토큰 인증은 로그인 회원 식별자와 이름을 반환한다")
    void authenticateLoginMemberIdentity() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginResponse loginResponse = authService.login(new LoginRequest("brown@example.com", "password"));

        LoginMember loginMember = authService.authenticate(loginResponse.accessToken());

        assertThat(loginMember.id()).isEqualTo(member.getId());
        assertThat(loginMember.name()).isEqualTo("브라운");
    }
}
