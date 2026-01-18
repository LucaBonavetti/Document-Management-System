package paperless.paperless.bl.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public class BlDocument {
    private Long id;

    @NotBlank(message = "filename must not be blank")
    private String filename;

    private String contentType;

    @Positive(message = "size must be > 0")
    private long size;

    @NotNull(message = "uploadedAt must not be null")
    private OffsetDateTime uploadedAt;

    private String summary;

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

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}