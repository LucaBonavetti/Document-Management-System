package paperless.paperless.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paperless.paperless.bl.mapper.TagMapper;
import paperless.paperless.bl.model.BlTag;
import paperless.paperless.bl.service.TagService;
import paperless.paperless.model.Tag;
import paperless.paperless.model.TagListRequest;
import paperless.paperless.model.TagNameRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public TagController(TagService tagService, TagMapper tagMapper) {
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    @GetMapping(path = "/documents/{documentId}/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Tag>> getTags(@PathVariable("documentId") Long documentId) {
        List<BlTag> tags = tagService.getTagsForDocument(documentId);
        return ResponseEntity.ok(tagMapper.toApiList(tags));
    }

    @PutMapping(path = "/documents/{documentId}/tags",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Tag>> setTags(@PathVariable("documentId") Long documentId,
                                             @Valid @RequestBody TagListRequest req) {
        List<BlTag> tags = tagService.setTagsForDocument(documentId, req.getTags());
        return ResponseEntity.ok(tagMapper.toApiList(tags));
    }

    @PostMapping(path = "/documents/{documentId}/tags",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Tag> addTag(@PathVariable("documentId") Long documentId,
                                      @Valid @RequestBody TagNameRequest req) {
        BlTag created = tagService.addTagToDocument(documentId, req.getName());
        return ResponseEntity.ok(tagMapper.toApi(created));
    }

    @DeleteMapping(path = "/documents/{documentId}/tags/{tagName}")
    public ResponseEntity<Void> removeTag(@PathVariable("documentId") Long documentId,
                                          @PathVariable("tagName") String tagName) {
        tagService.removeTagFromDocument(documentId, tagName);
        return ResponseEntity.noContent().build();
    }
}
