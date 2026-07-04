package roomescape.domain;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    protected Theme() {
    }

    public Theme(String name, String description, String thumbnailUrl) {
        validateRequired(name, description, thumbnailUrl);
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
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

    public void assignStore(Store store) {
        if (store == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "테마 매장은 비어 있을 수 없습니다.");
        }
        this.store = store;
    }

    public boolean isInStore(Store store) {
        if (this.store == null || store == null) {
            return false;
        }
        if (this.store == store) {
            return true;
        }
        return this.store.getId() != null && this.store.getId().equals(store.getId());
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
}
