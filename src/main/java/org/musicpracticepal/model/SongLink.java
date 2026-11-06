package org.musicpracticepal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "song_links", uniqueConstraints = {
        @UniqueConstraint(name = "uq_song_links_song_name_url", columnNames = {"song_name", "link_url"})
})
public class SongLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_name", nullable = false, length = 255)
    private String songName;

    @Column(name = "link_url", nullable = false, length = 2048)
    private String linkUrl;

    protected SongLink() {}

    public SongLink(String songName, String linkUrl) {
        this.songName = songName;
        this.linkUrl = linkUrl;
    }

    public Long getId() {
        return id;
    }

    public String getSongName() {
        return songName;
    }

    public String getLinkUrl() {
        return linkUrl;
    }
}
