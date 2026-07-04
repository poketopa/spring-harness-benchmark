package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    DUPLICATE_RESERVATION,
    PAST_RESERVATION,
    WAITING_NOT_AVAILABLE,
    DUPLICATE_WAITING,
    WAITING_NOT_FOUND;

    public String getCode() {
        return name();
    }
}
