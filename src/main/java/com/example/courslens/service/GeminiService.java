package com.example.courslens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

 private final String key;
 private final String model;

 private final ObjectMapper mapper = new ObjectMapper();
 private final RestClient restClient = RestClient.create();

 GeminiService(
         @Value("${gemini.api-key:}") String key,
         @Value("${gemini.model:gemini-2.0-flash}") String model
 ) {
  this.key = key;
  this.model = model;
 }

 public boolean enabled() {
  return key != null && !key.isBlank();
 }

 // ============================================================
 // TEXT GENERATION
 // ============================================================

 public String generate(String prompt) {

  if (!enabled()) {
   System.out.println("GEMINI GENERATE: API KEY NOT CONFIGURED");
   return "";
  }

  try {

   Map<String, Object> body = Map.of(
           "contents",
           List.of(
                   Map.of(
                           "parts",
                           List.of(
                                   Map.of(
                                           "text",
                                           prompt
                                   )
                           )
                   )
           ),
           "generationConfig",
           Map.of(
                   "temperature",
                   0.1
           )
   );

   String url =
           "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model
                   + ":generateContent?key="
                   + key;

   System.out.println();
   System.out.println("========== GEMINI DEBUG ==========");
   System.out.println("MODEL: " + model);
   System.out.println("CALLING GEMINI...");
   System.out.println("PROMPT LENGTH: " + prompt.length());

   String response =
           restClient
                   .post()
                   .uri(url)
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(body)
                   .retrieve()
                   .body(String.class);

   System.out.println("GEMINI HTTP REQUEST SUCCESS");
   System.out.println(
           "RAW RESPONSE LENGTH: "
                   + (response == null
                   ? 0
                   : response.length())
   );

   System.out.println(
           "RAW GEMINI RESPONSE:"
   );

   System.out.println(response);

   System.out.println(
           "========== END GEMINI DEBUG =========="
   );

   if (response == null || response.isBlank()) {
    System.out.println(
            "GEMINI ERROR: EMPTY HTTP RESPONSE"
    );
    return "";
   }

   JsonNode root =
           mapper.readTree(response);

   JsonNode textNode =
           root.at(
                   "/candidates/0/content/parts/0/text"
           );

   if (textNode.isMissingNode()
           || textNode.isNull()) {

    System.out.println(
            "GEMINI ERROR: RESPONSE DOES NOT CONTAIN "
                    + "candidates[0].content.parts[0].text"
    );

    System.out.println(
            "PARSED GEMINI JSON:"
    );

    System.out.println(
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root)
    );

    return "";
   }

   String text =
           textNode.asText();

   System.out.println(
           "GEMINI GENERATED TEXT:"
   );

   System.out.println(text);

   return text;

  } catch (RestClientResponseException e) {

   System.out.println();
   System.out.println(
           "========== GEMINI HTTP ERROR =========="
   );

   System.out.println(
           "STATUS: "
                   + e.getStatusCode()
   );

   System.out.println(
           "RESPONSE BODY:"
   );

   System.out.println(
           e.getResponseBodyAsString()
   );

   System.out.println(
           "========== END GEMINI HTTP ERROR =========="
   );

   return "";

  } catch (Exception e) {

   System.out.println();
   System.out.println(
           "========== GEMINI ERROR =========="
   );

   System.out.println(
           "ERROR TYPE: "
                   + e.getClass().getName()
   );

   System.out.println(
           "ERROR MESSAGE: "
                   + e.getMessage()
   );

   e.printStackTrace();

   System.out.println(
           "========== END GEMINI ERROR =========="
   );

   return "";
  }
 }

 // ============================================================
 // IMAGE / HANDWRITTEN NOTE TRANSCRIPTION
 // ============================================================

 public String readImage(
         byte[] bytes,
         String mime
 ) {

  if (!enabled()) {

   System.out.println(
           "GEMINI IMAGE: API KEY NOT CONFIGURED"
   );

   return "";
  }

  try {

   String actualMime =
           mime == null
                   ? "image/jpeg"
                   : mime;

   Map<String, Object> body =
           Map.of(
                   "contents",
                   List.of(
                           Map.of(
                                   "parts",
                                   List.of(
                                           Map.of(
                                                   "text",
                                                   "Transcribe this course page exactly. "
                                                           + "Include handwritten notes, equations, "
                                                           + "labels and tables. "
                                                           + "Return only readable content."
                                           ),
                                           Map.of(
                                                   "inline_data",
                                                   Map.of(
                                                           "mime_type",
                                                           actualMime,
                                                           "data",
                                                           Base64.getEncoder()
                                                                   .encodeToString(bytes)
                                                   )
                                           )
                                   )
                           )
                   )
           );

   String url =
           "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model
                   + ":generateContent?key="
                   + key;

   System.out.println();
   System.out.println(
           "========== GEMINI IMAGE DEBUG =========="
   );

   System.out.println(
           "MODEL: " + model
   );

   System.out.println(
           "MIME TYPE: " + actualMime
   );

   System.out.println(
           "IMAGE BYTES: " + bytes.length
   );

   String response =
           restClient
                   .post()
                   .uri(url)
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(body)
                   .retrieve()
                   .body(String.class);

   System.out.println(
           "GEMINI IMAGE REQUEST SUCCESS"
   );

   System.out.println(
           "RAW RESPONSE LENGTH: "
                   + (response == null
                   ? 0
                   : response.length())
   );

   System.out.println(
           "RAW GEMINI IMAGE RESPONSE:"
   );

   System.out.println(response);

   System.out.println(
           "========== END GEMINI IMAGE DEBUG =========="
   );

   if (response == null || response.isBlank()) {
    return "";
   }

   JsonNode root =
           mapper.readTree(response);

   JsonNode textNode =
           root.at(
                   "/candidates/0/content/parts/0/text"
           );

   if (textNode.isMissingNode()
           || textNode.isNull()) {

    System.out.println(
            "GEMINI IMAGE ERROR: "
                    + "NO TEXT FIELD IN RESPONSE"
    );

    return "";
   }

   return textNode.asText();

  } catch (RestClientResponseException e) {

   System.out.println();
   System.out.println(
           "========== GEMINI IMAGE HTTP ERROR =========="
   );

   System.out.println(
           "STATUS: "
                   + e.getStatusCode()
   );

   System.out.println(
           "RESPONSE BODY:"
   );

   System.out.println(
           e.getResponseBodyAsString()
   );

   System.out.println(
           "========== END GEMINI IMAGE HTTP ERROR =========="
   );

   return "";

  } catch (Exception e) {

   System.out.println();
   System.out.println(
           "========== GEMINI IMAGE ERROR =========="
   );

   System.out.println(
           "ERROR TYPE: "
                   + e.getClass().getName()
   );

   System.out.println(
           "ERROR MESSAGE: "
                   + e.getMessage()
   );

   e.printStackTrace();

   System.out.println(
           "========== END GEMINI IMAGE ERROR =========="
   );

   return "";
  }
 }
}