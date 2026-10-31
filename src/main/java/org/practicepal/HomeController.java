package org.practicepal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final List<String> scales;

    public HomeController(@Value("${scales}") List<String> scales) {
        this.scales = scales.stream()
                .map(String::trim)
                .filter(scale -> !scale.isEmpty())
                .toList();
        if (this.scales.isEmpty()) {
            throw new IllegalStateException("The scales property must contain at least one scale");
        }
    }

    @GetMapping("/")
    public String home(Model model) {
        String scale = scaleFor(LocalDate.now());
        model.addAttribute("scaleToPractice", scale);
        return "index";
    }

    String scaleFor(LocalDate date) {
        long daysSinceStart = date.toEpochDay() - LocalDate.of(1970, 1, 5).toEpochDay();
        return scales.get((int) Math.floorMod(daysSinceStart, scales.size()));
    }
}
