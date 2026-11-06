package org.musicpracticepal.config;

import org.musicpracticepal.service.SongService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ChartUploadConfig implements WebMvcConfigurer {

    private final SongService songService;

    ChartUploadConfig(SongService songService) {
        this.songService = songService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = songService.chartDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploaded-charts/**")
                .addResourceLocations(location);
    }
}
