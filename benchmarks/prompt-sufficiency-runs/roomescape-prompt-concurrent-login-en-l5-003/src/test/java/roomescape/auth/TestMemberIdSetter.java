package roomescape.auth;

import java.lang.reflect.Field;
import roomescape.domain.Member;

final class TestMemberIdSetter {

    private TestMemberIdSetter() {
    }

    static void setId(Member member, Long id) {
        try {
            Field field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
