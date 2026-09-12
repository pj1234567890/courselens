package com.example.courslens.domain;
import jakarta.persistence.*; import java.util.*; import org.hibernate.annotations.Type;
@Entity @Table(name="chunks") public class Chunk {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="document_page_id",nullable=false) public Page page; @Column(name="chunk_index",nullable=false) public int chunkIndex;
 @Column(name="chunk_text",nullable=false,columnDefinition="TEXT") public String text; @Type(PgVectorType.class) @Column(name="embedding",columnDefinition="vector(768)") public float[] embedding;
}
