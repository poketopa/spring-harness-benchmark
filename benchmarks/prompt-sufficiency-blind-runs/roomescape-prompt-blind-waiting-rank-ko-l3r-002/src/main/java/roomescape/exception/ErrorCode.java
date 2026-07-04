package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    DUPLICATE_RESERVATION,
    PAST_RESERVATION,
    WAITING_SLOT_AVAILABLE,
    WAITING_OWN_RESERVATION,
    DUPLICATE_WAITING;

    public String getCode() {
        return name();
    }
}
