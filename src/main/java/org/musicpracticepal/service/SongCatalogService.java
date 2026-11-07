package org.musicpracticepal.service;

import java.util.List;

import org.musicpracticepal.model.Song;
import org.musicpracticepal.repository.SongLinkRepository;
import org.musicpracticepal.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SongCatalogService {

    private final SongRepository songRepository;
    private final SongLinkRepository songLinkRepository;

    public SongCatalogService(SongRepository songRepository, SongLinkRepository songLinkRepository) {
        this.songRepository = songRepository;
        this.songLinkRepository = songLinkRepository;
    }

    @Transactional
    public boolean addSong(String name) {
        String normalizedName = name.trim();
        if (songRepository.existsByNameIgnoreCase(normalizedName)) {
            return false;
        }
        List<Song> songs = songRepository.findAllByOrderByPositionAsc();
        songRepository.save(new Song(normalizedName, null,
                songs.stream().mapToInt(Song::getPosition).max().orElse(-1) + 1));
        return true;
    }

    @Transactional
    public int removeSong(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
        if (songRepository.count() <= 1) {
            throw new IllegalStateException("The catalog must contain at least one song");
        }
        int removedPosition = song.getPosition();
        songLinkRepository.deleteBySong_Id(songId);
        songRepository.delete(song);
        songRepository.findAllByOrderByPositionAsc().stream()
                .filter(remainingSong -> remainingSong.getPosition() > removedPosition)
                .forEach(remainingSong -> remainingSong.setPosition(remainingSong.getPosition() - 1));
        return removedPosition;
    }
}
