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
    void crudFlow() throws Exception {

        // --- Create
        String create = """
            {"title":"title","contentText":"contentText"}
        """;
        String resp = mvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = mapper.readTree(resp);
        String id = created.get("id").asText();
        assertThat(id).isNotBlank();

        // --- Get by id
        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.contentText").value("contentText"));

        // --- Update
        String update = """
            {"title":"title (updated)","contentText":"contentText"}
        """;
        mvc.perform(put("/api/documents/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("title (updated)"))
                .andExpect(jsonPath("$.contentText").value("contentText"));

        // --- Delete
        mvc.perform(delete("/api/documents/{id}", id))
                .andExpect(status().isNoContent());

        // Verify gone
        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound());

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