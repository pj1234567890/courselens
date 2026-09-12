package com.example.courslens.domain;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="document_pages") public class Page {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="document_id",nullable=false) public Document document;
 @Column(name="page_number",nullable=false) public int pageNumber; @Column(name="extracted_text",columnDefinition="TEXT") public String extractedText;
 @Column(name="source_type",nullable=false,length=64) public String sourceType;
 @Column(name="source_path",columnDefinition="TEXT") public String imagePath; @Column(name="handwritten",nullable=false) public boolean handwritten;
 @OneToMany(mappedBy="page",cascade=CascadeType.ALL,orphanRemoval=true) public List<Chunk> chunks=new ArrayList<>();
}
