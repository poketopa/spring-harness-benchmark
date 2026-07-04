package roomescape.domain;

import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

final class DomainValidator {

    private DomainValidator() {
    }

    static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, message);
        }
    }

    static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, message);
        }
    }
}
