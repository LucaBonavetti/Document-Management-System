package paperless.paperless.controller;


import paperless.paperless.bl.component.DocumentManager;
import paperless.paperless.bl.model.BlDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DocumentManager documentManager;

    private static BlDocument sampleDoc() {
        BlDocument d = new BlDocument();
        d.setId(42L);
        d.setFilename("hello.txt");
        d.setContentType("text/plain");
        d.setSize(12);
        d.setUploadedAt(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        return d;
    }

    @Nested
    class UploadTests {

        @Test
        @DisplayName("POST /api/documents -> 201 with Location + JSON")
        void upload_success() throws Exception {
            Mockito.when(documentManager.upload(any())).thenReturn(sampleDoc());

            MockMultipartFile file = new MockMultipartFile(
                    "file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello".getBytes());

            mvc.perform(multipart("/api/documents").file(file))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/documents/42"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.filename").value("hello.txt"))
                    .andExpect(jsonPath("$.contentType").value("text/plain"))
                    .andExpect(jsonPath("$.size").value(12));
        }

        @Test
        @DisplayName("POST /api/documents with empty file -> 400")
        void upload_emptyFile() throws Exception {
            MockMultipartFile empty = new MockMultipartFile(
                    "file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

            mvc.perform(multipart("/api/documents").file(empty))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetByIdTests {

        @Test
        @DisplayName("GET /api/documents/{id} -> 200 with JSON body")
        void get_found() throws Exception {
            Mockito.when(documentManager.getById(anyLong())).thenReturn(sampleDoc());

            mvc.perform(get("/api/documents/{id}", 42))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.filename").value("hello.txt"));
        }

        @Test
        @DisplayName("GET /api/documents/{id} (not found) -> 404")
        void get_notFound() throws Exception {
            Mockito.when(documentManager.getById(anyLong())).thenReturn(null);

            mvc.perform(get("/api/documents/{id}", 999))
                    .andExpect(status().isNotFound());
        }
    }
}