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

@Getter
@Entity
@Table(name = "lesson", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_name", columnNames = "name")
})
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private int position;

    @Setter
    @Column(name = "link_url", length = 2048)
    private String linkUrl;

    protected Lesson() {}

    public Lesson(String name, String linkUrl, int position) {
        this.name = name;
        this.linkUrl = linkUrl;
        this.position = position;
    }

}
