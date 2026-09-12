package com.example.courslens.service;

import com.example.courslens.api.Dtos.*;
import com.example.courslens.repository.ChunkRepository;
import com.example.courslens.repository.ChunkRepository.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

 private final ChunkRepository chunks;
 private final EmbeddingService embed;
 private final GeminiService gemini;

 private final Map<String, List<String>> memory =
         new ConcurrentHashMap<>();

 QuestionService(
         ChunkRepository c,
         EmbeddingService e,
         GeminiService g
 ) {
  chunks = c;
  embed = e;
  gemini = g;
 }

 public AskResponse ask(AskRequest req) {

  String cid = Optional.ofNullable(req.conversationId())
          .filter(s -> !s.isBlank())
          .orElse(UUID.randomUUID().toString());

  String question = req.question() == null
          ? ""
          : req.question().trim();

  if (question.isBlank()) {

   return new AskResponse(
           "NOT_COVERED",
           "Please ask a question about your uploaded material.",
           List.of(),
           cid
   );
  }

  String contextual = question;

  List<String> prior =
          memory.getOrDefault(cid, List.of());

  if (question.matches("(?i).*(that|it|they|this).*?")
          && !prior.isEmpty()) {

   contextual =
           prior.get(prior.size() - 1)
                   + "\nFollow-up: "
                   + question;
  }

  // ============================================================
  // RETRIEVAL DEBUG
  // ============================================================

  System.out.println();
  System.out.println(
          "========== COURSELENS RETRIEVAL DEBUG =========="
  );

  System.out.println(
          "QUESTION: " + question
  );

  System.out.println(
          "CONTEXTUAL QUESTION: " + contextual
  );

  // ============================================================
  // GENERATE QUERY EMBEDDING
  // ============================================================

  double[] queryEmbedding;

  try {

   queryEmbedding = embed.embed(contextual);

   System.out.println(
           "EMBEDDING SUCCESS"
   );

   System.out.println(
           "EMBEDDING DIMENSION: "
                   + queryEmbedding.length
   );

   if (queryEmbedding.length > 0) {

    int count =
            Math.min(5, queryEmbedding.length);

    System.out.println(
            "FIRST 5 EMBEDDING VALUES: "
                    + Arrays.toString(
                    Arrays.copyOf(
                            queryEmbedding,
                            count
                    )
            )
    );
   }

  } catch (Exception e) {

   System.out.println(
           "EMBEDDING FAILED"
   );

   System.out.println(
           "ERROR: " + e.getMessage()
   );

   e.printStackTrace();

   return refuse(cid);
  }

  // ============================================================
  // CONVERT EMBEDDING TO PGVECTOR LITERAL
  // ============================================================

  String queryVector =
          toPgVectorLiteral(queryEmbedding);

  System.out.println(
          "VECTOR LITERAL START: "
                  + queryVector.substring(
                  0,
                  Math.min(
                          100,
                          queryVector.length()
                  )
          )
  );

  System.out.println(
          "VECTOR LITERAL LENGTH: "
                  + queryVector.length()
  );

  // ============================================================
  // PGVECTOR RETRIEVAL
  // ============================================================

  List<RetrievedChunk> hits;

  try {

   hits =
           chunks.findNearestByCosineDistance(
                   queryVector,
                   8
           );

   System.out.println(
           "PGVECTOR RETRIEVAL SUCCESS"
   );

   System.out.println(
           "NUMBER OF HITS: "
                   + hits.size()
   );

  } catch (Exception e) {

   System.out.println(
           "PGVECTOR RETRIEVAL FAILED"
   );

   System.out.println(
           "ERROR: " + e.getMessage()
   );

   e.printStackTrace();

   return refuse(cid);
  }

  // ============================================================
  // PRINT RETRIEVED CHUNKS
  // ============================================================

  System.out.println();
  System.out.println(
          "----- RETRIEVED CHUNKS -----"
  );

  for (int i = 0; i < hits.size(); i++) {

   RetrievedChunk h =
           hits.get(i);

   String text =
           h.getText();

   String preview =
           text == null
                   ? ""
                   : text.substring(
                   0,
                   Math.min(
                           200,
                           text.length()
                   )
           );

   System.out.println(
           "HIT #" + (i + 1)
                   + " | document="
                   + h.getDocumentName()
                   + " | page="
                   + h.getPageNumber()
                   + " | distance="
                   + h.getDistance()
   );

   System.out.println(
           "TEXT: " + preview
   );

   System.out.println();
  }

  System.out.println(
          "========== END RETRIEVAL DEBUG =========="
  );

  // ============================================================
  // NO RETRIEVAL RESULTS
  // ============================================================

  if (hits.isEmpty()) {

   System.out.println(
           "RESULT: NO RETRIEVED SOURCES"
   );

   return refuse(cid);
  }

  // ============================================================
  // BUILD SOURCES
  // ============================================================

  List<SourceView> sources =
          hits.stream()
                  .map(this::source)
                  .toList();

  System.out.println();
  System.out.println(
          "SOURCE COUNT: " + sources.size()
  );

  // ============================================================
  // BUILD EVIDENCE
  // ============================================================

  String evidence =
          hits.stream()
                  .map(h ->
                          "["
                                  + h.getDocumentName()
                                  + " p."
                                  + h.getPageNumber()
                                  + "] "
                                  + h.getText()
                  )
                  .collect(
                          Collectors.joining(
                                  "\n\n"
                          )
                  );

  System.out.println();
  System.out.println(
          "========== EVIDENCE DEBUG =========="
  );

  System.out.println(
          "EVIDENCE LENGTH: "
                  + evidence.length()
  );

  System.out.println(
          "EVIDENCE:"
  );

  System.out.println(
          evidence
  );

  System.out.println(
          "========== END EVIDENCE DEBUG =========="
  );

  // ============================================================
  // GEMINI STATUS
  // ============================================================

  boolean geminiEnabled =
          gemini.enabled();

  System.out.println();
  System.out.println(
          "GEMINI ENABLED: "
                  + geminiEnabled
  );

  // ============================================================
  // GENERATE ANSWER
  // ============================================================

  String answer;

  try {

   answer =
           answer(
                   question,
                   evidence
           );

  } catch (Exception e) {

   System.out.println();
   System.out.println(
           "ANSWER GENERATION FAILED"
   );

   System.out.println(
           "ERROR: " + e.getMessage()
   );

   e.printStackTrace();

   return refuse(cid);
  }

  // ============================================================
  // ANSWER DEBUG
  // ============================================================

  System.out.println();
  System.out.println(
          "========== ANSWER DEBUG =========="
  );

  System.out.println(
          "ANSWER LENGTH: "
                  + (answer == null
                  ? 0
                  : answer.length())
  );

  System.out.println(
          "GENERATED ANSWER:"
  );

  System.out.println(
          answer
  );

  System.out.println(
          "========== END ANSWER DEBUG =========="
  );

  // ============================================================
  // FINAL ANSWER VALIDATION
  // ============================================================

  if (answer == null
          || answer.isBlank()
          || answer.contains("NOT_COVERED")) {

   System.out.println();
   System.out.println(
           "RESULT: NOT_COVERED"
   );

   System.out.println(
           "REASON: Answer was blank or contained NOT_COVERED"
   );

   return refuse(cid);
  }

  // ============================================================
  // SAVE CONVERSATION MEMORY
  // ============================================================

  memory.computeIfAbsent(
          cid,
          k -> new ArrayList<>()
  ).add(question);

  memory.get(cid).add(answer);

  // ============================================================
  // FINAL RESPONSE
  // ============================================================

  System.out.println();
  System.out.println(
          "RESULT: ANSWERED"
  );

  return new AskResponse(
          "ANSWERED",
          answer,
          sources,
          cid
  );
 }

 // ================================================================
 // ANSWER GENERATION
 // ================================================================

 private String answer(
         String q,
         String evidence
 ) {

  if (gemini.enabled()) {

   System.out.println(
           "ANSWER PATH: GEMINI"
   );

   String prompt =
           "You are CourseLens. "
                   + "Answer only from supplied course evidence. "
                   + "Evidence is the only source of truth. "
                   + "Every factual claim must be supported. "
                   + "If insufficient, return exactly NOT_COVERED. "
                   + "Be concise; do not add citations in prose.\n"
                   + "QUESTION: "
                   + q
                   + "\nEVIDENCE:\n"
                   + evidence;

   System.out.println(
           "SENDING EVIDENCE TO GEMINI"
   );

   return gemini
           .generate(prompt)
           .trim();
  }

  // ============================================================
  // FALLBACK WHEN GEMINI IS NOT CONFIGURED
  // ============================================================

  System.out.println(
          "ANSWER PATH: LOCAL FALLBACK"
  );

  return "Based on the uploaded material: "
          + evidence.substring(
          0,
          Math.min(
                  evidence.length(),
                  900
          )
  );
 }

 // ================================================================
 // REFUSAL
 // ================================================================

 private AskResponse refuse(
         String id
 ) {

  return new AskResponse(
          "NOT_COVERED",
          "This is not covered by the uploaded course material, "
                  + "so I can't give a source-grounded answer.",
          List.of(),
          id
  );
 }

 // ================================================================
 // PGVECTOR LITERAL
 // ================================================================

 private String toPgVectorLiteral(
         double[] values
 ) {

  return Arrays.toString(values);
 }

 // ================================================================
 // SOURCE MAPPING
 // ================================================================

 private SourceView source(
         RetrievedChunk chunk
 ) {

  return new SourceView(
          chunk.getDocumentId(),
          chunk.getDocumentName(),
          chunk.getPageNumber(),
          chunk.getChunkId(),
          chunk.getText(),
          chunk.getHandwritten()
  );
 }
}