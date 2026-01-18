package paperless.paperless.model;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TagListRequest {

    @NotNull(message = "tags must not be null")
    private List<String> tags = new ArrayList<>();

    public TagListRequest() { }

    public TagListRequest(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
