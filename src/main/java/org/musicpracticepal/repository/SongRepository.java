package org.musicpracticepal.repository;

import java.util.List;

import org.musicpracticepal.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findBySongNameOrderByIdAsc(String songName);

    boolean existsBySongNameAndLinkUrl(String songName, String linkUrl);
}
