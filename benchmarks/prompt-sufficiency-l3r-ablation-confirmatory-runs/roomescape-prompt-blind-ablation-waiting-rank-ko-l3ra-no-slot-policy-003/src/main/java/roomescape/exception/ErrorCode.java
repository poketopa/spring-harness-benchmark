package roomescape.exception;

public enum ErrorCode {
    INVALID_INPUT,
    UNAUTHORIZED,
    THEME_NOT_FOUND,
    RESERVATION_TIME_NOT_FOUND,
    DUPLICATE_RESERVATION,
    RESERVATION_SLOT_NOT_OCCUPIED,
    DUPLICATE_WAITING,
    PAST_RESERVATION;

    public String getCode() {
        return name();
    }
}
