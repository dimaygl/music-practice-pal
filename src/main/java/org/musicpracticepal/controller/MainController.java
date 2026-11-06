package org.musicpracticepal.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    record ProgressionOption(String id, String label, String chordSequence, String chordSequencesByScale) {}

    record ScaleOption(String key, String label) {}

    private record ChordDegree(
            int semitones, boolean minor, boolean dominantSeventh, boolean diminished) {
        ChordDegree(int semitones, boolean minor) {
            this(semitones, minor, false, false);
        }

        ChordDegree(int semitones, boolean minor, boolean dominantSeventh) {
            this(semitones, minor, dominantSeventh, false);
        }
    }

    private record ProgressionDefinition(String id, String label, List<ChordDegree> degrees) {}

    private record PracticeData(List<Song> songs, List<Lesson> lessons) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);
    private static final List<String> MAJOR_SCALES = List.of(
            "A", "A#", "B", "B#", "C", "C#", "D", "D#", "E", "E#", "F", "F#", "G", "G#");
    private static final List<String> MINOR_SCALES = List.of(
            "F# minor", "G minor", "G# minor", "A minor", "A# minor", "B minor",
            "C minor", "C# minor", "D minor", "D# minor", "E minor", "F minor");
    private static final List<String> SCALES = java.util.stream.Stream
            .concat(MAJOR_SCALES.stream(), MINOR_SCALES.stream())
            .toList();
    private static final List<ProgressionDefinition> PROGRESSION_DEFINITIONS = List.of(
            new ProgressionDefinition("pop", "Pop (I–V–vi–IV)", List.of(
                    new ChordDegree(0, false), new ChordDegree(7, false),
                    new ChordDegree(9, true), new ChordDegree(5, false))),
            new ProgressionDefinition("doo-wop", "50s / Doo-wop (I–vi–IV–V)", List.of(
                    new ChordDegree(0, false), new ChordDegree(9, true),
                    new ChordDegree(5, false), new ChordDegree(7, false))),
            new ProgressionDefinition("minor-pop", "Pop / Rock (vi–IV–I–V)", List.of(
                    new ChordDegree(9, true), new ChordDegree(5, false),
                    new ChordDegree(0, false), new ChordDegree(7, false))),
            new ProgressionDefinition("folk-rock", "Folk / Rock (I–IV–V–I)", List.of(
                    new ChordDegree(0, false), new ChordDegree(5, false),
                    new ChordDegree(7, false), new ChordDegree(0, false))),
            new ProgressionDefinition("classic-rock", "Classic Rock (I–V–IV–I)", List.of(
                    new ChordDegree(0, false), new ChordDegree(7, false),
                    new ChordDegree(5, false), new ChordDegree(0, false))),
            new ProgressionDefinition("jazz-turnaround", "Jazz turnaround (ii–V–I–vi)", List.of(
                    new ChordDegree(2, true), new ChordDegree(7, false),
                    new ChordDegree(0, false), new ChordDegree(9, true))),
            new ProgressionDefinition("blues", "12-bar Blues (I7–IV7–V7)", List.of(
                    new ChordDegree(0, false, true), new ChordDegree(0, false, true),
                    new ChordDegree(0, false, true), new ChordDegree(0, false, true),
                    new ChordDegree(5, false, true), new ChordDegree(5, false, true),
                    new ChordDegree(0, false, true), new ChordDegree(0, false, true),
                    new ChordDegree(7, false, true), new ChordDegree(5, false, true),
                    new ChordDegree(0, false, true), new ChordDegree(0, false, true))));

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
        model.addAttribute("scaleToPractice", scaleDisplay(scale));
        model.addAttribute("scaleOptions", scaleOptions());
        List<ProgressionOption> progressionOptions = progressionOptionsFor(scale);
        model.addAttribute("progressionOptions", progressionOptions);
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

    private List<ProgressionOption> progressionOptionsFor(String scale) {
        return PROGRESSION_DEFINITIONS.stream()
                .map(definition -> new ProgressionOption(
                        definition.id(),
                        definition.label(),
                        chordSequenceFor(definition, scale),
                        SCALES.stream()
                                .map(key -> chordSequenceFor(definition, key))
                                .collect(Collectors.joining(";"))))
                .toList();
    }

    private String chordSequenceFor(ProgressionDefinition definition, String scale) {
        boolean minorScale = scale.endsWith(" minor");
        String rootName = minorScale ? scale.substring(0, scale.length() - " minor".length()) : scale;
        int root = switch (rootName) {
            case "A" -> 9;
            case "A#" -> 10;
            case "Bb" -> 10;
            case "B" -> 11;
            case "B#", "C" -> 0;
            case "C#" -> 1;
            case "Db" -> 1;
            case "D" -> 2;
            case "D#" -> 3;
            case "Eb" -> 3;
            case "E" -> 4;
            case "E#" -> 5;
            case "F" -> 5;
            case "F#" -> 6;
            case "Gb" -> 6;
            case "G" -> 7;
            case "G#" -> 8;
            case "Ab" -> 8;
            default -> throw new IllegalArgumentException("Unsupported scale: " + scale);
        };
        String[] noteNames = {"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};
        List<ChordDegree> degrees = minorScale ? minorDegreesFor(definition) : definition.degrees();
        return degrees.stream()
                .map(degree -> noteNames[(root + degree.semitones()) % noteNames.length]
                        + (degree.diminished() ? "dim" : degree.minor() && degree.dominantSeventh() ? "m7"
                        : degree.minor() ? "m" : degree.dominantSeventh() ? "7" : ""))
                .collect(Collectors.joining("|"));
    }

    private List<ChordDegree> minorDegreesFor(ProgressionDefinition definition) {
        return switch (definition.id()) {
            case "pop", "minor-pop" -> List.of(
                    new ChordDegree(0, true), new ChordDegree(8, false),
                    new ChordDegree(3, false), new ChordDegree(10, false));
            case "doo-wop" -> List.of(
                    new ChordDegree(0, true), new ChordDegree(8, false),
                    new ChordDegree(5, true), new ChordDegree(7, false));
            case "folk-rock" -> List.of(
                    new ChordDegree(0, true), new ChordDegree(5, true),
                    new ChordDegree(10, false), new ChordDegree(0, true));
            case "classic-rock" -> List.of(
                    new ChordDegree(0, true), new ChordDegree(10, false),
                    new ChordDegree(8, false), new ChordDegree(7, false));
            case "jazz-turnaround" -> List.of(
                    new ChordDegree(2, true, false, true), new ChordDegree(7, false, true),
                    new ChordDegree(0, true), new ChordDegree(8, false));
            case "blues" -> List.of(
                    new ChordDegree(0, true, true), new ChordDegree(0, true, true),
                    new ChordDegree(0, true, true), new ChordDegree(0, true, true),
                    new ChordDegree(5, true, true), new ChordDegree(5, true, true),
                    new ChordDegree(0, true, true), new ChordDegree(0, true, true),
                    new ChordDegree(7, false, true), new ChordDegree(5, true, true),
                    new ChordDegree(0, true, true), new ChordDegree(0, true, true));
            default -> throw new IllegalArgumentException("Unsupported progression: " + definition.id());
        };
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

    String scaleDisplay(String scale) {
        if (MINOR_SCALES.contains(scale)) {
            return scale + " (" + relativeMajorFor(scale) + " major)";
        }
        String relativeMinor = relativeMinorFor(scale);
        return relativeMinor == null ? scale : scale + " (" + relativeMinor + ")";
    }

    private List<ScaleOption> scaleOptions() {
        return SCALES.stream()
                .map(scale -> new ScaleOption(scale, scaleDisplay(scale)))
                .toList();
    }

    private static String relativeMinorFor(String major) {
        return switch (major) {
            case "A" -> "F# minor";
            case "A#" -> "G minor";
            case "B" -> "G# minor";
            case "B#", "C" -> "A minor";
            case "C#" -> "A# minor";
            case "D" -> "B minor";
            case "D#" -> "C minor";
            case "E" -> "C# minor";
            case "E#", "F" -> "D minor";
            case "F#" -> "D# minor";
            case "G" -> "E minor";
            case "G#" -> "F minor";
            default -> null;
        };
    }

    private static String relativeMajorFor(String minor) {
        return switch (minor) {
            case "F# minor" -> "A";
            case "G minor" -> "A#";
            case "G# minor" -> "B";
            case "A minor" -> "C";
            case "A# minor" -> "C#";
            case "B minor" -> "D";
            case "C minor" -> "D#";
            case "C# minor" -> "E";
            case "D minor" -> "F";
            case "D# minor" -> "F#";
            case "E minor" -> "G";
            case "F minor" -> "G#";
            default -> throw new IllegalArgumentException("Unsupported minor scale: " + minor);
        };
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
        return MAJOR_SCALES.get(Math.floorMod(daysSinceStart, MAJOR_SCALES.size()));
    }
}
