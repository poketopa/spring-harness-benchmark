package roomescape.exception;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(RoomescapeException exception) {
        return new ErrorResponse(exception.getErrorCode().getCode(), exception.getMessage());
    }
}
