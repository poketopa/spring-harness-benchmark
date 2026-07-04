package roomescape.exception;

import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorCodeStatusMapper statusMapper;

    public GlobalExceptionHandler(ErrorCodeStatusMapper statusMapper) {
        this.statusMapper = statusMapper;
    }

    @ExceptionHandler(RoomescapeException.class)
    ResponseEntity<ErrorResponse> handleRoomescapeException(RoomescapeException exception) {
        return ResponseEntity
                .status(statusMapper.statusOf(exception.getErrorCode()))
                .body(ErrorResponse.from(exception));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(), message));
    }
}
