ALTER TABLE courses
    ADD COLUMN submitted_for_review_at TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE courses
    ADD COLUMN reviewed_at TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE courses
    ADD COLUMN reviewed_by_user_id BIGINT;

ALTER TABLE courses
    ADD COLUMN review_comment TEXT;

ALTER TABLE courses
    DROP CONSTRAINT IF EXISTS chk_courses_status;

ALTER TABLE courses
    ADD CONSTRAINT chk_courses_status
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'CHANGES_REQUESTED', 'PUBLISHED', 'ARCHIVED'));
