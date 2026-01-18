package paperless.paperless.dal.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_tags_name", columnNames = "name")
)
public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // normalized name (lowercase, trimmed, whitespace->"-")
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
