package com.example.courslens.service;

import com.example.courslens.api.Dtos.*;
import com.example.courslens.repository.ChunkRepository;
import com.example.courslens.repository.ChunkRepository.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionServiceTest {
 @Test void retrievesTopKWithPgvectorAndPreservesCitationMetadata(){
  ChunkRepository repository=mock(ChunkRepository.class); EmbeddingService embeddings=mock(EmbeddingService.class); GeminiService gemini=mock(GeminiService.class);
  double[] vector=new double[768]; vector[0]=0.5; when(embeddings.embed("what is a database")).thenReturn(vector); when(gemini.enabled()).thenReturn(false);
  UUID documentId=UUID.randomUUID(),pageId=UUID.randomUUID(),chunkId=UUID.randomUUID();
  RetrievedChunk hit=new RetrievedChunk(){public UUID getChunkId(){return chunkId;}public UUID getPageId(){return pageId;}public UUID getDocumentId(){return documentId;}public String getDocumentName(){return "Chapter_1.pdf";}public int getPageNumber(){return 1;}public String getText(){return "Database: A collection of related data.";}public boolean getHandwritten(){return false;}public double getDistance(){return .1;}};
  when(repository.findNearestByCosineDistance(anyString(),eq(8))).thenReturn(List.of(hit));
  AskResponse response=new QuestionService(repository,embeddings,gemini).ask(new AskRequest("what is a database",null));
  ArgumentCaptor<String> query=ArgumentCaptor.forClass(String.class);verify(repository).findNearestByCosineDistance(query.capture(),eq(8));
  assertTrue(query.getValue().startsWith("[0.5,")); assertEquals(768,query.getValue().split(",").length); assertEquals("ANSWERED",response.status()); assertEquals(documentId,response.sources().get(0).documentId()); assertEquals(chunkId,response.sources().get(0).chunkId());
 }

 @Test void refusesWhenPgvectorReturnsNoEvidence(){
  ChunkRepository repository=mock(ChunkRepository.class); EmbeddingService embeddings=mock(EmbeddingService.class); GeminiService gemini=mock(GeminiService.class);
  when(embeddings.embed("missing topic")).thenReturn(new double[768]); when(repository.findNearestByCosineDistance(anyString(),eq(8))).thenReturn(List.of());
  AskResponse response=new QuestionService(repository,embeddings,gemini).ask(new AskRequest("missing topic",null));
  assertEquals("NOT_COVERED",response.status()); verifyNoInteractions(gemini);
 }
}
