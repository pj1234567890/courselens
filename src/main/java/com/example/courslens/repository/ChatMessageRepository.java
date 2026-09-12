package com.example.courslens.repository;

import com.example.courslens.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
 List<ChatMessage> findBySessionIdOrderByCreatedAt(UUID sessionId);
}
