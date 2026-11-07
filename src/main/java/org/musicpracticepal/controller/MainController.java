package org.musicpracticepal.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.musicpracticepal.service.SongService;
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

    record Song(String name, String chartImage) {
        Song {
            name = name.trim();
        }
    }

    record Lesson(String name, List<String> links) {
        Lesson {
            name = name.trim();
            links = List.copyOf(links);
        }
    }

    record SongCard(int index, String name, String chartImage) {}

    record ProgressionOption(String id, String label, String chordSequence) {}

    record ScaleOption(String key, String label) {}

    private record PracticeData(List<Song> songs, List<Lesson> lessons) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);
    private static final List<ScaleOption> SCALE_OPTIONS = ProgressionCatalog.KEYS.stream()
            .map(key -> new ScaleOption(key, ProgressionCatalog.displayName(key)))
            .toList();

    private final List<Song> songs;
    private final List<Lesson> lessons;
    private final SongService songService;

    @Autowired
    public MainController(
            @Value("classpath:data.json") Resource dataFile,
            ObjectMapper objectMapper,
            SongService songService) {
        this(loadPracticeData(dataFile, objectMapper), songService);
    }

    private MainController(PracticeData data, SongService songService) {
        this(data.songs(), data.lessons(), songService);
    }

    MainController(
            List<Song> songs,
            List<Lesson> lessons,
            SongService songService) {
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
        this.songService = songService;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now();
        String scale = scaleFor(today);
        model.addAttribute("currentScaleKey", scale);
        model.addAttribute("scaleToPractice", ProgressionCatalog.displayName(scale));
        model.addAttribute("scaleOptions", SCALE_OPTIONS);
        List<ProgressionOption> progressionOptions = progressionOptionsFor(scale);
        model.addAttribute("progressionOptions", progressionOptions);
        model.addAttribute("progressionData", ProgressionCatalog.PROGRESSION_DATA_JSON);
        model.addAttribute("chordProgression",
                List.of(progressionOptions.getFirst().chordSequence().split("\\|")));
        int songIndex = songIndexFor(today);
        Song song = songs.get(songIndex);
        model.addAttribute("songToPractice", song.name());
        model.addAttribute("songIndex", songIndex);
        model.addAttribute("songLinks", songService.findBySongName(song.name()));
        Lesson lesson = lessonFor(today);
        model.addAttribute("lessonToPractice", lesson.name());
        model.addAttribute("lessonLinks", lesson.links());
        return "index";
    }

    private static List<ProgressionOption> progressionOptionsFor(String scale) {
        return ProgressionCatalog.PROGRESSIONS.stream()
                .map(progression -> new ProgressionOption(
                        progression.id(),
                        progression.label(scale),
                        ProgressionCatalog.chordSequence(progression, scale)))
                .toList();
    }

    @PostMapping("/songs/{index}/links")
    public String addSongLink(
            @PathVariable int index,
            @RequestParam("url") String url,
            RedirectAttributes redirectAttributes) {
        if (index < 0 || index >= songs.size()) {
            redirectAttributes.addFlashAttribute("linkError", "That song could not be found.");
            return "redirect:/";
        }

        String normalizedUrl = url.trim();
        if (!isValidSongLink(normalizedUrl)) {
            redirectAttributes.addFlashAttribute(
                    "linkError", "Enter a valid HTTP or HTTPS link.");
            return "redirect:/";
        }

        Song song = songs.get(index);
        if (songService.addLink(song.name(), normalizedUrl)) {
            redirectAttributes.addFlashAttribute("linkSuccess", "Link added for " + song.name() + ".");
        } else {
            redirectAttributes.addFlashAttribute("linkError", "That link has already been added.");
        }
        return "redirect:/";
    }

    @GetMapping("/song-charts")
    public String songCharts(Model model) {
        List<SongCard> songCards = IntStream.range(0, songs.size())
                .mapToObj(index -> {
                    Song song = songs.get(index);
                    String uploadedImage = songService.chartImageUrl(index);
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
            songService.storeChart(index, upload);
            redirectAttributes.addFlashAttribute(
                    "uploadSuccess", "Chart uploaded for " + songs.get(index).name() + ".");
        } catch (SongService.InvalidChartImageException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save song chart upload", e);
        }
        return "redirect:/song-charts";
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
            return new PracticeData(songs, lessons);
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

    int songIndexFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return Math.floorMod(daysSinceStart, songs.size());
    }

    private static boolean isValidSongLink(String url) {
        if (url.isBlank() || url.length() > 2048) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return uri.isAbsolute()
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return ProgressionCatalog.MAJOR_KEYS.get(
                Math.floorMod(daysSinceStart, ProgressionCatalog.MAJOR_KEYS.size()));
    }
}
