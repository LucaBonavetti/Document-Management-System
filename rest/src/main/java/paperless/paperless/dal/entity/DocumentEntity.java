package paperless.paperless.dal.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "ocr_text_key")
    private String ocrTextKey;

    @Column(name = "ocr_processed_at")
    private OffsetDateTime ocrProcessedAt;

    @Column(name = "ocr_indexed_at")
    private OffsetDateTime ocrIndexedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getOcrTextKey() { return ocrTextKey; }
    public void setOcrTextKey(String ocrTextKey) { this.ocrTextKey = ocrTextKey; }

    public OffsetDateTime getOcrProcessedAt() { return ocrProcessedAt; }
    public void setOcrProcessedAt(OffsetDateTime ocrProcessedAt) { this.ocrProcessedAt = ocrProcessedAt; }

    public OffsetDateTime getOcrIndexedAt() { return ocrIndexedAt; }
    public void setOcrIndexedAt(OffsetDateTime ocrIndexedAt) { this.ocrIndexedAt = ocrIndexedAt; }
}