package com.example.courslens.repository;

import com.example.courslens.domain.Chunk;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChunkRepository extends JpaRepository<Chunk,UUID>{
 @Query("select c from Chunk c join fetch c.page p where p.document.id=:documentId order by p.pageNumber,c.chunkIndex")
 List<Chunk> findByDocumentIdWithPage(UUID documentId);

 /** Native pgvector cosine search. The ORDER BY expression matches the HNSW index operator class. */
 @Query(value="""
   SELECT c.id AS "chunkId", p.id AS "pageId", d.id AS "documentId",
          d.original_filename AS "documentName", p.page_number AS "pageNumber",
          c.chunk_text AS text, p.handwritten AS handwritten, p.source_type AS "sourceType",
          c.embedding <=> CAST(:queryVector AS vector) AS distance
   FROM chunks c
   JOIN document_pages p ON p.id = c.document_page_id
   JOIN documents d ON d.id = p.document_id
   WHERE c.embedding IS NOT NULL AND d.processing_status = 'COMPLETED'
   ORDER BY c.embedding <=> CAST(:queryVector AS vector)
   LIMIT :limit
   """,nativeQuery=true)
 List<RetrievedChunk> findNearestByCosineDistance(@Param("queryVector") String queryVector,@Param("limit") int limit);

 interface RetrievedChunk {
  UUID getChunkId(); UUID getPageId(); UUID getDocumentId(); String getDocumentName();
  int getPageNumber(); String getText(); boolean getHandwritten(); String getSourceType(); double getDistance();
 }
}
