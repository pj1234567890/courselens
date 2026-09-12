package com.example.courslens.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name="chat_sessions")
public class ChatSession {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) public Instant updatedAt=Instant.now();
 @OneToMany(mappedBy="session",cascade=CascadeType.ALL,orphanRemoval=true) public List<ChatMessage> messages=new ArrayList<>();
}
