# CourseLens

Run `mvnw.cmd spring-boot:run`, then open http://localhost:8080. Set `GEMINI_API_KEY` for multimodal handwritten-page transcription and grounded Gemini synthesis. For PostgreSQL, set `SPRING_PROFILES_ACTIVE=postgres`; Flyway creates and validates the CourseLens schema automatically.

The API is `POST /api/documents` (multipart `file`), `GET /api/documents`, and `POST /api/questions`. Answer results explicitly expose `ANSWERED` or `NOT_COVERED` and fully traceable sources.
