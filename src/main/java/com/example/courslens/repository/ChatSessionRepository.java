package com.example.courslens.repository;

import com.example.courslens.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {}
