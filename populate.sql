-- ═══════════════════════════════════════════════════════════════
-- MindForge DB Seed — mindforge_db
-- ═══════════════════════════════════════════════════════════════

SET FOREIGN_KEY_CHECKS = 0;

-- ── Subjects ─────────────────────────────────────────────────────
INSERT IGNORE INTO subject (id, name) VALUES
(1,  'Mathematics'),
(2,  'Physics'),
(3,  'Computer Science'),
(4,  'Algorithms'),
(5,  'Web Development'),
(6,  'Databases'),
(7,  'English'),
(8,  'History');

-- ── Users (password = "password123" bcrypt hashed) ───────────────
-- All users: email / password123
INSERT IGNORE INTO user (id, email, password, roles, is_verified, created_at, username, first_name, last_name, role) VALUES
(1,  'admin@mindforge.local',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '["ROLE_ADMIN","ROLE_USER"]', 1, NOW(), 'admin',    'Admin',   'MindForge', 'ROLE_ADMIN'),
(2,  'alice@mindforge.local',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '["ROLE_USER"]',              1, NOW(), 'alice',    'Alice',   'Martin',    'ROLE_USER'),
(3,  'bob@mindforge.local',      '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '["ROLE_USER"]',              1, NOW(), 'bob',      'Bob',     'Johnson',   'ROLE_USER'),
(4,  'carol@mindforge.local',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '["ROLE_USER"]',              1, NOW(), 'carol',    'Carol',   'Smith',     'ROLE_USER'),
(5,  'student2@mindforge.local', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '["ROLE_USER"]',              1, NOW(), 'student2', 'David',   'Lee',       'ROLE_USER');

-- ── Profiles ─────────────────────────────────────────────────────
INSERT IGNORE INTO profile (user_id, first_name, last_name, bio, timezone, locale, avatar) VALUES
(1, 'Admin',   'MindForge', 'Platform administrator.',          'UTC',    'en', NULL),
(2, 'Alice',   'Martin',    'Computer science student.',        'UTC',    'en', NULL),
(3, 'Bob',     'Johnson',   'Physics enthusiast.',              'UTC',    'en', NULL),
(4, 'Carol',   'Smith',     'Math lover and algorithm nerd.',   'UTC',    'en', NULL),
(5, 'David',   'Lee',       'Web developer in training.',       'UTC',    'en', NULL);

-- ── Gamification stats ────────────────────────────────────────────
INSERT IGNORE INTO gamification_stats (user_id, total_xp, current_level, streak_days, tasks_completed, total_focus_time) VALUES
(1, 1200, 3, 7,  15, 360),
(2,  850, 2, 5,  10, 210),
(3,  500, 2, 3,   6, 120),
(4,  320, 1, 2,   4,  80),
(5,  150, 1, 1,   2,  35);

-- ── Badges ───────────────────────────────────────────────────────
INSERT IGNORE INTO badge (id, name, icon, rarity, description) VALUES
(1, 'First Session',   '⏱️', 'common',    'Completed your first focus session'),
(2, 'Week Warrior',    '🔥', 'rare',      '7-day streak achieved'),
(3, 'Task Master',     '✅', 'epic',      'Completed 10 tasks'),
(4, 'Study Buddy',     '👥', 'common',    'Joined a virtual room'),
(5, 'Knowledge Sharer','📚', 'rare',      'Uploaded a resource');

-- ── User badges ───────────────────────────────────────────────────
INSERT IGNORE INTO user_badge (user_id, badge_id, earned_at) VALUES
(1, 1, NOW()), (1, 2, NOW()), (1, 3, NOW()),
(2, 1, NOW()), (2, 4, NOW()),
(3, 1, NOW()),
(4, 1, NOW()), (4, 5, NOW());

-- ── Tasks (owner_id = user id) ────────────────────────────────────
INSERT IGNORE INTO task (id, title, description, status, priority, due_date, owner_id, estimated_minutes, created_at) VALUES
(1,  'Algorithms Sprint',       'Study sorting algorithms',          'in_progress', 3, DATE_ADD(NOW(), INTERVAL 3 DAY),  2, 90,  NOW()),
(2,  'Physics Lab Report',      'Write up the pendulum experiment',  'todo',        2, DATE_ADD(NOW(), INTERVAL 5 DAY),  2, 60,  NOW()),
(3,  'Math Problem Set',        'Chapter 5 exercises',               'done',        2, DATE_ADD(NOW(), INTERVAL -1 DAY), 2, 45,  NOW()),
(4,  'Web Dev Project',         'Build the login page',              'in_progress', 3, DATE_ADD(NOW(), INTERVAL 7 DAY),  3, 120, NOW()),
(5,  'Database Schema',         'Design ER diagram for project',     'todo',        2, DATE_ADD(NOW(), INTERVAL 4 DAY),  3, 60,  NOW()),
(6,  'English Essay',           'Write 500-word essay on AI',        'todo',        1, DATE_ADD(NOW(), INTERVAL 10 DAY), 4, 90,  NOW()),
(7,  'Algorithms Sprint',       'Practice dynamic programming',      'in_progress', 3, DATE_ADD(NOW(), INTERVAL 2 DAY),  4, 75,  NOW()),
(8,  'Computer Science Review', 'Review OS concepts',                'done',        2, DATE_ADD(NOW(), INTERVAL -2 DAY), 5, 60,  NOW()),
(9,  'Math Exam Prep',          'Revise linear algebra',             'todo',        3, DATE_ADD(NOW(), INTERVAL 6 DAY),  5, 120, NOW()),
(10, 'History Reading',         'Read chapters 3-5',                 'todo',        1, DATE_ADD(NOW(), INTERVAL 8 DAY),  2, 45,  NOW());

-- ── Exams ─────────────────────────────────────────────────────────
INSERT IGNORE INTO exams (id, title, description, exam_date, duration_minutes, location, importance, owner_id, created_at) VALUES
(1, 'Algorithms',       'Final exam on sorting and graphs',    DATE_ADD(NOW(), INTERVAL 14 DAY), 120, 'Room A101', 5, 2, NOW()),
(2, 'Physics',          'Mechanics and thermodynamics',        DATE_ADD(NOW(), INTERVAL 21 DAY), 90,  'Room B202', 4, 2, NOW()),
(3, 'Mathematics',      'Linear algebra and calculus',         DATE_ADD(NOW(), INTERVAL 10 DAY), 120, 'Room C303', 5, 4, NOW()),
(4, 'Web Development',  'Practical exam — build a REST API',   DATE_ADD(NOW(), INTERVAL 18 DAY), 180, 'Lab 1',     4, 3, NOW()),
(5, 'Databases',        'SQL queries and normalization',       DATE_ADD(NOW(), INTERVAL 25 DAY), 90,  'Room D404', 3, 5, NOW());

-- ── Virtual Rooms ─────────────────────────────────────────────────
INSERT IGNORE INTO virtual_room (id, name, description, is_active, max_participants, created_at, creator_id, subject_id) VALUES
(1, 'Algorithms Sprint',    'Study room for algorithms prep.',                  1, 10, NOW(), 2, 4),
(2, 'Physics Study Group',  'Collaborative physics problem solving.',           1, 10, NOW(), 5, 2),
(3, 'Math Masters',         'Linear algebra and calculus revision.',            1, 15, NOW(), 4, 1),
(4, 'Web Dev Workshop',     'Build projects together and share code.',          1, 10, NOW(), 3, 5),
(5, 'CS Fundamentals',      'Operating systems, networks, and architecture.',   1, 20, NOW(), 1, 3);

-- ── Room participants ─────────────────────────────────────────────
INSERT IGNORE INTO virtual_room_participants (virtual_room_id, user_id) VALUES
(1, 2), (1, 4), (1, 3),
(2, 5), (2, 3),
(3, 4), (3, 2),
(4, 3), (4, 5),
(5, 1), (5, 2), (5, 3);

-- ── Resources ─────────────────────────────────────────────────────
INSERT IGNORE INTO resource (id, title, description, file_path, type, download_count, rating, created_at, updated_at, subject_id, uploader_id) VALUES
(1, 'Sorting Algorithms Cheat Sheet', 'Quick reference for Big-O complexity',    'sorting_cheatsheet.pdf', 'cheat_sheet', 12, 5, NOW(), NOW(), 4, 2),
(2, 'Physics Formula Sheet',          'All mechanics formulas in one page',       'physics_formulas.pdf',   'cheat_sheet',  8, 4, NOW(), NOW(), 2, 3),
(3, 'Linear Algebra Summary',         'Key theorems and proofs',                  'linear_algebra.pdf',     'summary',      6, 5, NOW(), NOW(), 1, 4),
(4, 'SQL Exercises Pack',             'Practice queries with solutions',          'sql_exercises.pdf',      'exercise',     9, 4, NOW(), NOW(), 6, 1),
(5, 'Web Dev Starter Guide',          'HTML, CSS, JS fundamentals',               'webdev_guide.pdf',       'pdf',          5, 4, NOW(), NOW(), 5, 3),
(6, 'Algorithm Design Manual',        'Dynamic programming and greedy methods',   'algo_design.pdf',        'pdf',          7, 5, NOW(), NOW(), 4, 1);

-- ── Focus sessions ────────────────────────────────────────────────
INSERT IGNORE INTO focus_session (id, duration, started_at, ended_at, session_type, user_id, task_id) VALUES
(1, 25, DATE_SUB(NOW(), INTERVAL 2 HOUR),  DATE_SUB(NOW(), INTERVAL 95 MINUTE), 'pomodoro', 2, 1),
(2, 50, DATE_SUB(NOW(), INTERVAL 1 DAY),   DATE_SUB(NOW(), INTERVAL 23 HOUR),   'pomodoro', 2, 3),
(3, 35, DATE_SUB(NOW(), INTERVAL 3 HOUR),  DATE_SUB(NOW(), INTERVAL 145 MINUTE),'pomodoro', 3, 4),
(4, 25, DATE_SUB(NOW(), INTERVAL 5 HOUR),  DATE_SUB(NOW(), INTERVAL 280 MINUTE),'pomodoro', 4, 7),
(5, 25, DATE_SUB(NOW(), INTERVAL 30 MINUTE),DATE_SUB(NOW(), INTERVAL 5 MINUTE), 'pomodoro', 5, 8),
(6, 50, DATE_SUB(NOW(), INTERVAL 2 DAY),   DATE_SUB(NOW(), INTERVAL 47 HOUR),   'pomodoro', 2, 1),
(7, 35, DATE_SUB(NOW(), INTERVAL 4 HOUR),  DATE_SUB(NOW(), INTERVAL 185 MINUTE),'pomodoro', 3, 5);

-- ── Shared tasks (challenges) ─────────────────────────────────────
INSERT IGNORE INTO shared_task (id, title, description, status, difficulty, category, created_at, shared_by_id, shared_with_id) VALUES
(1, 'Solve 5 DP problems',    'Complete 5 dynamic programming problems on LeetCode', 'pending',  'hard',   'tech_skills', NOW(), 2, 4),
(2, 'Read Clean Code ch.1-3', 'Read and summarize the first 3 chapters',             'accepted', 'medium', 'soft_skills', NOW(), 4, 2),
(3, '30-min run',             'Go for a 30-minute run today',                        'pending',  'easy',   'physical',    NOW(), 3, 5),
(4, 'Draw a system diagram',  'Design a microservices architecture diagram',         'pending',  'medium', 'creative',    NOW(), 1, 3);

-- ── Claims (support tickets) ──────────────────────────────────────
INSERT IGNORE INTO claim (id, title, description, status, priority, created_at, created_by_id) VALUES
(1, 'Cannot access virtual room', 'Getting 403 error when trying to join room #2', 'open',        'high',   NOW(), 3),
(2, 'Resource download broken',   'PDF download returns 404 for resource #3',      'in_progress', 'medium', NOW(), 5),
(3, 'Profile photo not saving',   'Avatar upload completes but photo not shown',   'resolved',    'low',    NOW(), 2);

SET FOREIGN_KEY_CHECKS = 1;
