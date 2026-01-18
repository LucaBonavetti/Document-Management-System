package paperless.paperless.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import paperless.paperless.bl.service.SearchService;
import paperless.paperless.model.SearchDocumentResult;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SearchService searchService;

    @Test
    void search_returns_json_list() throws Exception {
        SearchDocumentResult r = new SearchDocumentResult();
        r.setId(1L);
        r.setFilename("a.pdf");
        r.setContentType("application/pdf");
        r.setSize(10L);
        r.setUploadedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        r.setTags(List.of("invoice"));
        r.setScore(1.0);

        Mockito.when(searchService.search(Mockito.eq("hello"), Mockito.eq(List.of("invoice")), Mockito.eq(20)))
                .thenReturn(List.of(r));

        mvc.perform(get("/api/search")
                        .param("query", "hello")
                        .param("tag", "invoice")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].filename", is("a.pdf")))
                .andExpect(jsonPath("$[0].tags[0]", is("invoice")));
    }
}
