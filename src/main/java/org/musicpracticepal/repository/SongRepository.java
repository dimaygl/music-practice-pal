package org.musicpracticepal.repository;

import java.util.List;
import java.util.Optional;

import org.musicpracticepal.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findAllByOrderByPositionAsc();

    Optional<Song> findByName(String name);

    boolean existsByName(String name);
}
