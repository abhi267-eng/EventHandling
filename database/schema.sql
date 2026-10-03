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

    website VARCHAR(255),
    logo_url TEXT,

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
        UNIQUE (college_id, department_name),

    CONSTRAINT unique_department_code_per_college
        UNIQUE (college_id, department_code),

    CONSTRAINT unique_department_id_college
        UNIQUE (department_id, college_id)
);


-- ============================================================
-- 3. ORGANIZATIONS / COUNCILS
-- ============================================================

CREATE TABLE organizations (
    organization_id BIGSERIAL PRIMARY KEY,

    college_id BIGINT NOT NULL,

    organization_name VARCHAR(150) NOT NULL,
    organization_type VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_organization_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE CASCADE,

    CONSTRAINT unique_organization_per_college
        UNIQUE (college_id, organization_name),

    CONSTRAINT unique_organization_id_college
        UNIQUE (organization_id, college_id),

    CONSTRAINT check_organization_type
        CHECK (
            organization_type IN (
                'COUNCIL',
                'SOCIAL',
                'REPRESENTATIVE_BODY',
                'CLUB',
                'OTHER'
            )
        )
);


-- ============================================================
-- 4. USERS
-- ============================================================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    phone VARCHAR(20),
    profile_picture_url TEXT,

    college_id BIGINT,

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

CREATE TABLE email_verification_codes (
    verification_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 5. USER ↔ DEPARTMENT
-- Many-to-Many Relationship
-- ============================================================

CREATE TABLE user_departments (
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, department_id),

    CONSTRAINT fk_user_department_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_department_department
        FOREIGN KEY (department_id)
        REFERENCES departments(department_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 6. USER ↔ ORGANIZATION
-- Many-to-Many Relationship
-- ============================================================

CREATE TABLE user_organizations (
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, organization_id),

    CONSTRAINT fk_user_organization_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_organization_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(organization_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 7. ROLE APPLICATIONS
-- Used for elevated-role signup
--
-- Normal users do NOT create an application.
-- They directly become users after email verification.
-- ============================================================

CREATE TABLE role_applications (
    application_id BIGSERIAL PRIMARY KEY,

    role VARCHAR(30) NOT NULL,

    -- Applicant personal information
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),

    password_hash VARCHAR(255) NOT NULL,

    profile_picture_url TEXT,

    -- College information
    college_id BIGINT,

    college_id_number VARCHAR(100),
    college_id_photo_url TEXT,

    designation VARCHAR(100),

    -- Requested authority
    requested_department_id BIGINT,
    requested_organization_id BIGINT,

    -- New college information
    -- Used only when a College Admin is introducing
    -- a college that does not already exist.
    proposed_college_name VARCHAR(200),
    proposed_college_code VARCHAR(50),
    proposed_email_domain VARCHAR(100),
    proposed_address TEXT,
    proposed_city VARCHAR(100),
    proposed_latitude DECIMAL(9,6),
    proposed_longitude DECIMAL(9,6),
    proposed_website VARCHAR(255),
    proposed_logo_url TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    rejection_reason TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_college
        FOREIGN KEY (college_id)
        REFERENCES colleges(college_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_application_department
        FOREIGN KEY (requested_department_id, college_id)
        REFERENCES departments(department_id, college_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_application_organization
        FOREIGN KEY (requested_organization_id, college_id)
        REFERENCES organizations(organization_id, college_id)
        ON DELETE SET NULL,

    CONSTRAINT check_application_role
        CHECK (
            role IN (
                'COLLEGE_ADMIN',
                'EVENT_COORDINATOR',
                'FACULTY_COORDINATOR',
                'STUDENT_ORGANIZER'
            )
        ),

    CONSTRAINT check_application_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED'
            )
        )
);


-- ============================================================
-- 8. ROLE APPLICATION REVIEWS
-- Allows multiple people to review one application.
-- Example:
-- Student Organizer → Faculty Coordinator + Event Coordinator
-- ============================================================

CREATE TABLE role_application_reviews (
    review_id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    reviewer_user_id BIGINT NOT NULL,
    reviewer_role VARCHAR(30) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    comments TEXT,

    reviewed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_review_application
        FOREIGN KEY (application_id)
        REFERENCES role_applications(application_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_application_review_reviewer
        FOREIGN KEY (reviewer_user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT check_application_reviewer_role
        CHECK (
            reviewer_role IN (
                'PLATFORM_ADMIN',
                'COLLEGE_ADMIN',
                'EVENT_COORDINATOR',
                'FACULTY_COORDINATOR'
            )
        ),

    CONSTRAINT check_application_review_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED'
            )
        )
);


-- ============================================================
-- 9. EVENT CATEGORIES
-- ============================================================

CREATE TABLE event_categories (
    category_id BIGSERIAL PRIMARY KEY,

    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 10. EVENTS
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
-- 11. SAVED EVENTS
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
-- 12. EVENT REVIEWS
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
-- 13. INDEXES
-- ============================================================

-- Users
CREATE INDEX idx_users_college
    ON users(college_id);

CREATE INDEX idx_users_email
    ON users(email);


-- Departments
CREATE INDEX idx_departments_college
    ON departments(college_id);


-- Organizations
CREATE INDEX idx_organizations_college
    ON organizations(college_id);


-- User affiliations
CREATE INDEX idx_user_departments_department
    ON user_departments(department_id);

CREATE INDEX idx_user_organizations_organization
    ON user_organizations(organization_id);


-- Role applications
CREATE INDEX idx_role_applications_email
    ON role_applications(email);

CREATE INDEX idx_role_applications_status
    ON role_applications(status);

CREATE INDEX idx_role_applications_role
    ON role_applications(role);

CREATE INDEX idx_role_application_reviews_application
    ON role_application_reviews(application_id);

CREATE INDEX idx_role_application_reviews_reviewer
    ON role_application_reviews(reviewer_user_id);

CREATE INDEX idx_role_application_reviews_status
    ON role_application_reviews(status);


-- Events
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


-- Saved events
CREATE INDEX idx_saved_events_user
    ON saved_events(user_id);


-- Reviews
CREATE INDEX idx_reviews_event
    ON event_reviews(event_id);

CREATE INDEX idx_email_verification_codes_user_id
    ON email_verification_codes(user_id);