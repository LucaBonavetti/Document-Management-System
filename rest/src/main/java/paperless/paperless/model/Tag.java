package paperless.paperless.model;

import java.time.OffsetDateTime;

public class Tag {

    private Long id;
    private String name;
    private OffsetDateTime createdAt;

    public Tag() { }

    public Tag(Long id, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
