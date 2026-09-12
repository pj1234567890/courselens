package com.example.courslens.api;
import java.util.*;
public final class Dtos { private Dtos(){}
 public record DocumentView(UUID id,String filename,String type,String status,int pageCount,java.time.Instant createdAt,String error){}
 public record PageView(UUID id,int pageNumber,String extractedText,boolean handwritten){}
 public record ChunkView(UUID id,UUID pageId,int pageNumber,int chunkIndex,String text,int embeddingDimension){}
 public record SourceView(UUID documentId,String documentName,int pageNumber,UUID chunkId,String evidence,boolean handwritten){}
 public record AskRequest(String question,String conversationId){}
 public record AskResponse(String status,String answer,List<SourceView> sources,String conversationId){}
}
