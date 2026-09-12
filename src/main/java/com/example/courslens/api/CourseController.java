package com.example.courslens.api;
import com.example.courslens.api.Dtos.*; import com.example.courslens.domain.*; import com.example.courslens.repository.*; import com.example.courslens.service.*; import org.springframework.core.io.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.nio.file.*; import java.util.*;
@RestController @RequestMapping("/api") @CrossOrigin public class CourseController {
 private final IngestionService ingestion;private final DocumentRepository docs;private final PageRepository pages;private final ChunkRepository chunks;private final QuestionService questions;
 CourseController(IngestionService i,DocumentRepository d,PageRepository p,ChunkRepository c,QuestionService q){ingestion=i;docs=d;pages=p;chunks=c;questions=q;}
 @GetMapping("/documents") List<DocumentView> list(){return docs.findAll().stream().map(this::view).toList();}
 @GetMapping("/documents/{id}") DocumentView one(@PathVariable UUID id){return view(docs.findById(id).orElseThrow());}
 @GetMapping("/documents/{id}/pages") List<PageView> pages(@PathVariable UUID id){if(!docs.existsById(id))throw new NoSuchElementException();return pages.findByDocumentIdOrderByPageNumber(id).stream().map(p->new PageView(p.id,p.pageNumber,p.extractedText,p.handwritten)).toList();}
 @GetMapping("/documents/{id}/chunks") List<ChunkView> chunks(@PathVariable UUID id){if(!docs.existsById(id))throw new NoSuchElementException();return chunks.findByDocumentIdWithPage(id).stream().map(c->new ChunkView(c.id,c.page.id,c.page.pageNumber,c.chunkIndex,c.text,c.embedding==null?0:c.embedding.length)).toList();}
 @PostMapping(value="/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) ResponseEntity<DocumentView> upload(@RequestParam("file")MultipartFile file){Document document=ingestion.ingest(file);return ResponseEntity.status("FAILED".equals(document.status)?HttpStatus.UNPROCESSABLE_ENTITY:HttpStatus.CREATED).body(view(document));}
 @PostMapping("/questions") AskResponse ask(@RequestBody AskRequest request){return questions.ask(request);}
 @GetMapping("/documents/{id}/pages/{number}/source") ResponseEntity<Resource> source(@PathVariable UUID id,@PathVariable int number)throws Exception {Page page=pages.findWithDocumentByDocumentIdAndPageNumber(id,number).orElseThrow();Path p=Path.of(page.imagePath==null?page.document.storagePath:page.imagePath);return ResponseEntity.ok().header("X-CourseLens-Source-Number",Integer.toString(page.pageNumber)).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(p));}
 @ExceptionHandler({IllegalArgumentException.class}) ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));}
 private DocumentView view(Document d){return new DocumentView(d.id,d.filename,d.type,d.status,d.pageCount,d.createdAt,d.processingError);}
}
