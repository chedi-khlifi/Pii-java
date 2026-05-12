package com.example.service;

import com.example.entity.VirtualRoom;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class VirtualRoomService extends GenericServiceImpl<VirtualRoom, Integer> {
    
    public VirtualRoomService() {
        super(VirtualRoom.class);
    }
    
    public Optional<VirtualRoom> findByName(String name) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT vr FROM VirtualRoom vr WHERE vr.name = :name", VirtualRoom.class)
                    .setParameter("name", name)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }
    
    public List<VirtualRoom> findByRoomType(String roomType) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT vr FROM VirtualRoom vr WHERE vr.roomType = :roomType", VirtualRoom.class)
                    .setParameter("roomType", roomType)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<VirtualRoom> findActiveRooms() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT vr FROM VirtualRoom vr WHERE vr.isActive = true", VirtualRoom.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<VirtualRoom> findInactiveRooms() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT vr FROM VirtualRoom vr WHERE vr.isActive = false", VirtualRoom.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public VirtualRoom toggleRoomStatus(Integer roomId) {
        EntityManager em = emf.createEntityManager();
        javax.persistence.EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            VirtualRoom room = em.find(VirtualRoom.class, roomId);
            if (room != null) {
                room.setActive(!room.isActive());
                room = em.merge(room);
            }
            transaction.commit();
            return room;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** Check if user is a participant in the room */
    public boolean isParticipant(VirtualRoom room, com.example.entity.User user) {
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM VirtualRoomParticipant p WHERE p.virtualRoom = :room AND p.user = :user AND p.isActive = true", 
                Long.class)
                .setParameter("room", room)
                .setParameter("user", user)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    /** Add user as participant to room */
    public void addParticipant(VirtualRoom room, com.example.entity.User user) {
        EntityManager em = emf.createEntityManager();
        javax.persistence.EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            
            // Check if already participant
            Long existing = em.createQuery(
                "SELECT COUNT(p) FROM VirtualRoomParticipant p WHERE p.virtualRoom = :room AND p.user = :user", 
                Long.class)
                .setParameter("room", room)
                .setParameter("user", user)
                .getSingleResult();
                
            if (existing == 0) {
                com.example.entity.VirtualRoomParticipant participant = 
                    new com.example.entity.VirtualRoomParticipant(room, user);
                em.persist(participant);
            } else {
                // Reactivate if exists but inactive
                em.createQuery(
                    "UPDATE VirtualRoomParticipant p SET p.isActive = true WHERE p.virtualRoom = :room AND p.user = :user")
                    .setParameter("room", room)
                    .setParameter("user", user)
                    .executeUpdate();
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Remove user from room */
    public void removeParticipant(VirtualRoom room, com.example.entity.User user) {
        EntityManager em = emf.createEntityManager();
        javax.persistence.EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            em.createQuery(
                "UPDATE VirtualRoomParticipant p SET p.isActive = false WHERE p.virtualRoom = :room AND p.user = :user")
                .setParameter("room", room)
                .setParameter("user", user)
                .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Get all participants of a room */
    public java.util.List<com.example.entity.User> getRoomParticipants(VirtualRoom room) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT p.user FROM VirtualRoomParticipant p WHERE p.virtualRoom = :room AND p.isActive = true", 
                com.example.entity.User.class)
                .setParameter("room", room)
                .getResultList();
        } finally {
            em.close();
        }
    }
}
