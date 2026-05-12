package com.example.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * One-time startup migration for shared_task table naming.
 */
public final class SchemaMigrator {

    private SchemaMigrator() {}

    public static void migrateAll() {
        migratePair("`user`", "users",
                "INSERT IGNORE INTO `user` (id, username, email, password, first_name, last_name, role, created_at) " +
                        "SELECT id, username, email, password, first_name, last_name, role, created_at FROM users");

        // Ensure legacy user schemas have the columns expected by JPA mappings.
        ensureUserColumns();

        migratePair("shared_task", "shared_tasks",
                "INSERT IGNORE INTO shared_task " +
                        "(id, title, description, status, category, difficulty, attachment, created_at, responded_at, updated_at, shared_by_id, shared_with_id) " +
                        "SELECT id, title, description, status, category, difficulty, attachment, created_at, responded_at, updated_at, shared_by_id, shared_with_id FROM shared_tasks");

        migratePair("claim", "claims",
                "INSERT IGNORE INTO claim " +
                        "(id, title, description, status, priority, admin_notes, created_at, updated_at, resolved_at, created_by_id, assigned_to_id) " +
                        "SELECT id, title, description, status, priority, admin_notes, created_at, updated_at, resolved_at, created_by_id, assigned_to_id FROM claims");

        ensureClaimEnumValues();

        migratePair("chat_message", "chat_messages",
                "INSERT IGNORE INTO chat_message " +
                        "(id, content, is_edited, created_at, edited_at, sender_id, virtual_room_id) " +
                        "SELECT id, content, is_edited, created_at, edited_at, sender_id, virtual_room_id FROM chat_messages");

        migratePair("virtual_room", "virtual_rooms",
                "INSERT IGNORE INTO virtual_room " +
                        "(id, name, description, room_type, is_active, created_at) " +
                        "SELECT id, name, description, room_type, is_active, created_at FROM virtual_rooms");
    }

    private static void ensureClaimEnumValues() {
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("defaultPersistenceUnit");
            em = emf.createEntityManager();

            if (!tableExists(em, "claim")) {
                return;
            }

            em.getTransaction().begin();
            em.createNativeQuery(
                    "UPDATE claim SET priority = UPPER(REPLACE(REPLACE(priority, '-', '_'), ' ', '_')) " +
                            "WHERE priority IS NOT NULL")
                    .executeUpdate();
            em.createNativeQuery(
                    "UPDATE claim SET status = UPPER(REPLACE(REPLACE(status, '-', '_'), ' ', '_')) " +
                            "WHERE status IS NOT NULL")
                    .executeUpdate();
            em.getTransaction().commit();
            System.out.println("Schema migration: normalized claim status/priority values");
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Schema migration skipped for claim enum normalization: " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null && emf.isOpen()) emf.close();
        }
    }

    private static void migratePair(String originalTable, String duplicateTable, String copySql) {
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("defaultPersistenceUnit");
            em = emf.createEntityManager();

            String originalRaw = stripTicks(originalTable);
            boolean hasOriginal = tableExists(em, originalRaw);
            boolean hasDuplicate = tableExists(em, duplicateTable);

            if (!hasOriginal && hasDuplicate) {
                em.getTransaction().begin();
                em.createNativeQuery("RENAME TABLE " + duplicateTable + " TO " + originalTable).executeUpdate();
                em.getTransaction().commit();
                System.out.println("Schema migration: renamed " + duplicateTable + " to " + originalRaw);
                return;
            }

            if (hasOriginal && hasDuplicate) {
                em.getTransaction().begin();
                try {
                    em.createNativeQuery(copySql).executeUpdate();
                } catch (Exception ignored) {
                    // If schemas differ, still prefer original table and remove duplicate.
                }
                em.createNativeQuery("DROP TABLE " + duplicateTable).executeUpdate();
                em.getTransaction().commit();
                System.out.println("Schema migration: merged and removed " + duplicateTable);
            }
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Schema migration skipped for " + duplicateTable + ": " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null && emf.isOpen()) emf.close();
        }
    }

    private static void ensureUserColumns() {
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("defaultPersistenceUnit");
            em = emf.createEntityManager();

            if (!tableExists(em, "user")) {
                return;
            }

            em.getTransaction().begin();

            if (!columnExists(em, "user", "username")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN username VARCHAR(50) NULL").executeUpdate();
            }
            if (!columnExists(em, "user", "email")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN email VARCHAR(100) NULL").executeUpdate();
            }
            if (!columnExists(em, "user", "password")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN password VARCHAR(255) NULL").executeUpdate();
            }

            if (!columnExists(em, "user", "first_name")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN first_name VARCHAR(50) NULL").executeUpdate();
            }
            if (!columnExists(em, "user", "last_name")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN last_name VARCHAR(50) NULL").executeUpdate();
            }
            if (!columnExists(em, "user", "created_at")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP")
                        .executeUpdate();
            }
            if (!columnExists(em, "user", "role")) {
                em.createNativeQuery("ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER'")
                        .executeUpdate();
            }

            // Backfill from common legacy names when present.
            if (columnExists(em, "user", "firstname")) {
                em.createNativeQuery("UPDATE `user` SET first_name = COALESCE(first_name, firstname)").executeUpdate();
            }
            if (columnExists(em, "user", "lastname")) {
                em.createNativeQuery("UPDATE `user` SET last_name = COALESCE(last_name, lastname)").executeUpdate();
            }
            if (columnExists(em, "user", "user_name")) {
                em.createNativeQuery("UPDATE `user` SET username = COALESCE(username, user_name)").executeUpdate();
            }
            if (columnExists(em, "user", "login")) {
                em.createNativeQuery("UPDATE `user` SET username = COALESCE(username, login)").executeUpdate();
            }
            if (columnExists(em, "user", "mail")) {
                em.createNativeQuery("UPDATE `user` SET email = COALESCE(email, mail)").executeUpdate();
            }
            if (columnExists(em, "user", "pass")) {
                em.createNativeQuery("UPDATE `user` SET password = COALESCE(password, pass)").executeUpdate();
            }
            if (columnExists(em, "user", "passwd")) {
                em.createNativeQuery("UPDATE `user` SET password = COALESCE(password, passwd)").executeUpdate();
            }
                if (columnExists(em, "user", "user_id") && !columnExists(em, "user", "id")) {
                em.createNativeQuery("UPDATE `user` SET username = COALESCE(username, CONCAT('user_', user_id))")
                    .executeUpdate();
                }
            if (columnExists(em, "user", "roles")) {
                em.createNativeQuery(
                        "UPDATE `user` SET role = CASE " +
                                "WHEN role IS NULL OR role = '' THEN " +
                                "CASE WHEN roles LIKE '%ROLE_ADMIN%' THEN 'ROLE_ADMIN' ELSE 'ROLE_USER' END " +
                                "ELSE role END")
                        .executeUpdate();
            }

                if (columnExists(em, "user", "id")) {
                em.createNativeQuery("UPDATE `user` SET username = COALESCE(NULLIF(username, ''), CONCAT('user_', id))")
                    .executeUpdate();
                } else {
                em.createNativeQuery("UPDATE `user` SET username = COALESCE(NULLIF(username, ''), 'user_legacy')")
                    .executeUpdate();
                }
            em.createNativeQuery("UPDATE `user` SET email = COALESCE(NULLIF(email, ''), CONCAT(username, '@mindforge.local'))")
                    .executeUpdate();
            em.createNativeQuery("UPDATE `user` SET password = COALESCE(NULLIF(password, ''), 'changeme123')")
                    .executeUpdate();
            em.createNativeQuery("UPDATE `user` SET role = COALESCE(NULLIF(role, ''), 'ROLE_USER')").executeUpdate();

            try {
                em.createNativeQuery("ALTER TABLE `user` MODIFY COLUMN username VARCHAR(50) NOT NULL").executeUpdate();
                em.createNativeQuery("ALTER TABLE `user` MODIFY COLUMN email VARCHAR(100) NOT NULL").executeUpdate();
                em.createNativeQuery("ALTER TABLE `user` MODIFY COLUMN password VARCHAR(255) NOT NULL").executeUpdate();
                em.createNativeQuery("ALTER TABLE `user` ADD UNIQUE INDEX idx_user_username_unique (username)").executeUpdate();
            } catch (Exception ignored) {
                // Existing constraints/indexes or duplicate legacy data can make these operations fail safely.
            }

            em.getTransaction().commit();
            System.out.println("Schema migration: ensured user table columns (username, email, password, first_name, last_name, role, created_at)");
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Schema migration skipped for user columns: " + e.getMessage());
        } finally {
            if (em != null) em.close();
            if (emf != null && emf.isOpen()) emf.close();
        }
    }

    private static String stripTicks(String value) {
        return value.replace("`", "");
    }

    private static boolean tableExists(EntityManager em, String tableName) {
        Object result = em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")
                .setParameter(1, tableName)
                .getSingleResult();
        Number count = (Number) result;
        return count.longValue() > 0;
    }

    private static boolean columnExists(EntityManager em, String tableName, String columnName) {
        Object result = em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.columns " +
                                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")
                .setParameter(1, tableName)
                .setParameter(2, columnName)
                .getSingleResult();
        Number count = (Number) result;
        return count.longValue() > 0;
    }
}
