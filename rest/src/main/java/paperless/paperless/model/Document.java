package paperless.paperless.model;

import java.time.OffsetDateTime;
import java.util.List;

public class Document {
    private Long id;
    private String filename;
    private String contentType;
    private long size;
    private OffsetDateTime uploadedAt;
    private String category;
    private List<String> tags;

    public Document() {}

    public Document(Long id, String filename, String contentType, long size, OffsetDateTime uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}