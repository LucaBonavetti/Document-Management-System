# Document Management System (Paperless)

A teaching project that demonstrates a layered Spring Boot application with
code-first REST endpoints, OpenAPI documentation, a PostgreSQL DAL via JPA,
a Business Layer (BL) with MapStruct mappers, and tests (unit + repository).
It now includes a simple **HTML5/CSS/JS web frontend** served by **NGINX**.

> Service port: **8081** (backend)  
> Web UI: **http://localhost** (NGINX)  
> Database: **PostgreSQL 16** (Docker Compose)

---

## Quickstart

### Run everything (web + app + db) via Docker Compose

```bash
docker compose up -d --build
# Web UI: http://localhost:8080
# API:    http://localhost:8081 (also proxied under /api by NGINX)