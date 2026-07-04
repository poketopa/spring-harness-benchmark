package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    DUPLICATE_RESERVATION,
    PAST_RESERVATION,
    AVAILABLE_SLOT_WAITING,
    OWN_RESERVATION_WAITING,
    DUPLICATE_WAITING;

    public String getCode() {
        return name();
    }
}
