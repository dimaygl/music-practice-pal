package org.musicpracticepal.repository;

import java.util.List;

import org.musicpracticepal.model.Lesson;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @EntityGraph(attributePaths = "links")
    List<Lesson> findAllByOrderByPositionAsc();

    boolean existsByName(String name);
}
