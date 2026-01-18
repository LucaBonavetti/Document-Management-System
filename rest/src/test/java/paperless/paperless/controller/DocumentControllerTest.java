package paperless.paperless.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import paperless.paperless.bl.mapper.DocumentMapper;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.model.BlUploadRequest;
import paperless.paperless.bl.service.DocumentService;
import paperless.paperless.model.Document;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentMapper mapper;

    @Test
    void upload_returns_201_and_body_and_location() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());

        BlDocument bl = new BlDocument();
        bl.setId(100L);
        bl.setFilename("hello.txt");
        bl.setContentType("text/plain");
        bl.setSize(5L);
        bl.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));

        Document dto = new Document();
        dto.setId(100L);
        dto.setFilename("hello.txt");
        dto.setContentType("text/plain");
        dto.setSize(5L);
        dto.setUploadedAt(OffsetDateTime.parse("2025-10-22T10:00:00Z"));

        Mockito.when(documentService.saveDocument(Mockito.any(BlUploadRequest.class), Mockito.any(byte[].class)))
                .thenReturn(bl);
        Mockito.when(mapper.toApi(bl)).thenReturn(dto);

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/documents/100")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.filename", is("hello.txt")))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    void getById_found_returns_200() throws Exception {
        BlDocument bl = new BlDocument();
        bl.setId(7L);
        bl.setFilename("x.pdf");
        bl.setContentType("application/pdf");
        bl.setSize(10L);
        bl.setUploadedAt(OffsetDateTime.parse("2025-10-21T00:00:00Z"));

        Document dto = new Document();
        dto.setId(7L);
        dto.setFilename("x.pdf");
        dto.setContentType("application/pdf");
        dto.setSize(10L);
        dto.setUploadedAt(OffsetDateTime.parse("2025-10-21T00:00:00Z"));

        Mockito.when(documentService.getById(7L)).thenReturn(bl);
        Mockito.when(mapper.toApi(bl)).thenReturn(dto);

        mvc.perform(get("/api/documents/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.filename", is("x.pdf")));
    }

    @Test
    void getById_not_found_returns_404() throws Exception {
        Mockito.when(documentService.getById(404L)).thenReturn(null);
        mvc.perform(get("/api/documents/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returns_recent_documents() throws Exception {
        BlDocument a = new BlDocument();
        a.setId(1L);
        a.setFilename("a.txt");
        a.setContentType("text/plain");
        a.setSize(1L);
        a.setUploadedAt(OffsetDateTime.parse("2025-10-20T00:00:00Z"));

        BlDocument b = new BlDocument();
        b.setId(2L);
        b.setFilename("b.txt");
        b.setContentType("text/plain");
        b.setSize(2L);
        b.setUploadedAt(OffsetDateTime.parse("2025-10-21T00:00:00Z"));

        Document da = new Document();
        da.setId(1L); da.setFilename("a.txt"); da.setContentType("text/plain"); da.setSize(1L); da.setUploadedAt(a.getUploadedAt());
        Document db = new Document();
        db.setId(2L); db.setFilename("b.txt"); db.setContentType("text/plain"); db.setSize(2L); db.setUploadedAt(b.getUploadedAt());

        Mockito.when(documentService.getRecent(5)).thenReturn(List.of(a, b));
        Mockito.when(mapper.toApiList(List.of(a, b))).thenReturn(List.of(da, db));

        mvc.perform(get("/api/documents?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }
}
