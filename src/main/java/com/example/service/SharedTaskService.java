package com.example.service;

import com.example.entity.SharedTask;
import com.example.entity.User;
import com.example.entity.SharedTask.TaskStatus;
import com.example.entity.SharedTask.TaskCategory;
import com.example.entity.SharedTask.TaskDifficulty;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;

public class SharedTaskService extends GenericServiceImpl<SharedTask, Integer> {
    
    public SharedTaskService() {
        super(SharedTask.class);
    }
    
    public List<SharedTask> findByStatus(TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.status = :status", SharedTask.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findByCategory(TaskCategory category) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.category = :category", SharedTask.class)
                    .setParameter("category", category)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findByDifficulty(TaskDifficulty difficulty) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.difficulty = :difficulty", SharedTask.class)
                    .setParameter("difficulty", difficulty)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findBySharedBy(User sharedBy) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.sharedBy = :sharedBy", SharedTask.class)
                    .setParameter("sharedBy", sharedBy)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findBySharedWith(User sharedWith) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.sharedWith = :sharedWith", SharedTask.class)
                    .setParameter("sharedWith", sharedWith)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findTasksBetweenUsers(User user1, User user2) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE (st.sharedBy = :user1 AND st.sharedWith = :user2) OR (st.sharedBy = :user2 AND st.sharedWith = :user1)", SharedTask.class)
                    .setParameter("user1", user1)
                    .setParameter("user2", user2)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<SharedTask> findByTitleContaining(String title) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT st FROM SharedTask st WHERE st.title LIKE :title", SharedTask.class)
                    .setParameter("title", "%" + title + "%")
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public SharedTask updateTaskStatus(Integer taskId, TaskStatus newStatus) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            SharedTask task = em.find(SharedTask.class, taskId);
            if (task != null) {
                task.setStatus(newStatus);
                task = em.merge(task);
            }
            transaction.commit();
            return task;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public SharedTask updateTaskAttachment(Integer taskId, String attachmentPath) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            SharedTask task = em.find(SharedTask.class, taskId);
            if (task != null) {
                task.setAttachment(attachmentPath);
                task = em.merge(task);
            }
            transaction.commit();
            return task;
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Inbox: received challenges filtered by optional status */
    public List<SharedTask> findInbox(User recipient, TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = status != null
                                        ? "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedWith = :u AND st.status = :s ORDER BY st.createdAt DESC"
                                        : "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedWith = :u ORDER BY st.createdAt DESC";
            var q = em.createQuery(jpql, SharedTask.class).setParameter("u", recipient);
            if (status != null) q.setParameter("s", status);
            return q.getResultList();
        } finally { em.close(); }
    }

    /** Inbox by recipient id: robust when user entity comes from another persistence context */
    public List<SharedTask> findInboxByUserId(int recipientId, TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = status != null
                                        ? "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedWith.id = :uid AND st.status = :s ORDER BY st.createdAt DESC"
                                        : "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedWith.id = :uid ORDER BY st.createdAt DESC";
            var q = em.createQuery(jpql, SharedTask.class).setParameter("uid", recipientId);
            if (status != null) q.setParameter("s", status);
            return q.getResultList();
        } finally { em.close(); }
    }

    /** Outbox: sent challenges filtered by optional status */
    public List<SharedTask> findOutbox(User sender, TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = status != null
                                        ? "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedBy = :u AND st.status = :s ORDER BY st.createdAt DESC"
                                        : "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedBy = :u ORDER BY st.createdAt DESC";
            var q = em.createQuery(jpql, SharedTask.class).setParameter("u", sender);
            if (status != null) q.setParameter("s", status);
            return q.getResultList();
        } finally { em.close(); }
    }

    /** Outbox by sender id: robust when user entity comes from another persistence context */
    public List<SharedTask> findOutboxByUserId(int senderId, TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = status != null
                                        ? "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedBy.id = :uid AND st.status = :s ORDER BY st.createdAt DESC"
                                        : "SELECT st FROM SharedTask st " +
                                            "LEFT JOIN FETCH st.sharedBy " +
                                            "LEFT JOIN FETCH st.sharedWith " +
                                            "WHERE st.sharedBy.id = :uid ORDER BY st.createdAt DESC";
            var q = em.createQuery(jpql, SharedTask.class).setParameter("uid", senderId);
            if (status != null) q.setParameter("s", status);
            return q.getResultList();
        } finally { em.close(); }
    }

    /**
     * Outbox by sender identity. Useful when legacy data has id drift but username/email stayed stable.
     */
    public List<SharedTask> findOutboxByIdentity(Integer senderId, String username, String email, TaskStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            String statusClause = status != null ? " AND st.status = :s" : "";
            String jpql =
                "SELECT st FROM SharedTask st " +
                    "LEFT JOIN FETCH st.sharedBy " +
                    "LEFT JOIN FETCH st.sharedWith " +
                    "WHERE (" +
                            "(:sid IS NOT NULL AND st.sharedBy.id = :sid)" +
                            " OR (:uname IS NOT NULL AND LOWER(st.sharedBy.username) = :uname)" +
                            " OR (:uemail IS NOT NULL AND LOWER(st.sharedBy.email) = :uemail)" +
                            ")" + statusClause +
                            " ORDER BY st.createdAt DESC";

            var q = em.createQuery(jpql, SharedTask.class)
                    .setParameter("sid", senderId)
                    .setParameter("uname", normalize(username))
                    .setParameter("uemail", normalize(email));

            if (status != null) {
                q.setParameter("s", status);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase();
        return v.isEmpty() ? null : v;
    }

    /** Respond to a challenge (only recipient can call this) */
    public SharedTask respond(Integer taskId, TaskStatus response) {
        EntityManager em = emf.createEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            SharedTask task = em.find(SharedTask.class, taskId);
            if (task != null) {
                task.setStatus(response);
                task.setRespondedAt(java.time.LocalDateTime.now());
                task = em.merge(task);
            }
            tx.commit();
            return task;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally { em.close(); }
    }

    /** Prevent duplicate pending challenges with same title between same users */
    public boolean existsPendingDuplicate(String title, User sender, User recipient) {
        if (title == null || sender == null || recipient == null) return false;
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(st) FROM SharedTask st WHERE LOWER(st.title) = :t AND st.sharedBy = :s AND st.sharedWith = :r AND st.status = :pending",
                    Long.class)
                    .setParameter("t", title.trim().toLowerCase())
                    .setParameter("s", sender)
                    .setParameter("r", recipient)
                    .setParameter("pending", TaskStatus.PENDING)
                    .getSingleResult();
            return count != null && count > 0;
        } finally {
            em.close();
        }
    }
}
