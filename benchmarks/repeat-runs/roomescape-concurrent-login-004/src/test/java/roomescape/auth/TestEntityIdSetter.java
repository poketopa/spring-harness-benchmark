package roomescape.auth;

import java.lang.reflect.Field;

final class TestEntityIdSetter {

    private TestEntityIdSetter() {
    }

    static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
