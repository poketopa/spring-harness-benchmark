package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("같은 회원에게 발급한 토큰도 매번 다른 값이다")
    void createUniqueTokenForSameMember() {
        Member member = new Member("브라운", "brown@example.com", "password");
        TestEntityIdSetter.setId(member, 1L);

        String firstToken = authTokenProvider.createToken(member);
        String secondToken = authTokenProvider.createToken(member);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("토큰에서 로그인 회원 식별자를 추출한다")
    void extractMemberId() {
        Member member = new Member("브라운", "brown@example.com", "password");
        TestEntityIdSetter.setId(member, 1L);
        String token = authTokenProvider.createToken(member);

        Long memberId = authTokenProvider.extractMemberId(token);

        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증 실패로 처리한다")
    void rejectInvalidToken() {
        assertThatThrownBy(() -> authTokenProvider.extractMemberId("invalid-token"))
                .isInstanceOf(RoomescapeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
