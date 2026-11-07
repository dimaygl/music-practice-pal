INSERT INTO practice_song (name, chart_image, position)
SELECT 'song-1', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM practice_song WHERE name = 'song-1'
);

INSERT INTO practice_lesson (name, position)
SELECT 'lesson-1', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM practice_lesson
    WHERE name = 'lesson-1'
);

