package org.musicpracticepal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
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

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "chart_image_data", length = 10 * 1024 * 1024)
    private byte[] chartImageData;

    @Column(name = "chart_image_content_type", length = 32)
    private String chartImageContentType;

    @Setter
    @Column(nullable = false)
    private int position;

    protected Song() {}

    public Song(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public void setChartImageData(byte[] chartImageData, String contentType) {
        this.chartImageData = chartImageData;
        this.chartImageContentType = contentType;
    }

}
