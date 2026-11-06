package org.musicpracticepal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "song", uniqueConstraints = {
        @UniqueConstraint(name = "uq_song_links_song_name_url", columnNames = {"song_name", "link_url"})
})
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_name", nullable = false, length = 255)
    private String songName;

    @Column(name = "link_url", nullable = false, length = 2048)
    private String linkUrl;

    protected Song() {}

    public Song(String songName, String linkUrl) {
        this.songName = songName;
        this.linkUrl = linkUrl;
    }

}
