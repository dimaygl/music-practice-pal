package org.musicpracticepal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "song_link", uniqueConstraints = {
        @UniqueConstraint(name = "uq_song_link_url", columnNames = {"song_id", "link_url"})
})
public class SongLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "link_url", nullable = false, length = 2048)
    private String linkUrl;

    public SongLink(Song song, String linkUrl) {
        this.song = song;
        this.linkUrl = linkUrl;
    }

}
