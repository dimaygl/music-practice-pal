package org.musicpracticepal.config;

import org.musicpracticepal.SongChartStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ChartUploadConfig implements WebMvcConfigurer {

    private final SongChartStorage chartStorage;

    ChartUploadConfig(SongChartStorage chartStorage) {
        this.chartStorage = chartStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = chartStorage.directory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploaded-charts/**")
                .addResourceLocations(location);
    }
}
