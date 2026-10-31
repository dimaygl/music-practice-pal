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

    private record PracticeData(List<String> songs, LinkedHashMap<String, RelativeMinor> scales) {}

    private static final LocalDate ROTATION_START = LocalDate.of(1970, 1, 5);

    private final List<String> songs;
    private final List<String> scales;
    private final Map<String, RelativeMinor> relativeMinors;

    @Autowired
    public MainController(
            @Value("classpath:data.json") Resource dataFile,
            ObjectMapper objectMapper) {
        this(loadPracticeData(dataFile, objectMapper));
    }

    MainController(PracticeData data) {
        this(data.songs(), data.scales());
    }

    MainController(List<String> songs, Map<String, RelativeMinor> relativeMinors) {
        this.songs = songs.stream()
                .map(String::trim)
                .filter(song -> !song.isEmpty())
                .toList();
        if (this.songs.isEmpty()) {
            throw new IllegalStateException("The data JSON must contain at least one song");
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
        model.addAttribute("songToPractice", songFor(today));
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
            List<String> songs = objectMapper.convertValue(
                    data.get("Songs"), new TypeReference<List<String>>() {});
            Object scaleData = data.get("Scales");
            if (scaleData == null) {
                throw new IllegalStateException("The data JSON must contain at least one scale");
            }
            LinkedHashMap<String, RelativeMinor> scales = objectMapper.convertValue(
                    scaleData, new TypeReference<LinkedHashMap<String, RelativeMinor>>() {});
            if (scales.isEmpty()) {
                throw new IllegalStateException("The data JSON must contain at least one scale");
            }
            return new PracticeData(songs, scales);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load practice data from " + resource.getDescription(), e);
        }
    }

    String songFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return songs.get((int) Math.floorMod(daysSinceStart, songs.size()));
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - ROTATION_START.toEpochDay();
        return scales.get((int) Math.floorMod(daysSinceStart, scales.size()));
    }
}
