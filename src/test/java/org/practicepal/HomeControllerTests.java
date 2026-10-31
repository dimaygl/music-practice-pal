package org.practicepal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class HomeControllerTests {

    @Test
    void homeSelectsAScaleFromConfiguredScales() {
        HomeController controller = new HomeController(List.of("A", "B", "C"));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.home(model);

        assertEquals("index", view);
        assertTrue(List.of("A", "B", "C").contains(model.get("scaleToPractice")));
    }

    @Test
    void homeTrimsAndIgnoresEmptyConfiguredScales() {
        HomeController controller = new HomeController(List.of(" A ", "", " B "));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.home(model);

        assertTrue(List.of("A", "B").contains(model.get("scaleToPractice")));
    }

    @Test
    void scalesAdvanceInOrderAcrossTwoWeeks() {
        List<String> scales = List.of("A", "A#", "B", "B#", "C", "C#", "D",
                "D#", "E", "E#", "F", "F#", "G", "G#");
        HomeController controller = new HomeController(scales);
        LocalDate firstMonday = LocalDate.of(1970, 1, 5);

        for (int day = 0; day < 14; day++) {
            assertEquals(scales.get(day), controller.scaleFor(firstMonday.plusDays(day)));
        }
    }
}
