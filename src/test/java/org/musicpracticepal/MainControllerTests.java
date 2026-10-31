package org.musicpracticepal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MainControllerTests {

    @Test
    void songsRotateDailyInConfiguredOrder() {
        Map<String, MainController.RelativeMinor> scales = new LinkedHashMap<>();
        scales.put("A", new MainController.RelativeMinor("F# minor", "track-a"));

        MainController controller = new MainController(
                List.of("Song One", "Song Two"), scales);
        LocalDate rotationStart = LocalDate.of(1970, 1, 5);

        assertEquals("Song One", controller.songFor(rotationStart));
        assertEquals("Song Two", controller.songFor(rotationStart.plusDays(1)));
        assertEquals("Song One", controller.songFor(rotationStart.plusDays(2)));
    }
}
