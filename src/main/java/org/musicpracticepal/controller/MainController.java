package org.musicpracticepal.controller;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.musicpracticepal.repository.LessonRepository;
import org.musicpracticepal.repository.SongRepository;
import org.musicpracticepal.service.SongCatalogService;
import org.musicpracticepal.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MainController {

    record Song(Long id, String name) {
        Song(String name) {
            this(null, name);
        }

        Song {
            name = name.trim();
        }
    }

    record Lesson(Long id, String name, String linkUrl) {
        Lesson(String name, String linkUrl) {
            this(null, name, linkUrl);
        }

        Lesson {
            name = name.trim();
        }
    }

    record SongCard(int index, Long id, String name, String chartImage) {}

    record ProgressionOption(String id, String label, String chordSequence) {}

    record ScaleOption(String key, String label) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);
    private static final List<ScaleOption> SCALE_OPTIONS = ProgressionCatalog.KEYS.stream()
            .map(key -> new ScaleOption(key, ProgressionCatalog.displayName(key)))
            .toList();

    private final SongRepository songRepository;
    private final LessonRepository lessonRepository;
    private final SongService songService;
    private final SongCatalogService songCatalogService;

    @Autowired
    public MainController(
            SongRepository songRepository,
            LessonRepository lessonRepository,
            SongService songService,
            SongCatalogService songCatalogService) {
        this.songRepository = songRepository;
        this.lessonRepository = lessonRepository;
        this.configuredSongs = null;
        this.configuredLessons = null;
        this.songService = songService;
        this.songCatalogService = songCatalogService;
    }

    MainController(
            List<Song> songs,
            List<Lesson> lessons,
            SongService songService) {
        this.configuredSongs = songs.stream()
                .filter(song -> !song.name().isBlank())
                .toList();
        this.configuredLessons = lessons.stream()
                .filter(lesson -> !lesson.name().isBlank())
                .toList();
        this.songRepository = null;
        this.lessonRepository = null;
        this.songService = songService;
        this.songCatalogService = null;
    }

    private final List<Song> configuredSongs;
    private final List<Lesson> configuredLessons;

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
        Song song = songs().get(songIndex);
        model.addAttribute("songToPractice", song.name());
        model.addAttribute("songIndex", songIndex);
        model.addAttribute("songLinks", songService.findBySongName(song.name()));
        Lesson lesson = lessonFor(today);
        model.addAttribute("lessonToPractice", lesson.name());
        model.addAttribute("lessonId", lesson.id());
        model.addAttribute("lessonLink", lesson.linkUrl());
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
        if (index < 0 || index >= songs().size()) {
            redirectAttributes.addFlashAttribute("linkError", "That song could not be found.");
            return "redirect:/";
        }

        String normalizedUrl = url.trim();
        if (isValidHttpUrl(normalizedUrl)) {
            redirectAttributes.addFlashAttribute(
                    "linkError", "Enter a valid HTTP or HTTPS link.");
            return "redirect:/";
        }

        Song song = songs().get(index);
        if (songService.addLink(song.name(), normalizedUrl)) {
            redirectAttributes.addFlashAttribute("linkSuccess", "Link added for " + song.name() + ".");
        } else {
            redirectAttributes.addFlashAttribute("linkError", "That link has already been added.");
        }
        return "redirect:/";
    }

    @PostMapping("/lessons/{id}/link")
    public String updateLessonLink(
            @PathVariable Long id,
            @RequestParam("url") String url,
            RedirectAttributes redirectAttributes) {
        var lesson = lessonRepository.findById(id).orElse(null);
        if (lesson == null) {
            redirectAttributes.addFlashAttribute("lessonLinkError", "That lesson could not be found.");
            return "redirect:/";
        }

        String normalizedUrl = url.trim();
        if (!normalizedUrl.isEmpty() && isValidHttpUrl(normalizedUrl)) {
            redirectAttributes.addFlashAttribute(
                    "lessonLinkError", "Enter a valid HTTP or HTTPS link.");
            return "redirect:/";
        }

        lesson.setLinkUrl(normalizedUrl.isEmpty() ? null : normalizedUrl);
        lessonRepository.save(lesson);
        redirectAttributes.addFlashAttribute("lessonLinkSuccess", "Lesson link updated.");
        return "redirect:/";
    }

    @GetMapping("/song-charts")
    public String songCharts(Model model) {
        List<Song> songs = songs();
        List<SongCard> songCards = IntStream.range(0, songs.size())
                .mapToObj(index -> {
                    Song song = songs.get(index);
                    return new SongCard(
                            index, song.id(), song.name(), songService.chartImageUrl(index));
                })
                .toList();
        model.addAttribute("songs", songCards);
        model.addAttribute("songCount", songs.size());
        return "song-charts";
    }

    @PostMapping("/songs")
    public String addSong(
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        String normalizedName = name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > 255) {
            redirectAttributes.addFlashAttribute(
                    "songError", "Enter a song name of no more than 255 characters.");
        } else if (songCatalogService.addSong(normalizedName)) {
            redirectAttributes.addFlashAttribute("songSuccess", "Song added.");
        } else {
            redirectAttributes.addFlashAttribute("songError", "That song is already in the list.");
        }
        return "redirect:/song-charts";
    }

    @PostMapping("/songs/{id}/remove")
    public String removeSong(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        if (songs().stream().noneMatch(song -> id.equals(song.id()))) {
            redirectAttributes.addFlashAttribute("songError", "That song could not be found.");
            return "redirect:/song-charts";
        }
        try {
            songCatalogService.removeSong(id);
            redirectAttributes.addFlashAttribute("songSuccess", "Song removed.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("songError", "That song could not be found.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("songError", "The last song cannot be removed.");
        }
        return "redirect:/song-charts";
    }

    @PostMapping("/song-charts/{index}/upload")
    public String uploadSongChart(
            @PathVariable int index,
            @RequestParam("chart") MultipartFile upload,
            RedirectAttributes redirectAttributes) {
        if (index < 0 || index >= songs().size()) {
            redirectAttributes.addFlashAttribute("uploadError", "That song could not be found.");
            return "redirect:/song-charts";
        }
        try {
            songService.storeChart(songs().get(index).id(), upload);
            redirectAttributes.addFlashAttribute(
                    "uploadSuccess", "Chart uploaded for " + songs().get(index).name() + ".");
        } catch (SongService.InvalidChartImageException e) {
            redirectAttributes.addFlashAttribute("uploadError", e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save song chart upload", e);
        }
        return "redirect:/song-charts";
    }

    @GetMapping("/songs/{id}/chart-image")
    public ResponseEntity<byte[]> songChartImage(@PathVariable Long id) {
        try {
            SongService.ChartImage image = songService.chartImage(id);
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .body(image.data());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found", e);
        }
    }

    private List<Song> songs() {
        if (configuredSongs != null) {
            return configuredSongs;
        }
        List<Song> songs = songRepository.findAllByOrderByPositionAsc().stream()
                .map(song -> new Song(song.getId(), song.getName()))
                .toList();
        if (songs.isEmpty()) {
            throw new IllegalStateException("No songs are configured in the database");
        }
        return songs;
    }

    private List<Lesson> lessons() {
        if (configuredLessons != null) {
            return configuredLessons;
        }
        List<Lesson> lessons = lessonRepository.findAllByOrderByPositionAsc().stream()
                .map(lesson -> new Lesson(lesson.getId(), lesson.getName(), lesson.getLinkUrl()))
                .toList();
        if (lessons.isEmpty()) {
            throw new IllegalStateException("No lessons are configured in the database");
        }
        return lessons;
    }

    Song songFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        List<Song> songs = songs();
        return songs.get(Math.floorMod(daysSinceStart, songs.size()));
    }

    Lesson lessonFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        List<Lesson> lessons = lessons();
        return lessons.get(Math.floorMod(daysSinceStart, lessons.size()));
    }

    int songIndexFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return Math.floorMod(daysSinceStart, songs().size());
    }

    private static boolean isValidHttpUrl(String url) {
        if (url.isBlank() || url.length() > 2048) {
            return true;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return !uri.isAbsolute()
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return ProgressionCatalog.MAJOR_KEYS.get(
                Math.floorMod(daysSinceStart, ProgressionCatalog.MAJOR_KEYS.size()));
    }
}
