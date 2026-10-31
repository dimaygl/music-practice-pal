package org.practicepal;

import java.io.IOException;
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
public class HomeController {

    record RelativeMinor(String relativeMinor, String backingTrackUrl) {}

    private final List<String> scales;
    private final Map<String, RelativeMinor> relativeMinors;

    @Autowired
    public HomeController(
            @Value("classpath:data.json") Resource dataFile,
            ObjectMapper objectMapper) {
        this(loadRelativeMinors(dataFile, objectMapper));
    }

    HomeController(Map<String, RelativeMinor> relativeMinors) {
        this.relativeMinors = Collections.unmodifiableMap(new LinkedHashMap<>(relativeMinors));
        this.scales = List.copyOf(this.relativeMinors.keySet());
        if (this.scales.isEmpty()) {
            throw new IllegalStateException("The data JSON must contain at least one scale");
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        String scale = scaleFor(LocalDate.now());
        model.addAttribute("scaleToPractice", scaleDisplay(scale));
        model.addAttribute("minorBackingTrackUrl", minorBackingTrackUrl(scale));
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

    private static Map<String, RelativeMinor> loadRelativeMinors(Resource resource, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(
                    resource.getInputStream(), new TypeReference<LinkedHashMap<String, RelativeMinor>>() {});
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load scale data from " + resource.getDescription(), e);
        }
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - LocalDate.of(1970, 1, 5).toEpochDay();
        return scales.get((int) Math.floorMod(daysSinceStart, scales.size()));
    }
}
