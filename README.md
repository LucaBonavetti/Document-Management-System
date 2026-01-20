# Document Management System (Paperless)

This semester project implements a document management system for archiving documents in an object-based file store. Uploaded documents are automatically processed using OCR via a queue-based worker service, summarized using a Gen-AI service, and enriched with tags for structured organization.

The system supports full-text search through Elasticsearch, allowing users to quickly find documents based on their content. The overall architecture is containerized and follows a microservice-style design, ensuring scalability, modularity, and clear separation of responsibilities between services.

## Project Architecture

The project consists of the following main modules:

- **REST Server**
  Handles document upload, metadata storage, summaries, tags, search endpoints, and communication with queues.

- **OCR Worker**
  Receives OCR jobs from RabbitMQ, downloads documents from MinIO, performs OCR using Tesseract, stores extracted text back to MinIO, and publishes results.

- **GenAI Worker**
  Receives OCR text references, sends text to Google Gemini, and returns generated summaries which are stored in the database.

- **Web UI (Nginx)**
  Simple frontend for uploading documents, viewing documents, summaries, tags, and performing search.

- **PostgreSQL**
  Stores document metadata, summaries, and tags.

- **RabbitMQ**
  Message broker for communication between REST server and workers.

- **MinIO**
  Object storage for original documents and OCR text files.

- **Elasticsearch**
  Stores indexed OCR text for document search.

## Main Workflow

1. User uploads a document via the Web UI.
2. REST server stores metadata in PostgreSQL and the file in MinIO.
3. REST server sends an OCR job to RabbitMQ.
4. OCR worker downloads the file, performs OCR, stores text in MinIO, and publishes a result.
5. GenAI worker generates a summary using Gemini and sends it back.
6. REST server stores the summary in PostgreSQL.
7. OCR text is indexed in Elasticsearch for search.
8. User can search documents via the Web UI.

## Additional Use Case: Tags

An additional use case was implemented using tags:
- Users can add tags to documents,
- Retrieve tags for a document,
- Use tags as additional metadata for organization.
  Tags are stored in separate database entities and linked to documents using a relation table. This fulfills the requirement for an additional use case with additional entities.

## Sprint Coverage

### Sprint 1
- REST server,
  to PostgreSQL integration,
  the repository pattern,
  tests,
  and Docker setup.

### Sprint 2
- Web UI with Nginx,
  gfrontend connected to REST server.

### Sprint 3
- RabbitMQ integration and OCR job publishing.

### Sprint 4
- OCR worker service with Tesseract and MinIO storage.

### Sprint 5
- GenAI worker with Google Gemini summary generation.

### Sprint 6
- Elasticsearch indexing and document search functionality. Additional use case: Tags

### Sprint 7
- Integration test for document upload use case. Batch processing was not implemented

## Integration Test

An integration test is provided to demonstrate the full document upload workflow.

## Verifies:
- Document upload
- Database persistence
- Proper REST response

The integration test can be executed using Maven in the REST module.

## Example:
```
mvn clean verify
```

The test must complete successfully without errors.

## Running the Project

The entire system is started using Docker Compose.

**From the project root directory:**
```
docker compose build
docker compose up
```

**After startup:**
- Web UI: [http://localhost](http://localhost)
- REST API: [http://localhost:8081](http://localhost:8081)
- RabbitMQ UI: [http://localhost:9093](http://localhost:9093)
- MinIO Console: [http://localhost:9090](http://localhost:9090)
- Elasticsearch: [http://localhost:9200](http://localhost:9200)
- Kibana: [http://localhost:5601](http://localhost:5601)

## Search Function

Documents can be searched by OCR text using Elasticsearch.
Searching for a word that appears in the document content will return the corresponding document.

### Example:
Uploading `HelloWorld.pdf` and searching for "Hello" will return the document.

## Notes
- All services are containerized.
- The system starts fully using docker compose.
- Logging and exception handling are implemented in all critical paths.
- Unit tests and integration tests are provided.
- Batch processing from Sprint 7 was not implemented.