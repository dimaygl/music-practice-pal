package org.practicepal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class HomeControllerTests {

    private static final Map<String, HomeController.RelativeMinor> RELATIVE_MINORS = createRelativeMinors();

    private static Map<String, HomeController.RelativeMinor> createRelativeMinors() {
        List<Map.Entry<String, HomeController.RelativeMinor>> entries = List.of(
                Map.entry("A", new HomeController.RelativeMinor("F# minor", "https://www.guitarscale.org/audio/jam_fsharp_minor_normal.mp3")),
                Map.entry("A#", new HomeController.RelativeMinor("G minor", "https://www.guitarscale.org/audio/jam_pop_gminorkey_normal.mp3")),
                Map.entry("B", new HomeController.RelativeMinor("G# minor", "https://www.guitarscale.org/audio/jam_gsharp_minor_normal.mp3")),
                Map.entry("B#", new HomeController.RelativeMinor("A minor", "https://www.guitarscale.org/audio/jam_pop_aminorkey_fast.mp3")),
                Map.entry("C", new HomeController.RelativeMinor("A minor", "https://www.guitarscale.org/audio/jam_pop_aminorkey_fast.mp3")),
                Map.entry("C#", new HomeController.RelativeMinor("A# minor", "https://www.guitarscale.org/audio/jam_pop_asharpminorkey_normal.mp3")),
                Map.entry("D", new HomeController.RelativeMinor("B minor", "https://www.guitarscale.org/audio/jam_b_minor_normal.mp3")),
                Map.entry("D#", new HomeController.RelativeMinor("C minor", "https://www.guitarscale.org/audio/jam_pop_cminorkey_normal.mp3")),
                Map.entry("E", new HomeController.RelativeMinor("C# minor", "https://www.guitarscale.org/audio/jam_pop_csharpminorkey_normal.mp3")),
                Map.entry("E#", new HomeController.RelativeMinor("D minor", "https://www.guitarscale.org/audio/jam_pop_dminorkey_normal.mp3")),
                Map.entry("F", new HomeController.RelativeMinor("D minor", "https://www.guitarscale.org/audio/jam_pop_dminorkey_normal.mp3")),
                Map.entry("F#", new HomeController.RelativeMinor("D# minor", "https://www.guitarscale.org/audio/jam_pop_dsharpminorkey_normal.mp3")),
                Map.entry("G", new HomeController.RelativeMinor("E minor", "https://www.guitarscale.org/audio/jam_pop_eminorkey_normal.mp3")),
                Map.entry("G#", new HomeController.RelativeMinor("F minor", "https://www.guitarscale.org/audio/jam_pop_fminorkey_normal.mp3")));
        Map<String, HomeController.RelativeMinor> mappings = new LinkedHashMap<>();
        entries.forEach(entry -> mappings.put(entry.getKey(), entry.getValue()));
        return mappings;
    }

    private HomeController controller() {
        return new HomeController(RELATIVE_MINORS);
    }

    @Test
    void homeSelectsAScaleFromConfiguredScales() {
        HomeController controller = controller();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.home(model);

        assertEquals("index", view);
        assertTrue(RELATIVE_MINORS.keySet().stream()
                .map(controller::scaleDisplay)
                .anyMatch(model.get("scaleToPractice")::equals));
    }

    @Test
    void scalesAdvanceInOrderAcrossTwoWeeks() {
        List<String> scales = new ArrayList<>(RELATIVE_MINORS.keySet());
        HomeController controller = controller();
        LocalDate firstMonday = LocalDate.of(1970, 1, 5);

        for (int day = 0; day < 14; day++) {
            assertEquals(scales.get(day), controller.scaleFor(firstMonday.plusDays(day)));
        }
    }

    @Test
    void scaleDisplayIncludesItsRelativeMinor() {
        HomeController controller = controller();

        assertEquals("A (F# minor)", controller.scaleDisplay("A"));
        assertEquals("C# (A# minor)", controller.scaleDisplay("C#"));
        assertEquals("B# (A minor)", controller.scaleDisplay("B#"));
    }

    @Test
    void scaleDisplayLeavesUnmappedScalesUnchanged() {
        HomeController controller = controller();

        assertEquals("Dorian", controller.scaleDisplay("Dorian"));
    }

    @Test
    void minorBackingTrackUrlUsesTheRelativeMinor() {
        HomeController controller = controller();

        assertEquals("https://www.guitarscale.org/audio/jam_pop_csharpminorkey_normal.mp3",
                controller.minorBackingTrackUrl("E"));
        assertEquals("https://www.guitarscale.org/audio/jam_fsharp_minor_normal.mp3",
                controller.minorBackingTrackUrl("A"));
        assertEquals("https://www.guitarscale.org/audio/jam_pop_dsharpminorkey_normal.mp3",
                controller.minorBackingTrackUrl("F#"));
    }

    @Test
    void unmappedScalesHaveNoMinorBackingTrackUrl() {
        HomeController controller = controller();

        assertNull(controller.minorBackingTrackUrl("Dorian"));
    }
}
