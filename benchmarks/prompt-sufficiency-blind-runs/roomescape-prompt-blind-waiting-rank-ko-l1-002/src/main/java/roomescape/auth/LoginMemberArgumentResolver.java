package roomescape.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final AuthTokenProvider authTokenProvider;
    private final MemberRepository memberRepository;

    public LoginMemberArgumentResolver(AuthTokenProvider authTokenProvider, MemberRepository memberRepository) {
        this.authTokenProvider = authTokenProvider;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(LoginMember.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String authorization = webRequest.getHeader(AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "인증 정보가 필요합니다.");
        }

        Long memberId = authTokenProvider.extractMemberId(authorization.substring(BEARER.length()));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        return new LoginMember(member.getId(), member.getName());
    }
}
