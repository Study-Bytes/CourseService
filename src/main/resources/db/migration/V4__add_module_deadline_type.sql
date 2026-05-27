ALTER TABLE course_modules
    ADD COLUMN deadline_type VARCHAR(32) DEFAULT 'NONE' NOT NULL;

ALTER TABLE course_modules
    ADD COLUMN time_limit_minutes INTEGER;

UPDATE course_modules
SET deadline_type = 'ABSOLUTE'
WHERE deadline_at IS NOT NULL;

ALTER TABLE course_modules
    ADD CONSTRAINT chk_course_modules_deadline_type
        CHECK (deadline_type IN ('NONE', 'ABSOLUTE', 'RELATIVE_FROM_START'));

ALTER TABLE course_modules
    ADD CONSTRAINT chk_course_modules_deadline_values
        CHECK (
            (deadline_type = 'NONE' AND deadline_at IS NULL AND time_limit_minutes IS NULL)
            OR (deadline_type = 'ABSOLUTE' AND deadline_at IS NOT NULL AND time_limit_minutes IS NULL)
            OR (deadline_type = 'RELATIVE_FROM_START' AND deadline_at IS NULL AND time_limit_minutes IS NOT NULL AND time_limit_minutes > 0)
        );
