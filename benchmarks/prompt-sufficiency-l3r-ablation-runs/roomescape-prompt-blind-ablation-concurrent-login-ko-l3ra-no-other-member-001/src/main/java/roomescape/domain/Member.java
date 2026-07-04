package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String activeSessionToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected Member() {
    }

    public Member(String name, String email, String password) {
        this(name, email, password, Role.USER);
    }

    public Member(String name, String email, String password, Role role) {
        validateRequired(name, email, password, role);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    private void validateRequired(String name, String email, String password, Role role) {
        validateNonBlank(name, "회원 이름은 비어 있을 수 없습니다.");
        validateNonBlank(email, "이메일은 비어 있을 수 없습니다.");
        validateNonBlank(password, "비밀번호는 비어 있을 수 없습니다.");
        if (role == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "회원 권한은 비어 있을 수 없습니다.");
        }
    }

    private void validateNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, message);
        }
    }

    public boolean hasPassword(String rawPassword) {
        return password.equals(rawPassword);
    }

    public void activateSession(String sessionToken) {
        validateNonBlank(sessionToken, "인증 토큰은 비어 있을 수 없습니다.");
        this.activeSessionToken = sessionToken;
    }

    public boolean hasActiveSession(String sessionToken) {
        return activeSessionToken != null && activeSessionToken.equals(sessionToken);
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isSameMember(Member target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }
        return id != null && id.equals(target.id);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
