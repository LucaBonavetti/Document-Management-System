package paperless.paperless.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import paperless.paperless.bl.mapper.TagMapper;
import paperless.paperless.bl.model.BlTag;
import paperless.paperless.bl.service.TagService;
import paperless.paperless.model.Tag;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TagService tagService;

    @MockBean
    private TagMapper tagMapper;

    @Test
    void getTags_returns_list() throws Exception {
        BlTag a = new BlTag(); a.setId(1L); a.setName("invoice"); a.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        BlTag b = new BlTag(); b.setId(2L); b.setName("important"); b.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        Tag ta = new Tag(1L, "invoice", a.getCreatedAt());
        Tag tb = new Tag(2L, "important", b.getCreatedAt());

        Mockito.when(tagService.getTagsForDocument(10L)).thenReturn(List.of(a, b));
        Mockito.when(tagMapper.toApiList(List.of(a, b))).thenReturn(List.of(ta, tb));

        mvc.perform(get("/api/documents/10/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("invoice")))
                .andExpect(jsonPath("$[1].name", is("important")));
    }

    @Test
    void setTags_replaces_list() throws Exception {
        BlTag a = new BlTag(); a.setId(1L); a.setName("invoice"); a.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        Tag ta = new Tag(1L, "invoice", a.getCreatedAt());

        Mockito.when(tagService.setTagsForDocument(Mockito.eq(10L), Mockito.anyList())).thenReturn(List.of(a));
        Mockito.when(tagMapper.toApiList(List.of(a))).thenReturn(List.of(ta));

        mvc.perform(put("/api/documents/10/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"invoice\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("invoice")));
    }

    @Test
    void addTag_adds_one() throws Exception {
        BlTag a = new BlTag(); a.setId(1L); a.setName("invoice"); a.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        Tag ta = new Tag(1L, "invoice", a.getCreatedAt());

        Mockito.when(tagService.addTagToDocument(Mockito.eq(10L), Mockito.anyString())).thenReturn(a);
        Mockito.when(tagMapper.toApi(a)).thenReturn(ta);

        mvc.perform(post("/api/documents/10/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"invoice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("invoice")));
    }

    @Test
    void removeTag_returns_204() throws Exception {
        mvc.perform(delete("/api/documents/10/tags/invoice"))
                .andExpect(status().isNoContent());
    }
}
