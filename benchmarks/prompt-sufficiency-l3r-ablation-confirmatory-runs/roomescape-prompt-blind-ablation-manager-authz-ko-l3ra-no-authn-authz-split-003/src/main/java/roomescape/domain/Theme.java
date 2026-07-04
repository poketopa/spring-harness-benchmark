package roomescape.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    protected Theme() {
    }

    public Theme(String name, String description, String thumbnailUrl) {
        this(name, description, thumbnailUrl, new Store("기본 매장"));
    }

    public Theme(String name, String description, String thumbnailUrl, Store store) {
        validateRequired(name, description, thumbnailUrl);
        validateStore(store);
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.store = store;
    }

    private void validateRequired(String name, String description, String thumbnailUrl) {
        validateNonBlank(name, "테마 이름은 비어 있을 수 없습니다.");
        validateNonBlank(description, "테마 설명은 비어 있을 수 없습니다.");
        validateNonBlank(thumbnailUrl, "테마 썸네일은 비어 있을 수 없습니다.");
    }

    private void validateNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, message);
        }
    }

    private void validateStore(Store store) {
        if (store == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "테마 매장은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Store getStore() {
        return store;
    }
}
