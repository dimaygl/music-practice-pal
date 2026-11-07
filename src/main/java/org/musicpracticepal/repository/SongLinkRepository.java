package org.musicpracticepal.repository;

import java.util.List;

import org.musicpracticepal.model.SongLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongLinkRepository extends JpaRepository<SongLink, Long> {

    List<SongLink> findBySong_NameOrderByIdAsc(String songName);

    boolean existsBySong_NameAndLinkUrl(String songName, String linkUrl);

    void deleteBySong_Id(Long songId);
}
