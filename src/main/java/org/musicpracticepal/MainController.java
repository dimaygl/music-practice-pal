package org.musicpracticepal;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Controller
public class MainController {

    record RelativeMinor(String relativeMinor, String backingTrackUrl) {}

    record Song(String name, List<String> links) {
        Song {
            name = name.trim();
            links = List.copyOf(links);
        }
    }

    record Lesson(String name, List<String> links) {
        Lesson {
            name = name.trim();
            links = List.copyOf(links);
        }
    }

    private record PracticeData(
            List<Song> songs, List<Lesson> lessons, LinkedHashMap<String, RelativeMinor> scales) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);

    private final List<Song> songs;
    private final List<Lesson> lessons;
    private final List<String> scales;
    private final Map<String, RelativeMinor> relativeMinors;

    @Autowired
    public MainController(
            @Value("classpath:data.json") Resource dataFile,
            ObjectMapper objectMapper) {
        this(loadPracticeData(dataFile, objectMapper));
    }

    MainController(PracticeData data) {
        this(data.songs(), data.lessons(), data.scales());
    }

    MainController(List<Song> songs, List<Lesson> lessons, Map<String, RelativeMinor> relativeMinors) {
        this.songs = songs.stream()
                .filter(song -> !song.name().isBlank())
                .toList();
        if (this.songs.isEmpty()) {
            throw new IllegalStateException("The data JSON must contain at least one song");
        }
        this.lessons = lessons.stream()
                .filter(lesson -> !lesson.name().isBlank())
                .toList();
        if (this.lessons.isEmpty()) {
            throw new IllegalStateException("The data JSON must contain at least one lesson");
        }
        this.relativeMinors = Collections.unmodifiableMap(new LinkedHashMap<>(relativeMinors));
        this.scales = List.copyOf(this.relativeMinors.keySet());
        if (this.scales.isEmpty()) {
            throw new IllegalStateException("The data JSON must contain at least one scale");
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now();
        String scale = scaleFor(today);
        model.addAttribute("scaleToPractice", scaleDisplay(scale));
        model.addAttribute("minorBackingTrackUrl", minorBackingTrackUrl(scale));
        Song song = songFor(today);
        model.addAttribute("songToPractice", song.name());
        model.addAttribute("songLinks", song.links());
        Lesson lesson = lessonFor(today);
        model.addAttribute("lessonToPractice", lesson.name());
        model.addAttribute("lessonLinks", lesson.links());
        return "index";
    }

    String scaleDisplay(String scale) {
        RelativeMinor relativeMinor = relativeMinors.get(scale);
        return relativeMinor == null ? scale : scale + " (" + relativeMinor.relativeMinor() + ")";
    }

    String minorBackingTrackUrl(String scale) {
        RelativeMinor relativeMinor = relativeMinors.get(scale);
        return relativeMinor == null ? null : relativeMinor.backingTrackUrl();
    }

    private static PracticeData loadPracticeData(Resource resource, ObjectMapper objectMapper) {
        try (InputStream input = resource.getInputStream()) {
            Map<String, Object> data = objectMapper.readValue(input, new TypeReference<>() {});
            if (data.get("Songs") == null) {
                throw new IllegalStateException("The data JSON must contain at least one song");
            }
            List<Song> songs = objectMapper.convertValue(data.get("Songs"), new TypeReference<>() {});
            if (data.get("Lessons") == null) {
                throw new IllegalStateException("The data JSON must contain at least one lesson");
            }
            List<Lesson> lessons = objectMapper.convertValue(data.get("Lessons"), new TypeReference<>() {});
            Object scaleData = data.get("Scales");
            if (scaleData == null) {
                throw new IllegalStateException("The data JSON must contain at least one scale");
            }
            LinkedHashMap<String, RelativeMinor> scales = objectMapper.convertValue(
                    scaleData, new TypeReference<>() {
                    });
            if (scales.isEmpty()) {
                throw new IllegalStateException("The data JSON must contain at least one scale");
            }
            return new PracticeData(songs, lessons, scales);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load practice data from " + resource.getDescription(), e);
        }
    }

    Song songFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return songs.get(Math.floorMod(daysSinceStart, songs.size()));
    }

    Lesson lessonFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return lessons.get(Math.floorMod(daysSinceStart, lessons.size()));
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return scales.get(Math.floorMod(daysSinceStart, scales.size()));
    }
}
