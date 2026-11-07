package org.musicpracticepal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "practice_song", uniqueConstraints = {
        @UniqueConstraint(name = "uq_practice_song_name", columnNames = "name")
})
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "chart_image", length = 2048)
    private String chartImage;

    @Column(nullable = false)
    private int position;

    protected Song() {}

    public Song(String name, String chartImage, int position) {
        this.name = name;
        this.chartImage = chartImage;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getChartImage() {
        return chartImage;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
