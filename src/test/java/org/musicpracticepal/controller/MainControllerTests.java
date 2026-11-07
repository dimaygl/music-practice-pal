package org.musicpracticepal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.musicpracticepal.repository.SongRepository;
import org.musicpracticepal.service.SongService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.imageio.ImageIO;

class MainControllerTests {

    @TempDir
    Path uploadDirectory;

    @Test
    void songsRotateDailyInConfiguredOrder() {
        MainController controller = new MainController(
                List.of(
                        new MainController.Song("Song One", null),
                        new MainController.Song("Song Two", null)),
                List.of(
                        new MainController.Lesson("Lesson One", List.of("https://example.com/lesson-one")),
                        new MainController.Lesson("Lesson Two", List.of("https://example.com/lesson-two"))),
                mock(SongService.class));
        LocalDate rotationStart = LocalDate.of(1970, 1, 5);

        assertEquals("Song One", controller.songFor(rotationStart).name());
        assertEquals("Song Two", controller.songFor(rotationStart.plusDays(1)).name());
        assertEquals("Song One", controller.songFor(rotationStart.plusDays(2)).name());
        assertEquals("Lesson One", controller.lessonFor(rotationStart).name());
        assertEquals(List.of("https://example.com/lesson-two"),
                controller.lessonFor(rotationStart.plusDays(1)).links());
    }

    @Test
    void homeModelIncludesLinksForSelectedSong() {
        List<MainController.Song> songs = List.of(
                new MainController.Song("Song One", null),
                new MainController.Song("Song Two", null));
        List<MainController.Lesson> lessons = List.of(
                new MainController.Lesson("Lesson One", List.of("https://example.com/lesson-one")));
        SongService songService = mock(SongService.class);
        when(songService.findBySongName(anyString())).thenReturn(List.of("https://example.com/song-link"));
        MainController controller = new MainController(
                songs, lessons, songService);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.home(model);

        MainController.Song selectedSong = controller.songFor(LocalDate.now());
        String selectedScale = controller.scaleFor(LocalDate.now());
        assertEquals(selectedSong.name(), model.get("songToPractice"));
        @SuppressWarnings("unchecked")
        List<MainController.ProgressionOption> progressionOptions =
                (List<MainController.ProgressionOption>) model.get("progressionOptions");
        assertEquals(selectedScale, model.get("currentScaleKey"));
        assertEquals(List.of(progressionOptions.getFirst().chordSequence().split("\\|")),
                model.get("chordProgression"));
        assertEquals(11, progressionOptions.size());
        assertEquals(ProgressionCatalog.chordSequence(ProgressionCatalog.progression("pop"), selectedScale),
                progressionOptions.getFirst().chordSequence());
        assertEquals("Pop (I–V–vi–IV)", progressionOptions.getFirst().label());
        assertEquals(ProgressionCatalog.PROGRESSION_DATA_JSON, model.get("progressionData"));
        @SuppressWarnings("unchecked")
        List<MainController.ScaleOption> scaleOptions =
                (List<MainController.ScaleOption>) model.get("scaleOptions");
        assertEquals(24, scaleOptions.size());
        assertEquals("C (A minor)", scaleOptions.getFirst().label());
        assertEquals("F# minor (A major)", scaleOptions.get(21).label());
        assertEquals(List.of("https://example.com/song-link"), model.get("songLinks"));
        MainController.Lesson selectedLesson = controller.lessonFor(LocalDate.now());
        assertEquals(selectedLesson.name(), model.get("lessonToPractice"));
        assertEquals(selectedLesson.links(), model.get("lessonLinks"));
    }

    @Test
    void songChartsModelIncludesAllSongs() {
        List<MainController.Song> songs = List.of(
                new MainController.Song("Song One", null),
                new MainController.Song("Song Two", "/song-charts/song-two.jpg"));
        MainController controller = new MainController(
                songs,
                List.of(new MainController.Lesson("Lesson One", List.of())),
                mock(SongService.class));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("song-charts", controller.songCharts(model));
        @SuppressWarnings("unchecked")
        List<MainController.SongCard> songCards =
                (List<MainController.SongCard>) model.get("songs");
        assertEquals(songs.size(), songCards.size());
        assertEquals("Song One", songCards.get(0).name());
        assertEquals("/song-charts/song-two.jpg", songCards.get(1).chartImage());
    }

    @Test
    void uploadingChartMakesItAvailableInTheGallery() throws IOException {
        SongService songService = new SongService(
                mock(SongRepository.class), uploadDirectory.toString());
        MainController controller = new MainController(
                List.of(new MainController.Song("Song One", null)),
                List.of(new MainController.Lesson("Lesson One", List.of())),
                songService);
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
        MockMultipartFile upload = new MockMultipartFile(
                "chart", "chart.png", "image/png", imageBytes.toByteArray());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertEquals("redirect:/song-charts",
                controller.uploadSongChart(0, upload, redirectAttributes));
        assertEquals("Chart uploaded for Song One.", redirectAttributes.getFlashAttributes().get("uploadSuccess"));

        ExtendedModelMap galleryModel = new ExtendedModelMap();
        controller.songCharts(galleryModel);
        @SuppressWarnings("unchecked")
        List<MainController.SongCard> songCards =
                (List<MainController.SongCard>) galleryModel.get("songs");
        assertEquals("/uploaded-charts/chart-0.png", songCards.get(0).chartImage());
    }
}
