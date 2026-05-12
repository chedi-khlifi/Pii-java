package com.example.service;

import com.example.entity.ChatMessage;
import com.example.entity.User;
import com.example.entity.VirtualRoom;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;

public class ChatMessageService extends GenericServiceImpl<ChatMessage, Integer> {
    
    public ChatMessageService() {
        super(ChatMessage.class);
    }
    
    public List<ChatMessage> findBySender(User sender) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT cm FROM ChatMessage cm WHERE cm.sender = :sender", ChatMessage.class)
                    .setParameter("sender", sender)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<ChatMessage> findByVirtualRoom(VirtualRoom virtualRoom) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT cm FROM ChatMessage cm WHERE cm.virtualRoom = :virtualRoom ORDER BY cm.createdAt", ChatMessage.class)
                    .setParameter("virtualRoom", virtualRoom)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<ChatMessage> findByContentContaining(String content) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT cm FROM ChatMessage cm WHERE cm.content LIKE :content", ChatMessage.class)
                    .setParameter("content", "%" + content + "%")
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<ChatMessage> findEditedMessages() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT cm FROM ChatMessage cm WHERE cm.isEdited = true", ChatMessage.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public ChatMessage editMessage(Integer messageId, String newContent) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            ChatMessage message = em.find(ChatMessage.class, messageId);
            if (message != null) {
                message.setContent(newContent);
                message.setEdited(true);
                message.setEditedAt(java.time.LocalDateTime.now());
                message = em.merge(message);
            }
            transaction.commit();
            return message;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
