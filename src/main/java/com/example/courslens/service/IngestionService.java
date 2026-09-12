package com.example.courslens.service;

import com.example.courslens.domain.*;
import com.example.courslens.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@Service
public class IngestionService {

 private final DocumentRepository docs;
 private final List<DocumentExtractor> extractors;
 private final ChunkingService chunking;
 private final EmbeddingService embeddings;
 private final Path root;

 IngestionService(
         DocumentRepository d,
         List<DocumentExtractor> e,
         ChunkingService c,
         EmbeddingService em,
         @Value("${courselens.storage.upload-dir}") String dir) {

  docs = d;
  extractors = e;
  chunking = c;
  embeddings = em;
  root = Paths.get(dir).toAbsolutePath();
 }

 public Document ingest(MultipartFile file) {

  validate(file);

  Document doc = new Document();

  doc.filename = safe(file.getOriginalFilename());
  doc.type = type(doc.filename);

  try {

   Files.createDirectories(root);

   Path stored = root.resolve(
           UUID.randomUUID() + "-" + doc.filename
   );

   Files.copy(
           file.getInputStream(),
           stored,
           StandardCopyOption.REPLACE_EXISTING
   );

   doc.storagePath = stored.toString();
   doc.status = "PROCESSING";

   docs.save(doc);

   String documentType = doc.type;

   DocumentExtractor extractor =
           extractors.stream()
                   .filter(x -> x.supports(documentType))
                   .findFirst()
                   .orElseThrow(() ->
                           new IllegalArgumentException(
                                   "No extractor available for " + documentType
                           )
                   );

   List<ExtractedPage> pages =
           extractor.extract(
                   stored,
                   file.getContentType()
           );

   if (pages.isEmpty()) {
    throw new IllegalArgumentException(
            "No readable pages found"
    );
   }

   for (ExtractedPage extracted : pages) {
    addPage(doc, extracted);
   }

   doc.pageCount = doc.pages.size();
   doc.status = "COMPLETED";
   doc.processingError = null;

   return docs.save(doc);

  } catch (Exception ex) {

   ex.printStackTrace();

   doc.status = "FAILED";
   doc.processingError = clean(ex);

   return docs.save(doc);
  }
 }

 private void addPage(
         Document doc,
         ExtractedPage source) {

  Page page = new Page();

  page.document = doc;
  page.pageNumber = source.number();
  page.extractedText = source.text();
  page.sourceType = source.sourceType();

  /*
   * The original implementation pointed this to the entire
   * uploaded PDF. That isn't actually a page image.
   *
   * Exact page-image storage can be added later when the
   * source viewer is implemented.
   */
  // An uploaded image is already a single-page original and can be shown safely.
  page.imagePath = "IMAGE".equals(source.sourceType()) ? doc.storagePath : null;

  page.handwritten = source.handwritten();

  int index = 0;

  String pageText = source.text();

  if (pageText == null || pageText.isBlank()) {
   throw new IllegalArgumentException(
           "Page " + source.number() +
                   " contains no readable content"
   );
  }

  List<String> chunks = chunking.chunk(pageText);

  if (chunks.isEmpty()) {
   throw new IllegalArgumentException(
           "Page " + source.number() +
                   " could not be chunked"
   );
  }

  for (String text : chunks) {

   if (text == null || text.isBlank()) {
    continue;
   }

   Chunk chunk = new Chunk();

   chunk.page = page;
   chunk.chunkIndex = index++;
   chunk.text = text;

   double[] embedding =
           embeddings.embed(text);

   embeddings.validate(embedding);

   chunk.embedding =
           embeddings.toFloatArray(embedding);

   page.chunks.add(chunk);
  }

  if (page.chunks.isEmpty()) {
   throw new IllegalArgumentException(
           "Page " + source.number() +
                   " produced no embeddable chunks"
   );
  }

  doc.pages.add(page);
 }

 private void validate(MultipartFile file) {

  if (file == null || file.isEmpty()) {
   throw new IllegalArgumentException(
           "Uploaded file is empty"
   );
  }

  if (file.getSize() > 30L * 1024 * 1024) {
   throw new IllegalArgumentException(
           "File exceeds 30 MB limit"
   );
  }

  String name = file.getOriginalFilename();

  if (name == null ||
          name.isBlank() ||
          type(name).equals("UNSUPPORTED")) {

   throw new IllegalArgumentException(
           "Unsupported file type. " +
                   "Use PDF, PPTX, TXT, MD, PNG, JPG, or JPEG."
   );
  }
 }

 private String type(String name) {

  String lower =
          name.toLowerCase(Locale.ROOT);

  if (lower.endsWith(".pdf")) {
   return "PDF";
  }

  if (lower.endsWith(".pptx")) {
   return "PPTX";
  }

  if (lower.endsWith(".txt")) {
   return "TEXT";
  }

  if (lower.endsWith(".md")) {
   return "MARKDOWN";
  }

  if (lower.matches(".*\\.(png|jpg|jpeg)$")) {
   return "IMAGE";
  }

  return "UNSUPPORTED";
 }

 private String safe(String name) {

  return name.replaceAll(
          "[^a-zA-Z0-9._-]",
          "_"
  );
 }

 private String clean(Exception e) {

  String message = e.getMessage();

  if (message == null || message.isBlank()) {
   return "Processing failed";
  }

  return message.substring(
          0,
          Math.min(500, message.length())
  );
 }
}
