package org.musicpracticepal.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "practice_lesson", uniqueConstraints = {
        @UniqueConstraint(name = "uq_practice_lesson_name", columnNames = "name")
})
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int position;

    @ElementCollection
    @CollectionTable(name = "lesson_link", joinColumns = @JoinColumn(name = "lesson_id"))
    @OrderColumn(name = "link_order")
    @Column(name = "link_url", nullable = false, length = 2048)
    private List<String> links = new ArrayList<>();

    protected Lesson() {}

    public Lesson(String name, List<String> links, int position) {
        this.name = name;
        this.links = new ArrayList<>(links);
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public List<String> getLinks() {
        return List.copyOf(links);
    }
}
