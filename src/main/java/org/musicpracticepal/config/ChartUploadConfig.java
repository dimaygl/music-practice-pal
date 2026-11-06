package org.musicpracticepal.config;

import org.musicpracticepal.service.SongLinkService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ChartUploadConfig implements WebMvcConfigurer {

    private final SongLinkService songLinkService;

    ChartUploadConfig(SongLinkService songLinkService) {
        this.songLinkService = songLinkService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = songLinkService.chartDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploaded-charts/**")
                .addResourceLocations(location);
    }
}
