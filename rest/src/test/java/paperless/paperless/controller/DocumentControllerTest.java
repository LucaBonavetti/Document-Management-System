package paperless.paperless.controller;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import paperless.paperless.bl.model.BlDocument;
import paperless.paperless.bl.service.DocumentService;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @SuppressWarnings("removal")
    @MockBean
    private DocumentService documentService;

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
        @DisplayName("POST /api/documents -> 201 Created with Location and JSON body")
        void upload_success() throws Exception {
            Mockito.when(documentService.saveDocument(anyString(), anyString(), anyLong(), any(byte[].class)))
                    .thenReturn(sampleDoc());

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
        @DisplayName("POST /api/documents with empty file -> 400 (controller-level)")
        void upload_emptyFile() throws Exception {
            MockMultipartFile empty = new MockMultipartFile(
                    "file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

            mvc.perform(multipart("/api/documents").file(empty))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/documents with BL validation error -> 400 via @ControllerAdvice")
        void upload_blValidationError() throws Exception {
            Mockito.when(documentService.saveDocument(anyString(), anyString(), anyLong(), any(byte[].class)))
                    .thenThrow(new ConstraintViolationException("invalid", Collections.emptySet()));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "bad.txt", MediaType.TEXT_PLAIN_VALUE, "x".getBytes());

            mvc.perform(multipart("/api/documents").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    class GetByIdTests {

        @Test
        @DisplayName("GET /api/documents/{id} -> 200 OK with JSON body")
        void get_found() throws Exception {
            Mockito.when(documentService.getById(anyLong())).thenReturn(sampleDoc());

            mvc.perform(get("/api/documents/{id}", 42))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.filename").value("hello.txt"));
        }

        @Test
        @DisplayName("GET /api/documents/{id} (not found) -> 404 Not Found")
        void get_notFound() throws Exception {
            Mockito.when(documentService.getById(anyLong())).thenReturn(null);

            mvc.perform(get("/api/documents/{id}", 999))
                    .andExpect(status().isNotFound());
        }
    }
}