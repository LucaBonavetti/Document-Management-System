package paperless.paperless.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class IndexedDocument {

    private Long id;
    private String filename = "";
    private String content = "";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime uploadedAt;

    /**
     * Optional metadata (will be filled in the Tags use-case step).
     */
    private List<String> tags = new ArrayList<>();
}
