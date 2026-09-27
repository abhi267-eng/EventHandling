-- ============================================================
-- EventHandling Database Schema
-- PostgreSQL
-- ============================================================


-- ============================================================
-- 1. COLLEGES
-- ============================================================

CREATE TABLE colleges (
    college_id BIGSERIAL PRIMARY KEY,
    college_name VARCHAR(200) NOT NULL,
    college_code VARCHAR(50) UNIQUE,
    email_domain VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================

CREATE TABLE departments (
    department_id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL,
    department_name VARCHAR(150) NOT NULL,
    department_code VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_department_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE CASCADE,

    CONSTRAINT unique_department_per_college
        UNIQUE (college_id, department_name)
);


-- ============================================================
-- 3. USERS
-- ============================================================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    college_id BIGINT,
    department_id BIGINT,

    role VARCHAR(30) NOT NULL DEFAULT 'NORMAL_USER',

    designation VARCHAR(100),

    email_verified BOOLEAN NOT NULL DEFAULT FALSE,

    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_user_department
        FOREIGN KEY (department_id)
        REFERENCES departments(department_id)
        ON DELETE SET NULL,

    CONSTRAINT check_user_role
        CHECK (
            role IN (
                'PLATFORM_ADMIN',
                'COLLEGE_ADMIN',
                'EVENT_COORDINATOR',
                'FACULTY_COORDINATOR',
                'STUDENT_ORGANIZER',
                'NORMAL_USER'
            )
        ),

    CONSTRAINT check_account_status
        CHECK (
            account_status IN (
                'ACTIVE',
                'SUSPENDED'
            )
        )
);


-- ============================================================
-- 4. EVENT CATEGORIES
-- ============================================================

CREATE TABLE event_categories (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. EVENTS
-- ============================================================

CREATE TABLE events (
    event_id BIGSERIAL PRIMARY KEY,

    college_id BIGINT NOT NULL,
    created_by BIGINT,

    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,

    category_id BIGINT NOT NULL,

    rules TEXT,

    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,

    venue_type VARCHAR(20) NOT NULL,

    venue_name VARCHAR(255) NOT NULL,
    venue_address TEXT NOT NULL,

    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),

    accept_visitors BOOLEAN NOT NULL DEFAULT FALSE,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',

    poster_url TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_event_creator
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_event_category
        FOREIGN KEY (category_id)
        REFERENCES event_categories(category_id)
        ON DELETE RESTRICT,

    CONSTRAINT check_venue_type
        CHECK (
            venue_type IN (
                'COLLEGE_CAMPUS',
                'EXTERNAL_VENUE'
            )
        ),

    CONSTRAINT check_event_status
        CHECK (
            status IN (
                'DRAFT',
                'PENDING',
                'APPROVED',
                'REJECTED'
            )
        ),

    CONSTRAINT check_verification_status
        CHECK (
            verification_status IN (
                'UNVERIFIED',
                'VERIFIED'
            )
        ),

    CONSTRAINT check_event_datetime
        CHECK (end_datetime > start_datetime)
);


-- ============================================================
-- 6. SAVED EVENTS
-- ============================================================

CREATE TABLE saved_events (
    saved_event_id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_saved_event_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_saved_event_event
        FOREIGN KEY (event_id)
        REFERENCES events(event_id)
        ON DELETE CASCADE,

    CONSTRAINT unique_saved_event
        UNIQUE (user_id, event_id)
);


-- ============================================================
-- 7. EVENT REVIEWS
-- ============================================================

CREATE TABLE event_reviews (
    review_id BIGSERIAL PRIMARY KEY,

    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    rating INTEGER NOT NULL,

    review_text TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_event
        FOREIGN KEY (event_id)
        REFERENCES events(event_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_review_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT check_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT unique_user_event_review
        UNIQUE (user_id, event_id)
);


-- ============================================================
-- 8. VERIFICATION REQUESTS
-- ============================================================

CREATE TABLE verification_requests (
    request_id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    requested_role VARCHAR(30) NOT NULL,

    designation VARCHAR(100),

    college_id BIGINT,
    department_id BIGINT,

    reason TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_verification_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_verification_department
        FOREIGN KEY (department_id)
        REFERENCES departments(department_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_verification_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT check_requested_role
        CHECK (
            requested_role IN (
                'COLLEGE_ADMIN',
                'EVENT_COORDINATOR',
                'FACULTY_COORDINATOR',
                'STUDENT_ORGANIZER'
            )
        ),

    CONSTRAINT check_verification_request_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED'
            )
        )
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_college
    ON users(college_id);

CREATE INDEX idx_users_department
    ON users(department_id);

CREATE INDEX idx_events_college
    ON events(college_id);

CREATE INDEX idx_events_created_by
    ON events(created_by);

CREATE INDEX idx_events_category
    ON events(category_id);

CREATE INDEX idx_events_start_datetime
    ON events(start_datetime);

CREATE INDEX idx_events_city_location
    ON colleges(city);

CREATE INDEX idx_saved_events_user
    ON saved_events(user_id);

CREATE INDEX idx_reviews_event
    ON event_reviews(event_id);

CREATE INDEX idx_verification_requests_status
    ON verification_requests(status);
