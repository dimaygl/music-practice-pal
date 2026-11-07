package org.musicpracticepal.service;

import java.util.List;

import org.musicpracticepal.model.Lesson;
import org.musicpracticepal.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonCatalogService {

    private final LessonRepository lessonRepository;

    public LessonCatalogService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Transactional
    public boolean addLesson(String name) {
        String normalizedName = name.trim();
        if (lessonRepository.existsByNameIgnoreCase(normalizedName)) {
            return false;
        }
        List<Lesson> lessons = lessonRepository.findAllByOrderByPositionAsc();
        lessonRepository.save(new Lesson(
                normalizedName,
                null,
                lessons.stream().mapToInt(Lesson::getPosition).max().orElse(-1) + 1));
        return true;
    }

    @Transactional
    public int removeLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
        if (lessonRepository.count() <= 1) {
            throw new IllegalStateException("The catalog must contain at least one lesson");
        }
        int removedPosition = lesson.getPosition();
        lessonRepository.delete(lesson);
        lessonRepository.findAllByOrderByPositionAsc().stream()
                .filter(remainingLesson -> remainingLesson.getPosition() > removedPosition)
                .forEach(remainingLesson -> remainingLesson.setPosition(remainingLesson.getPosition() - 1));
        return removedPosition;
    }
}
