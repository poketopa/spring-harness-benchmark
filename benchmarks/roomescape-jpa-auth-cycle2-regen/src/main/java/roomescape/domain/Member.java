package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected Member() {
    }

    public Member(String name, String email, String password) {
        this(name, email, password, Role.USER);
    }

    public Member(String name, String email, String password, Role role) {
        DomainValidator.requireNonBlank(name, "회원 이름은 비어 있을 수 없습니다.");
        DomainValidator.requireNonBlank(email, "이메일은 비어 있을 수 없습니다.");
        DomainValidator.requireNonBlank(password, "비밀번호는 비어 있을 수 없습니다.");
        DomainValidator.requireNonNull(role, "회원 권한은 비어 있을 수 없습니다.");
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public boolean hasPassword(String rawPassword) {
        return password.equals(rawPassword);
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
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
