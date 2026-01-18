package paperless.paperless.dal.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "document_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_document_tags_document_tag",
                columnNames = {"document_id", "tag_id"}
        )
)
public class DocumentTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // join -> document
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    // join -> tag
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @Column(nullable = false, name = "assigned_at")
    private OffsetDateTime assignedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentEntity getDocument() { return document; }
    public void setDocument(DocumentEntity document) { this.document = document; }

    public TagEntity getTag() { return tag; }
    public void setTag(TagEntity tag) { this.tag = tag; }

    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }
}
