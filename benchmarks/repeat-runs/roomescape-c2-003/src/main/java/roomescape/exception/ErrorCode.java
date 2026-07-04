package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    RESERVATION_NOT_FOUND,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    RESERVATION_TIME_IN_USE,
    WAITING_NOT_FOUND,
    DUPLICATE_RESERVATION,
    DUPLICATE_WAITING,
    PAST_RESERVATION,
    WAITING_NOT_ALLOWED;

    public String getCode() {
        return name();
    }
}
