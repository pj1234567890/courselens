package com.example.courslens.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="documents") public class Document {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="original_filename",nullable=false,length=512) public String filename;
 @Column(name="document_type",nullable=false,length=32) public String type;
 @Column(name="uploaded_at",nullable=false) public Instant createdAt=Instant.now();
 @Column(name="processing_status",nullable=false,length=32) public String status="UPLOADED";
 @Column(name="storage_path",nullable=false,columnDefinition="TEXT") public String storagePath;
 @Column(name="page_count",nullable=false) public int pageCount;
 @Column(name="processing_error",columnDefinition="TEXT") public String processingError;
 @OneToMany(mappedBy="document",cascade=CascadeType.ALL,orphanRemoval=true) public List<Page> pages=new ArrayList<>();
}
