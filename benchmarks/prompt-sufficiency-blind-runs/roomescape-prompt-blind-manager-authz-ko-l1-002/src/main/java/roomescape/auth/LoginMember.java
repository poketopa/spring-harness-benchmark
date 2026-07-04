package roomescape.auth;

import roomescape.domain.Role;

public record LoginMember(Long id, String name, Role role) {

    public LoginMember(Long id, String name) {
        this(id, name, Role.USER);
    }
}
