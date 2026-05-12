package com.example.service;

import com.example.entity.Claim;
import com.example.entity.User;
import com.example.entity.Claim.ClaimStatus;
import com.example.entity.Claim.ClaimPriority;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Optional;

public class ClaimService extends GenericServiceImpl<Claim, Integer> {
    
    public ClaimService() {
        super(Claim.class);
    }
    
    public List<Claim> findByStatus(ClaimStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Claim c WHERE c.status = :status", Claim.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Claim> findByPriority(ClaimPriority priority) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Claim c WHERE c.priority = :priority", Claim.class)
                    .setParameter("priority", priority)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Claim> findByCreatedBy(User createdBy) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Claim c WHERE c.createdBy = :createdBy", Claim.class)
                    .setParameter("createdBy", createdBy)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Claim> findByAssignedTo(User assignedTo) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Claim c WHERE c.assignedTo = :assignedTo", Claim.class)
                    .setParameter("assignedTo", assignedTo)
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Claim> findByTitleContaining(String title) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Claim c WHERE c.title LIKE :title", Claim.class)
                    .setParameter("title", "%" + title + "%")
                    .getResultList();
        } finally {
            em.close();
        }
    }
    
    public Claim updateClaimStatus(Integer claimId, ClaimStatus newStatus, String adminNotes) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Claim claim = em.find(Claim.class, claimId);
            if (claim != null) {
                claim.setStatus(newStatus);
                if (adminNotes != null && !adminNotes.trim().isEmpty()) {
                    claim.setAdminNotes(adminNotes);
                }
                claim = em.merge(claim);
            }
            transaction.commit();
            return claim;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Claim assignClaim(Integer claimId, User assignedTo) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Claim claim = em.find(Claim.class, claimId);
            if (claim != null) {
                claim.setAssignedTo(assignedTo);
                claim = em.merge(claim);
            }
            transaction.commit();
            return claim;
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Dynamic search + combined filter for admin view */
    public List<Claim> search(String keyword, ClaimStatus status, ClaimPriority priority, User assignedTo) {
        EntityManager em = emf.createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT c FROM Claim c " +
                    "LEFT JOIN FETCH c.createdBy " +
                    "LEFT JOIN FETCH c.assignedTo " +
                    "WHERE 1=1");
            if (keyword != null && !keyword.isBlank())
                jpql.append(" AND (LOWER(c.title) LIKE :kw OR LOWER(c.description) LIKE :kw OR LOWER(c.createdBy.username) LIKE :kw)");
            if (status != null) jpql.append(" AND c.status = :status");
            if (priority != null) jpql.append(" AND c.priority = :priority");
            if (assignedTo != null) jpql.append(" AND c.assignedTo = :assignedTo");
            jpql.append(" ORDER BY c.createdAt DESC");

            var query = em.createQuery(jpql.toString(), Claim.class);
            if (keyword != null && !keyword.isBlank()) query.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            if (status != null) query.setParameter("status", status);
            if (priority != null) query.setParameter("priority", priority);
            if (assignedTo != null) query.setParameter("assignedTo", assignedTo);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public long countByStatus(ClaimStatus status) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(c) FROM Claim c WHERE c.status = :s", Long.class)
                    .setParameter("s", status).getSingleResult();
        } finally { em.close(); }
    }

    public long countByPriority(ClaimPriority priority) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(c) FROM Claim c WHERE c.priority = :p", Long.class)
                    .setParameter("p", priority).getSingleResult();
        } finally { em.close(); }
    }

    public long countAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(c) FROM Claim c", Long.class).getSingleResult();
        } finally { em.close(); }
    }

    /** My tickets sorted newest first */
    public List<Claim> findByCreatedBySorted(User user) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Claim c " +
                            "LEFT JOIN FETCH c.createdBy " +
                            "LEFT JOIN FETCH c.assignedTo " +
                            "WHERE c.createdBy = :u ORDER BY c.createdAt DESC",
                            Claim.class)
                    .setParameter("u", user).getResultList();
        } finally { em.close(); }
    }

    /** My tickets by creator id sorted newest first (most reliable filter). */
    public List<Claim> findByCreatedByIdSorted(int creatorId) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Claim c " +
                            "LEFT JOIN FETCH c.createdBy " +
                            "LEFT JOIN FETCH c.assignedTo " +
                            "WHERE c.createdBy.id = :cid ORDER BY c.createdAt DESC",
                            Claim.class)
                    .setParameter("cid", creatorId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Robust "my tickets" lookup when session user id may differ from legacy/migrated rows.
     */
    public List<Claim> findMyTicketsByIdentity(Integer creatorId, String username, String email) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql =
                    "SELECT c FROM Claim c " +
                    "LEFT JOIN FETCH c.createdBy " +
                    "LEFT JOIN FETCH c.assignedTo " +
                    "WHERE (" +
                    "(:cid IS NOT NULL AND c.createdBy.id = :cid) " +
                    "OR (:uname IS NOT NULL AND LOWER(c.createdBy.username) = :uname) " +
                    "OR (:uemail IS NOT NULL AND LOWER(c.createdBy.email) = :uemail)" +
                    ") ORDER BY c.createdAt DESC";

            return em.createQuery(jpql, Claim.class)
                    .setParameter("cid", creatorId)
                    .setParameter("uname", normalize(username))
                    .setParameter("uemail", normalize(email))
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase();
        return v.isEmpty() ? null : v;
    }

    /** Prevent duplicate active tickets with same title for same creator */
    public boolean existsActiveDuplicateTitle(String title, User creator) {
        if (title == null || creator == null) return false;
        EntityManager em = emf.createEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(c) FROM Claim c WHERE LOWER(c.title) = :t AND c.createdBy = :u AND c.status IN (:open, :progress)",
                    Long.class)
                    .setParameter("t", title.trim().toLowerCase())
                    .setParameter("u", creator)
                    .setParameter("open", ClaimStatus.OPEN)
                    .setParameter("progress", ClaimStatus.IN_PROGRESS)
                    .getSingleResult();
            return count != null && count > 0;
        } finally {
            em.close();
        }
    }
}
