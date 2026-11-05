package org.musicpracticepal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Controller
public class MainController {

    record RelativeMinor(String relativeMinor, String backingTrackUrl) {}

    record Song(String name, List<String> links, String chartImage) {
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

    record SongCard(int index, String name, String chartImage) {}

    private record PracticeData(
            List<Song> songs, List<Lesson> lessons, LinkedHashMap<String, RelativeMinor> scales) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);

    private final List<Song> songs;
    private final List<Lesson> lessons;
    private final List<String> scales;
    private final Map<String, RelativeMinor> relativeMinors;
    private final SongChartStorage chartStorage;

    @Autowired
    public MainController(
            @Value("classpath:data.json") Resource dataFile,
            ObjectMapper objectMapper,
            SongChartStorage chartStorage) {
        this(loadPracticeData(dataFile, objectMapper), chartStorage);
    }

    MainController(PracticeData data) {
        this(data.songs(), data.lessons(), data.scales(),
                new SongChartStorage(Path.of("uploads/song-charts")));
    }

    private MainController(PracticeData data, SongChartStorage chartStorage) {
        this(data.songs(), data.lessons(), data.scales(), chartStorage);
    }

    MainController(List<Song> songs, List<Lesson> lessons, Map<String, RelativeMinor> relativeMinors) {
        this(songs, lessons, relativeMinors, new SongChartStorage(Path.of("uploads/song-charts")));
    }

    MainController(
            List<Song> songs,
            List<Lesson> lessons,
            Map<String, RelativeMinor> relativeMinors,
            SongChartStorage chartStorage) {
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
        this.chartStorage = chartStorage;
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

    @GetMapping("/song-charts")
    public String songCharts(Model model) {
        List<SongCard> songCards = IntStream.range(0, songs.size())
                .mapToObj(index -> {
                    Song song = songs.get(index);
                    String uploadedImage = chartStorage.imageUrl(index);
                    String chartImage = uploadedImage == null ? song.chartImage() : uploadedImage;
                    return new SongCard(index, song.name(), chartImage);
                })
                .toList();
        model.addAttribute("songs", songCards);
        return "song-charts";
    }

    @PostMapping("/song-charts/{index}/upload")
    public String uploadSongChart(
            @PathVariable int index,
            @RequestParam("chart") MultipartFile upload,
            RedirectAttributes redirectAttributes) {
        if (index < 0 || index >= songs.size()) {
            redirectAttributes.addFlashAttribute("uploadError", "That song could not be found.");
            return "redirect:/song-charts";
        }
        try {
            chartStorage.store(index, upload);
            redirectAttributes.addFlashAttribute(
                    "uploadSuccess", "Chart uploaded for " + songs.get(index).name() + ".");
        } catch (SongChartStorage.InvalidChartImageException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save song chart upload", e);
        }
        return "redirect:/song-charts";
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
