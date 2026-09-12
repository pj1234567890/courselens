package com.example.courslens.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Service
public class EmbeddingService {

 private final String key;
 private final String model;
 private final int dimension;

 private final ObjectMapper json =
         new ObjectMapper();

 private final RestClient restClient =
         RestClient.create();

 public EmbeddingService(
         @Value("${gemini.api-key:}") String k,
         @Value("${gemini.embedding-model:gemini-embedding-2}") String m,
         @Value("${gemini.embedding-dimension:768}") int d) {

  key = k;
  model = m;
  dimension = d;
 }

 public double[] embed(String text) {

  if (key == null || key.isBlank()) {
   throw new IllegalStateException(
           "GEMINI_API_KEY is required for embeddings"
   );
  }

  if (text == null || text.isBlank()) {
   throw new IllegalArgumentException(
           "Cannot generate embedding for empty text"
   );
  }

  try {

   Map<String, Object> body = Map.of(
           "model", "models/" + model,
           "content", Map.of(
                   "parts",
                   List.of(
                           Map.of("text", text)
                   )
           ),
           "outputDimensionality", dimension
   );

   String url =
           "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model
                   + ":embedContent?key="
                   + key;

   System.out.println(
           "========== GEMINI EMBEDDING DEBUG =========="
   );

   System.out.println(
           "MODEL: " + model
   );

   System.out.println(
           "DIMENSION: " + dimension
   );

   System.out.println(
           "TEXT LENGTH: " + text.length()
   );

   String out =
           restClient
                   .post()
                   .uri(url)
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(body)
                   .retrieve()
                   .body(String.class);

   System.out.println(
           "GEMINI EMBEDDING REQUEST SUCCESS"
   );

   if (out == null || out.isBlank()) {
    throw new IllegalStateException(
            "Gemini returned an empty embedding response"
    );
   }

   double[] vector =
           json.readTree(out)
                   .at("/embedding/values")
                   .traverse(json)
                   .readValueAs(double[].class);

   validate(vector);

   return vector;

  } catch (RestClientResponseException e) {

   System.out.println(
           "========== GEMINI EMBEDDING HTTP ERROR =========="
   );

   System.out.println(
           "STATUS: " + e.getStatusCode()
   );

   System.out.println(
           "RESPONSE BODY:"
   );

   System.out.println(
           e.getResponseBodyAsString()
   );

   System.out.println(
           "========== END GEMINI EMBEDDING HTTP ERROR =========="
   );

   throw new IllegalStateException(
           "Gemini embedding HTTP "
                   + e.getStatusCode()
                   + ": "
                   + e.getResponseBodyAsString(),
           e
   );

  } catch (IllegalStateException e) {

   throw e;

  } catch (Exception e) {

   e.printStackTrace();

   throw new IllegalStateException(
           "Gemini embedding request failed: "
                   + e.getMessage(),
           e
   );
  }
 }

 public void validate(double[] vector) {

  if (vector == null ||
          vector.length != dimension) {

   throw new IllegalStateException(
           "Expected Gemini embedding dimension "
                   + dimension
                   + ", received "
                   + (vector == null ? 0 : vector.length)
   );
  }
 }

 public float[] toFloatArray(
         double[] values) {

  float[] result =
          new float[values.length];

  for (int i = 0;
       i < values.length;
       i++) {

   result[i] =
           (float) values[i];
  }

  return result;
 }
}