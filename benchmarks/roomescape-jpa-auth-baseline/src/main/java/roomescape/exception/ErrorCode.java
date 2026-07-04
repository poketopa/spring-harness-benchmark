package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    WAITING_NOT_FOUND,
    DUPLICATE_RESERVATION,
    PAST_RESERVATION,
    DUPLICATE_WAITING,
    PAST_WAITING,
    UNAVAILABLE_WAITING;

    public String getCode() {
        return name();
    }
}
