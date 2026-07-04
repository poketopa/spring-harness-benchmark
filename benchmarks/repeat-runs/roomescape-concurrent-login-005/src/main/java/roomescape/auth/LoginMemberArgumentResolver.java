package roomescape.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.service.AuthService;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final AuthService authService;

    public LoginMemberArgumentResolver(AuthService authService) {
        this.authService = authService;
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
        return authService.authenticate(extractAccessToken(authorization));
    }

    private String extractAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "인증 정보가 필요합니다.");
        }
        return authorization.substring(BEARER.length());
    }
}
