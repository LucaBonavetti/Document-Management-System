package paperless.paperless.model;

import jakarta.validation.constraints.NotBlank;

public class TagNameRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    public TagNameRequest() { }

    public TagNameRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
