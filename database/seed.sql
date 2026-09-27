-- ============================================================
-- EventHandling Seed Data
-- PostgreSQL
-- ============================================================


-- ============================================================
-- 1. COLLEGES
-- ============================================================

INSERT INTO colleges
    (college_name, college_code, email_domain, city, address, latitude, longitude)
VALUES
    (
        'Vasantdada Patil Pratishthan''s College of Engineering and Visual Arts',
        'VPPCOEVA',
        'vppcoe.ac.in',
        'Mumbai',
        'Sion-Panvel Highway, Eastern Express Highway Junction, Sion, Mumbai, Maharashtra',
        19.044700,
        72.878500
    ),
    (
        'University of Mumbai',
        'MU',
        'mu.ac.in',
        'Mumbai',
        'Vidyanagari, Kalina, Santacruz East, Mumbai, Maharashtra',
        19.072800,
        72.858700
    );


-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================

INSERT INTO departments
    (college_id, department_name, department_code)
VALUES
    (1, 'Computer Engineering', 'CE'),
    (1, 'Artificial Intelligence and Machine Learning', 'AIML'),
    (1, 'Information Technology', 'IT'),
    (1, 'Electronics and Computer Science', 'ECS'),
    (1, 'Mechanical Engineering', 'ME'),
    (1, 'Electronics and Telecommunication Engineering', 'EXTC');


-- ============================================================
-- 3. EVENT CATEGORIES
-- ============================================================

INSERT INTO event_categories
    (category_name, description)
VALUES
    ('Technical', 'Technical events related to computing, engineering and technology.'),
    ('Cultural', 'Cultural and creative events.'),
    ('Sports', 'Sports and athletic events.'),
    ('Workshop', 'Practical workshops and skill-development sessions.'),
    ('Seminar', 'Seminars, talks and knowledge-sharing sessions.'),
    ('Competition', 'Competitions, contests and challenges.'),
    ('Social', 'Social and community-oriented events.'),
    ('Other', 'Events that do not fit into another category.');