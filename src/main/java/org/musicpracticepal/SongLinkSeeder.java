package org.musicpracticepal;

import org.musicpracticepal.service.SongLinkService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SongLinkSeeder {

    @Bean
    ApplicationRunner seedDefaultSongLinks(SongLinkService songLinkService) {
        return args -> {
            songLinkService.addLink(
                    "Ain't No Sunshine",
                    "https://www.ultimate-guitar.com/search.php?search_type=title&value=Ain%27t+No+Sunshine");
            songLinkService.addLink(
                    "Wind Cries Mary",
                    "https://www.ultimate-guitar.com/search.php?search_type=title&value=Wind+Cries+Mary");
        };
    }
}
