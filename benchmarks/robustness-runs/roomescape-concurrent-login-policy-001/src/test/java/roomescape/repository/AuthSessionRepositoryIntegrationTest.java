package roomescape.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import roomescape.domain.AuthSession;
import roomescape.domain.Member;

@DataJpaTest
class AuthSessionRepositoryIntegrationTest {

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원의 활성 인증 세션을 조회한다")
    void findByMember() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        AuthSession authSession = authSessionRepository.save(new AuthSession(member, "token"));

        AuthSession found = authSessionRepository.findByMember(member).orElseThrow();

        assertThat(found.getId()).isEqualTo(authSession.getId());
        assertThat(found.hasAccessToken("token")).isTrue();
    }

    @Test
    @DisplayName("한 회원에게 활성 인증 세션은 하나만 저장할 수 있다")
    void duplicateMemberSessionFailsByDatabaseConstraint() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        authSessionRepository.saveAndFlush(new AuthSession(member, "old-token"));

        AuthSession duplicate = new AuthSession(member, "new-token");

        assertThatThrownBy(() -> authSessionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
