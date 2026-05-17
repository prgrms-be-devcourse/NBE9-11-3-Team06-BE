DELETE FROM adjust_result;
DELETE FROM time_info;
DELETE FROM date_info;
DELETE FROM time_table;
DELETE FROM available_time;
DELETE FROM available_date_time;
DELETE FROM time_block;
DELETE FROM participant;
DELETE FROM meeting;
DELETE FROM member;

-- =========================
-- 0️⃣ MEMBER (필수)
-- =========================
INSERT INTO member (id, email, password_hash, nickname, timezone, created_at, modified_at)
VALUES
(1, 'user1@test.com', 'pass', 'user1', 'ASIA_SEOUL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================
-- 1️⃣ MEETING
-- =========================
INSERT INTO meeting (id, title, status, category, duration, local_time, random_url, member_id, created_at, modified_at)
VALUES
(1, '회의1', 'PENDING', 'GENERAL', 60, 'ASIA/SEOUL', 'url1', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '회의2', 'PENDING', 'GENERAL', 60, 'ASIA/SEOUL', 'url2', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '회의3', 'PENDING', 'GENERAL', 60, 'ASIA/SEOUL', 'url3', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '회의4', 'PENDING', 'GENERAL', 60, 'ASIA/SEOUL', 'url4', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '회의5', 'PENDING', 'GENERAL', 60, 'ASIA/SEOUL', 'url5', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO time_table (id, meeting_id)
VALUES (1, 1);

INSERT INTO time_table (id, meeting_id)
VALUES (2, 2);

INSERT INTO time_table (id, meeting_id)
VALUES (3, 3);

INSERT INTO time_table (id, meeting_id)
VALUES (4, 4);

INSERT INTO time_table (id, meeting_id)
VALUES (5, 5);
-- =========================
-- 2️⃣ PARTICIPANT
-- =========================
INSERT INTO participant (id, guest_name, guest_password, meeting_id, created_at, modified_at)
VALUES
(1, '철수', '1234', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '영희', '1234', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '민수', '1234', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '지훈', '1234', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '수지', '1234', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'A', '1234', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'B', '1234', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, '새참가자', '1234', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================
-- 3️⃣ TIME_BLOCK
-- =========================
INSERT INTO time_block (id, meeting_id, participant_id, created_by, created_at, modified_at)
VALUES
(1, 1, 1, '철수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 2, '영희', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 3, '민수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 4, '지훈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, 5, '수지', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 3, 6, 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 3, 7, 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================
-- 4️⃣ AVAILABLE_DATE_TIME
-- =========================
INSERT INTO available_date_time (id, date, meeting_id, time_block_id, created_by, created_at, modified_at)
VALUES
(1, '2024-05-20', 1, 1, '철수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '2024-05-20', 1, 2, '영희', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '2024-05-20', 1, 3, '민수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '2024-05-21', 2, 4, '지훈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '2024-05-22', 2, 5, '수지', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '2024-05-23', 3, 6, 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, '2024-05-23', 3, 7, 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =========================
-- 5️⃣ AVAILABLE_TIME
-- =========================
INSERT INTO available_time (id, time, available_date_time_id, meeting_id, time_block_id, created_by, created_at, modified_at)
VALUES
-- meeting1
(1, '09:00', 1, 1, 1, '철수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '09:30', 1, 1, 1, '철수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '09:00', 2, 1, 2, '영희', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '11:00', 3, 1, 3, '민수', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- meeting2
(5, '10:00', 4, 2, 4, '지훈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '10:00', 5, 2, 5, '수지', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- meeting3
(7, '09:00', 6, 3, 6, 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, '09:00', 7, 3, 7, 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE time_block ALTER COLUMN id RESTART WITH 100;
ALTER TABLE participant ALTER COLUMN id RESTART WITH 100;
ALTER TABLE available_date_time ALTER COLUMN id RESTART WITH 100;
ALTER TABLE available_time ALTER COLUMN id RESTART WITH 100;
ALTER TABLE time_table ALTER COLUMN id RESTART WITH 100;
ALTER TABLE meeting ALTER COLUMN id RESTART WITH 100;
ALTER TABLE member ALTER COLUMN id RESTART WITH 100;