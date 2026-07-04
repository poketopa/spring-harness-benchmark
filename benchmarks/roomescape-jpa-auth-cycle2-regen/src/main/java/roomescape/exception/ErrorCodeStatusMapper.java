package roomescape.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeStatusMapper {

    public HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_INPUT, PAST_RESERVATION, WAITING_NOT_ALLOWED -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case MEMBER_NOT_FOUND, STORE_NOT_FOUND, THEME_NOT_FOUND, RESERVATION_TIME_NOT_FOUND,
                 RESERVATION_NOT_FOUND, WAITING_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case MANAGER_ALREADY_HAS_STORE, RESERVATION_TIME_IN_USE, DUPLICATE_RESERVATION, DUPLICATE_WAITING ->
                    HttpStatus.CONFLICT;
        };
    }
}
