package paperless.paperless.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class SearchDocumentResult {

    private Long id;
    private String filename;
    private String contentType;
    private long size;
    private OffsetDateTime uploadedAt;

    private List<String> tags = new ArrayList<>();
    private Double score;

    public SearchDocumentResult() { }

    public SearchDocumentResult(Long id, String filename, String contentType, long size,
                                OffsetDateTime uploadedAt, List<String> tags, Double score) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedAt = uploadedAt;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.score = score;
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

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
