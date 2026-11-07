INSERT INTO song (name, position)
SELECT 'song-1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM song WHERE name = 'song-1'
);

INSERT INTO lesson (name, position)
SELECT 'lesson-1', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM lesson
    WHERE name = 'lesson-1'
);
