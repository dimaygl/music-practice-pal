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
@Table(name = "song", uniqueConstraints = {
        @UniqueConstraint(name = "uq_song_name", columnNames = "name")
})
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Setter
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "song_chart", length = 10 * 1024 * 1024)
    private byte[] songChart;

    @Setter
    @Column(name = "song_chart_content_type", length = 32)
    private String songChartContentType;

    @Setter
    @Column(nullable = false)
    private int position;

    protected Song() {}

    public Song(String name, int position) {
        this.name = name;
        this.position = position;
    }

}
