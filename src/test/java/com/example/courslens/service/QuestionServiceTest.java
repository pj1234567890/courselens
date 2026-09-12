package com.example.courslens.service;

import com.example.courslens.api.Dtos.*;
import com.example.courslens.repository.ChunkRepository;
import com.example.courslens.repository.ChunkRepository.RetrievedChunk;
import com.example.courslens.repository.ChatMessageRepository;
import com.example.courslens.repository.ChatSessionRepository;
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
  RetrievedChunk hit=new RetrievedChunk(){public UUID getChunkId(){return chunkId;}public UUID getPageId(){return pageId;}public UUID getDocumentId(){return documentId;}public String getDocumentName(){return "Chapter_1.pdf";}public int getPageNumber(){return 1;}public String getText(){return "Database: A collection of related data.";}public boolean getHandwritten(){return false;}public String getSourceType(){return "PDF_TEXT";}public double getDistance(){return .1;}};
  when(repository.findNearestByCosineDistance(anyString(),eq(8))).thenReturn(List.of(hit));
  AskResponse response=service(repository,embeddings,gemini).ask(new AskRequest("what is a database",null));
  ArgumentCaptor<String> query=ArgumentCaptor.forClass(String.class);verify(repository).findNearestByCosineDistance(query.capture(),eq(8));
  assertTrue(query.getValue().startsWith("[0.5,")); assertEquals(768,query.getValue().split(",").length); assertEquals("ANSWERED",response.status()); assertEquals(documentId,response.sources().get(0).documentId()); assertEquals(chunkId,response.sources().get(0).chunkId());
 }

 @Test void refusesWhenPgvectorReturnsNoEvidence(){
  ChunkRepository repository=mock(ChunkRepository.class); EmbeddingService embeddings=mock(EmbeddingService.class); GeminiService gemini=mock(GeminiService.class);
  when(embeddings.embed("missing topic")).thenReturn(new double[768]); when(repository.findNearestByCosineDistance(anyString(),eq(8))).thenReturn(List.of());
  AskResponse response=service(repository,embeddings,gemini).ask(new AskRequest("missing topic",null));
 assertEquals("NOT_COVERED",response.status()); verifyNoInteractions(gemini);
 }
 private QuestionService service(ChunkRepository chunks,EmbeddingService embeddings,GeminiService gemini){
  ChatSessionRepository sessions=mock(ChatSessionRepository.class); ChatMessageRepository messages=mock(ChatMessageRepository.class);
  when(sessions.save(any())).thenAnswer(i->{var session=i.getArgument(0,com.example.courslens.domain.ChatSession.class);if(session.id==null)session.id=UUID.randomUUID();return session;});
  when(messages.findBySessionIdOrderByCreatedAt(any())).thenReturn(List.of());
  return new QuestionService(chunks,embeddings,gemini,sessions,messages,.65);
 }
}
