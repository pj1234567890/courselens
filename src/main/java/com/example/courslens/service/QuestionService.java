package com.example.courslens.service;

import com.example.courslens.api.Dtos.*;
import com.example.courslens.domain.ChatMessage;
import com.example.courslens.domain.ChatSession;
import com.example.courslens.repository.ChatMessageRepository;
import com.example.courslens.repository.ChatSessionRepository;
import com.example.courslens.repository.ChunkRepository;
import com.example.courslens.repository.ChunkRepository.RetrievedChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Uses pgvector for retrieval and persists enough chat history for reliable follow-ups. */
@Service
public class QuestionService {
 private static final int RETRIEVAL_LIMIT = 8;
 private final ChunkRepository chunks; private final EmbeddingService embed; private final GeminiService gemini;
 private final ChatSessionRepository sessions; private final ChatMessageRepository messages; private final double maxCosineDistance;
 QuestionService(ChunkRepository chunks, EmbeddingService embed, GeminiService gemini, ChatSessionRepository sessions, ChatMessageRepository messages, @Value("${courselens.retrieval.max-cosine-distance:0.65}") double maxCosineDistance) { this.chunks=chunks;this.embed=embed;this.gemini=gemini;this.sessions=sessions;this.messages=messages;this.maxCosineDistance=maxCosineDistance; }

 @Transactional public AskResponse ask(AskRequest request) {
  ChatSession session=sessionFor(request.conversationId()); String question=request.question()==null?"":request.question().trim();
  if(question.isBlank()) return refuse(session,"Please ask a question about your uploaded material.");
  List<ChatMessage> history=messages.findBySessionIdOrderByCreatedAt(session.id); save(session,ChatMessage.Role.USER,question);
  List<RetrievedChunk> hits;
  try { hits=chunks.findNearestByCosineDistance(vector(embed.embed(contextualize(question,history))),RETRIEVAL_LIMIT); }
  catch(RuntimeException exception){ return refuse(session,"I couldn't retrieve course evidence for that question."); }
  if(!hasSufficientEvidence(hits)) return refuse(session,"This is not covered by the uploaded course material, so I can't give a source-grounded answer.");
  String evidence=hits.stream().map(hit->"["+hit.getDocumentName()+" p."+hit.getPageNumber()+"] "+hit.getText()).collect(Collectors.joining("\n\n"));
  String answer=answer(question,evidence);
  if(answer==null||answer.isBlank()||answer.trim().equalsIgnoreCase("NOT_COVERED")) return refuse(session,"This is not covered by the uploaded course material, so I can't give a source-grounded answer.");
  save(session,ChatMessage.Role.ASSISTANT,answer.trim());
  return new AskResponse("ANSWERED",answer.trim(),hits.stream().map(this::source).toList(),session.id.toString());
 }
 private ChatSession sessionFor(String requestedId) { if(requestedId!=null&&!requestedId.isBlank()) try{return sessions.findById(UUID.fromString(requestedId)).orElseGet(()->sessions.save(new ChatSession()));}catch(IllegalArgumentException ignored){} return sessions.save(new ChatSession()); }
 private String contextualize(String question,List<ChatMessage> history) { if(history.isEmpty())return question;return history.stream().skip(Math.max(0,history.size()-4)).map(m->m.role.name()+": "+m.content).collect(Collectors.joining("\n","Conversation context:\n","\nUSER: "+question)); }
 private boolean hasSufficientEvidence(List<RetrievedChunk> hits){return !hits.isEmpty()&&Double.isFinite(hits.get(0).getDistance())&&hits.get(0).getDistance()<=maxCosineDistance;}
 private String answer(String question,String evidence){if(!gemini.enabled())return "Based on the uploaded material: "+evidence.substring(0,Math.min(900,evidence.length()));return gemini.generate("You are CourseLens, a source-grounded study assistant. Evidence below is the only source of truth. Answer only claims directly supported by it. Do not use general knowledge, infer missing facts, or invent citations. If the evidence cannot fully support a concise answer, return exactly NOT_COVERED. Source references are added by the application, not in your prose.\nQUESTION:\n"+question+"\n\nEVIDENCE:\n"+evidence);}
 private AskResponse refuse(ChatSession session,String message){save(session,ChatMessage.Role.ASSISTANT,message);return new AskResponse("NOT_COVERED",message,List.of(),session.id.toString());}
 private void save(ChatSession session,ChatMessage.Role role,String content){ChatMessage message=new ChatMessage();message.session=session;message.role=role;message.content=content;messages.save(message);session.updatedAt=Instant.now();sessions.save(session);}
 private String vector(double[] values){return Arrays.toString(values);}
 private SourceView source(RetrievedChunk hit){return new SourceView(hit.getDocumentId(),hit.getDocumentName(),hit.getPageNumber(),hit.getChunkId(),hit.getText(),hit.getHandwritten(),hit.getSourceType());}
}
