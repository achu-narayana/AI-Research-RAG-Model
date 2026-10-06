package com.researchassistant.chat.repository;

import com.researchassistant.chat.entity.ChatMessage;
import com.researchassistant.chat.entity.ProjectChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatOrderByIdAsc(
            ProjectChat chat);

    void deleteAllByChat(ProjectChat chat);
}
