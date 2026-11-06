package org.musicpracticepal.repository;

import java.util.List;

import org.musicpracticepal.model.SongLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongLinkRepository extends JpaRepository<SongLink, Long> {

    List<SongLink> findBySongNameOrderByIdAsc(String songName);

    boolean existsBySongNameAndLinkUrl(String songName, String linkUrl);
}
