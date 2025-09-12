package paperless.paperless.web;


import paperless.paperless.domain.Document;
import paperless.paperless.repository.DocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DocumentRepository repository;

    @Test
    void fullCrudFlow() throws Exception {

        // --- Create
        String createJson = """
            {"title":"Doc A","contentText":"alpha"}
        """;
        String createResp = mvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = mapper.readTree(createResp);
        String id = created.get("id").asText();
        assertThat(id).isNotBlank();

        // --- Get by id
        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Doc A"))
                .andExpect(jsonPath("$.contentText").value("alpha"));

        // --- Update
        String updateJson = """
            {"title":"Doc A (updated)","contentText":"beta"}
        """;
        mvc.perform(put("/api/documents/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Doc A (updated)"))
                .andExpect(jsonPath("$.contentText").value("beta"));

        // --- Delete
        mvc.perform(delete("/api/documents/{id}", id))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound());

        // Repo sanity
        assertThat(repository.findById(UUID.fromString(id))).isEmpty();
    }

    @Test
    @DisplayName("GET list returns all created documents")
    void listReturnsAll() throws Exception {
        repository.save(new Document("A", "x"));
        repository.save(new Document("B", "y"));

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}