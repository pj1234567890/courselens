package com.example.courslens.service;

import com.example.courslens.domain.Document;
import com.example.courslens.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngestionServiceTest {
 @Test void returnsFailedDocumentInsteadOf500WhenPostProcessingSaveFails() throws Exception {
  DocumentRepository docs = mock(DocumentRepository.class);
  ChunkingService chunking = mock(ChunkingService.class);
  EmbeddingService embeddings = mock(EmbeddingService.class);
  AtomicInteger saveCalls = new AtomicInteger();
  when(docs.save(any(Document.class))).thenAnswer(invocation -> {
   Document current = invocation.getArgument(0);
   int attempt = saveCalls.incrementAndGet();
   if (attempt == 1) {
    if (current.id == null) current.id = UUID.randomUUID();
    return current;
   }
   if (attempt == 2) {
    throw new RuntimeException("Table \"CHUNKS\" not found");
   }
   assertEquals("FAILED", current.status);
   assertEquals(0, current.pageCount);
   assertTrue(current.pages.isEmpty());
   return current;
  });
  when(chunking.chunk(anyString())).thenReturn(List.of("embeddable chunk"));
  when(embeddings.embed(anyString())).thenReturn(new double[768]);
  when(embeddings.toFloatArray(any(double[].class))).thenReturn(new float[768]);
  DocumentExtractor extractor = new DocumentExtractor() {
   @Override public boolean supports(String type) { return "TEXT".equals(type); }
   @Override public List<ExtractedPage> extract(Path file, String mime) { return List.of(new ExtractedPage(1, "page text", false, "TEXT")); }
  };

  Path uploadDir = Files.createTempDirectory("courselens-ingestion-test");
  try {
   IngestionService service = new IngestionService(docs, List.of(extractor), chunking, embeddings, uploadDir.toString());
   MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "example".getBytes(StandardCharsets.UTF_8));
   Document result = service.ingest(file);

   assertEquals("FAILED", result.status);
   assertEquals("Table \"CHUNKS\" not found", result.processingError);
   assertEquals(0, result.pageCount);
   assertTrue(result.pages.isEmpty());
   verify(docs, times(3)).save(any(Document.class));
  } finally {
   Files.walk(uploadDir).sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
    try {
     Files.deleteIfExists(path);
    } catch (Exception ignored) {
    }
   });
  }
 }
}
