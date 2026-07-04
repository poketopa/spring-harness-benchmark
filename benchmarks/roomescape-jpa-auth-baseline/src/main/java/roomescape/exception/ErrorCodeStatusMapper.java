package roomescape.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeStatusMapper {

    public HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_INPUT, PAST_RESERVATION, PAST_WAITING, UNAVAILABLE_WAITING -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case THEME_NOT_FOUND, RESERVATION_TIME_NOT_FOUND, WAITING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_RESERVATION, DUPLICATE_WAITING -> HttpStatus.CONFLICT;
        };
    }
}
