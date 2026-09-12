package com.example.courslens.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="chat_messages")
public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="session_id",nullable=false) public ChatSession session;
 @Enumerated(EnumType.STRING) @Column(name="role",nullable=false,length=16) public Role role;
 @Column(name="content",nullable=false,columnDefinition="TEXT") public String content;
 @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
 public enum Role { USER, ASSISTANT }
}
