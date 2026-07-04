package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_store_id")
    private Store managedStore;

    protected Member() {
    }

    public Member(String name, String email, String password) {
        this(name, email, password, Role.USER);
    }

    public Member(String name, String email, String password, Role role) {
        this(name, email, password, role, null);
    }

    public Member(String name, String email, String password, Role role, Store managedStore) {
        validateRequired(name, email, password, role);
        validateManagedStore(role, managedStore);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.managedStore = managedStore;
    }

    private void validateRequired(String name, String email, String password, Role role) {
        validateNonBlank(name, "회원 이름은 비어 있을 수 없습니다.");
        validateNonBlank(email, "이메일은 비어 있을 수 없습니다.");
        validateNonBlank(password, "비밀번호는 비어 있을 수 없습니다.");
        if (role == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "회원 권한은 비어 있을 수 없습니다.");
        }
    }

    private void validateManagedStore(Role role, Store managedStore) {
        if (role == Role.MANAGER && managedStore == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저는 관리 매장이 필요합니다.");
        }
        if (role != Role.MANAGER && managedStore != null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저가 아닌 회원은 관리 매장을 가질 수 없습니다.");
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

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean manages(Store store) {
        return managedStore != null && managedStore.isSameStore(store);
    }

    public Store getManagedStore() {
        return managedStore;
    }
}
