package com.example.courslens;
import com.example.courslens.service.EmbeddingService; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class EmbeddingServiceTest { @Test void convertsEmbeddingToNativeVectorValues(){var e=new EmbeddingService("","gemini-embedding-2",768);assertArrayEquals(new float[]{1.5f,-2f},e.toFloatArray(new double[]{1.5,-2}));} @Test void dimensionValidationRejectsWrongEmbedding(){var e=new EmbeddingService("","gemini-embedding-2",768);assertThrows(IllegalStateException.class,()->e.validate(new double[32]));} }
