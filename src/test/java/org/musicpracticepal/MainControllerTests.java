package org.musicpracticepal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class MainControllerTests {

    @Test
    void songsRotateDailyInConfiguredOrder() {
        Map<String, MainController.RelativeMinor> scales = new LinkedHashMap<>();
        scales.put("A", new MainController.RelativeMinor("F# minor", "track-a"));

        MainController controller = new MainController(
                List.of(
                        new MainController.Song("Song One", List.of("https://example.com/song-one")),
                        new MainController.Song("Song Two", List.of("https://example.com/song-two"))),
                List.of(
                        new MainController.Lesson("Lesson One", List.of("https://example.com/lesson-one")),
                        new MainController.Lesson("Lesson Two", List.of("https://example.com/lesson-two"))),
                scales);
        LocalDate rotationStart = LocalDate.of(1970, 1, 5);

        assertEquals("Song One", controller.songFor(rotationStart).name());
        assertEquals(List.of("https://example.com/song-two"),
                controller.songFor(rotationStart.plusDays(1)).links());
        assertEquals("Song One", controller.songFor(rotationStart.plusDays(2)).name());
        assertEquals("Lesson One", controller.lessonFor(rotationStart).name());
        assertEquals(List.of("https://example.com/lesson-two"),
                controller.lessonFor(rotationStart.plusDays(1)).links());
    }

    @Test
    void homeModelIncludesLinksForSelectedSong() {
        Map<String, MainController.RelativeMinor> scales = new LinkedHashMap<>();
        scales.put("A", new MainController.RelativeMinor("F# minor", "track-a"));
        List<MainController.Song> songs = List.of(
                new MainController.Song("Song One", List.of("https://example.com/song-one")),
                new MainController.Song("Song Two", List.of("https://example.com/song-two")));
        List<MainController.Lesson> lessons = List.of(
                new MainController.Lesson("Lesson One", List.of("https://example.com/lesson-one")));
        MainController controller = new MainController(songs, lessons, scales);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.home(model);

        MainController.Song selectedSong = controller.songFor(LocalDate.now());
        assertEquals(selectedSong.name(), model.get("songToPractice"));
        assertEquals(selectedSong.links(), model.get("songLinks"));
        MainController.Lesson selectedLesson = controller.lessonFor(LocalDate.now());
        assertEquals(selectedLesson.name(), model.get("lessonToPractice"));
        assertEquals(selectedLesson.links(), model.get("lessonLinks"));
    }
}
