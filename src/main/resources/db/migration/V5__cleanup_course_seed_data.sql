UPDATE courses
SET title = REPLACE(REPLACE(title, 'Pyhton', 'Python'), 'pyhton', 'python')
WHERE title LIKE '%Pyhton%' OR title LIKE '%pyhton%';

UPDATE courses
SET short_description = REPLACE(REPLACE(short_description, 'Pyhton', 'Python'), 'pyhton', 'python')
WHERE short_description LIKE '%Pyhton%' OR short_description LIKE '%pyhton%';

UPDATE courses
SET description = REPLACE(REPLACE(description, 'Pyhton', 'Python'), 'pyhton', 'python')
WHERE description LIKE '%Pyhton%' OR description LIKE '%pyhton%';

UPDATE courses
SET title = REPLACE(REPLACE(title, 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE title LIKE '%begginers%' OR title LIKE '%Begginers%';

UPDATE courses
SET short_description = REPLACE(REPLACE(short_description, 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE short_description LIKE '%begginers%' OR short_description LIKE '%Begginers%';

UPDATE courses
SET description = REPLACE(REPLACE(description, 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE description LIKE '%begginers%' OR description LIKE '%Begginers%';

UPDATE course_modules
SET title = REPLACE(REPLACE(REPLACE(REPLACE(title, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE title LIKE '%Pyhton%' OR title LIKE '%pyhton%' OR title LIKE '%begginers%' OR title LIKE '%Begginers%';

UPDATE course_modules
SET description = REPLACE(REPLACE(REPLACE(REPLACE(description, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE description LIKE '%Pyhton%' OR description LIKE '%pyhton%' OR description LIKE '%begginers%' OR description LIKE '%Begginers%';

UPDATE course_items
SET title = REPLACE(REPLACE(REPLACE(REPLACE(title, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE title LIKE '%Pyhton%' OR title LIKE '%pyhton%' OR title LIKE '%begginers%' OR title LIKE '%Begginers%';

UPDATE course_items
SET statement = REPLACE(REPLACE(REPLACE(REPLACE(statement, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE statement LIKE '%Pyhton%' OR statement LIKE '%pyhton%' OR statement LIKE '%begginers%' OR statement LIKE '%Begginers%';

UPDATE course_item_content_blocks
SET title = REPLACE(REPLACE(REPLACE(REPLACE(title, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE title LIKE '%Pyhton%' OR title LIKE '%pyhton%' OR title LIKE '%begginers%' OR title LIKE '%Begginers%';

UPDATE course_item_content_blocks
SET text_content = REPLACE(REPLACE(REPLACE(REPLACE(text_content, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE text_content LIKE '%Pyhton%' OR text_content LIKE '%pyhton%' OR text_content LIKE '%begginers%' OR text_content LIKE '%Begginers%';

UPDATE course_item_hints
SET text = REPLACE(REPLACE(REPLACE(REPLACE(text, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE text LIKE '%Pyhton%' OR text LIKE '%pyhton%' OR text LIKE '%begginers%' OR text LIKE '%Begginers%';

UPDATE course_item_options
SET text = REPLACE(REPLACE(REPLACE(REPLACE(text, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE text LIKE '%Pyhton%' OR text LIKE '%pyhton%' OR text LIKE '%begginers%' OR text LIKE '%Begginers%';

UPDATE course_item_options
SET explanation = REPLACE(REPLACE(REPLACE(REPLACE(explanation, 'Pyhton', 'Python'), 'pyhton', 'python'), 'begginers', 'beginners'), 'Begginers', 'Beginners')
WHERE explanation LIKE '%Pyhton%' OR explanation LIKE '%pyhton%' OR explanation LIKE '%begginers%' OR explanation LIKE '%Begginers%';

UPDATE course_items
SET starter_code = NULL,
    language = NULL,
    time_limit_ms = NULL,
    memory_limit_mb = NULL,
    output_limit_kb = NULL
WHERE item_type IN ('THEORY', 'FILE', 'QUIZ');

DELETE FROM courses
WHERE LOWER(TRIM(title)) = 'fff'
   OR LOWER(TRIM(slug)) = 'fff';

DELETE FROM courses
WHERE (
      LOWER(TRIM(title)) IN ('lesson 1', 'test lesson 1')
      OR LOWER(TRIM(slug)) IN ('lesson-1', 'lesson1', 'test-lesson-1', 'test_lesson_1')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM course_modules
      WHERE course_modules.course_id = courses.id
  );

DELETE FROM course_modules
WHERE LOWER(TRIM(title)) = 'fff';

DELETE FROM course_modules
WHERE LOWER(TRIM(title)) IN ('lesson 1', 'test lesson 1')
  AND NOT EXISTS (
      SELECT 1
      FROM course_items
      WHERE course_items.module_id = course_modules.id
  );

DELETE FROM course_items
WHERE LOWER(TRIM(title)) = 'fff';

DELETE FROM course_items
WHERE LOWER(TRIM(title)) IN ('lesson 1', 'test lesson 1')
  AND COALESCE(TRIM(statement), '') = ''
  AND NOT EXISTS (
      SELECT 1
      FROM course_item_content_blocks
      WHERE course_item_content_blocks.item_id = course_items.id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM course_item_test_cases
      WHERE course_item_test_cases.item_id = course_items.id
  )
  AND NOT EXISTS (
      SELECT 1
      FROM course_item_options
      WHERE course_item_options.item_id = course_items.id
  );

DELETE FROM course_item_content_blocks
WHERE LOWER(TRIM(COALESCE(title, ''))) = 'fff'
   OR LOWER(TRIM(COALESCE(text_content, ''))) = 'fff'
   OR (
      LOWER(TRIM(COALESCE(title, ''))) IN ('lesson 1', 'test lesson 1')
      AND COALESCE(TRIM(text_content), '') = ''
  )
   OR (
      LOWER(TRIM(COALESCE(text_content, ''))) IN ('lesson 1', 'test lesson 1')
      AND COALESCE(TRIM(title), '') = ''
  );

DELETE FROM course_item_hints
WHERE LOWER(TRIM(text)) = 'fff';

DELETE FROM course_item_options
WHERE LOWER(TRIM(text)) = 'fff';

UPDATE courses
SET cover_image_url = CASE
    WHEN LOWER(CONCAT(COALESCE(title, ''), ' ', COALESCE(slug, ''))) LIKE '%python%' THEN 'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?auto=format&fit=crop&w=1200&q=80'
    WHEN LOWER(CONCAT(COALESCE(title, ''), ' ', COALESCE(slug, ''))) LIKE '%java%' THEN 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80'
    WHEN LOWER(CONCAT(COALESCE(title, ''), ' ', COALESCE(slug, ''))) LIKE '%sql%' THEN 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=1200&q=80'
    WHEN LOWER(CONCAT(COALESCE(title, ''), ' ', COALESCE(slug, ''))) LIKE '%database%' THEN 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?auto=format&fit=crop&w=1200&q=80'
    WHEN LOWER(CONCAT(COALESCE(title, ''), ' ', COALESCE(slug, ''))) LIKE '%network%' THEN 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1200&q=80'
    ELSE 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80'
END
WHERE status = 'PUBLISHED'
  AND (
      cover_image_url IS NULL
      OR TRIM(cover_image_url) = ''
      OR LOWER(cover_image_url) LIKE 'https://example.com/%'
      OR LOWER(cover_image_url) LIKE 'http://example.com/%'
  );
