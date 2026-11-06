package org.musicpracticepal.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.musicpracticepal.model.SongLink;
import org.musicpracticepal.repository.SongLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SongLinkService {

    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif");

    private final SongLinkRepository songLinkRepository;
    private final Path chartDirectory;

    public SongLinkService(
            SongLinkRepository songLinkRepository,
            @Value("${practice-pal.upload-dir:uploads/song-charts}") String chartDirectory) {
        this.songLinkRepository = songLinkRepository;
        this.chartDirectory = Path.of(chartDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<String> findBySongName(String songName) {
        return songLinkRepository.findBySongNameOrderByIdAsc(songName).stream()
                .map(SongLink::getLinkUrl)
                .toList();
    }

    @Transactional
    public boolean addLink(String songName, String linkUrl) {
        if (songLinkRepository.existsBySongNameAndLinkUrl(songName, linkUrl)) {
            return false;
        }
        songLinkRepository.save(new SongLink(songName, linkUrl));
        return true;
    }

    public Path chartDirectory() {
        return chartDirectory;
    }

    public String chartImageUrl(int songIndex) {
        for (String extension : IMAGE_EXTENSIONS) {
            Path image = chartImagePath(songIndex, extension);
            if (Files.isRegularFile(image)) {
                return "/uploaded-charts/" + image.getFileName();
            }
        }
        return null;
    }

    public void storeChart(int songIndex, MultipartFile upload) throws IOException {
        if (upload.isEmpty()) {
            throw new InvalidChartImageException("Choose an image to upload.");
        }

        String extension = extensionOf(upload.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new InvalidChartImageException("Upload a PNG, JPG, JPEG, or GIF image.");
        }
        try (InputStream input = upload.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new InvalidChartImageException("The selected file is not a readable image.");
            }
        } catch (InvalidChartImageException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidChartImageException("The selected file is not a readable image.");
        }

        Files.createDirectories(chartDirectory);
        Path temporaryFile = Files.createTempFile(chartDirectory, "chart-", ".upload");
        try {
            try (InputStream input = upload.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporaryFile, chartImagePath(songIndex, extension),
                    StandardCopyOption.REPLACE_EXISTING);
            for (String existingExtension : IMAGE_EXTENSIONS) {
                if (!existingExtension.equals(extension)) {
                    Files.deleteIfExists(chartImagePath(songIndex, existingExtension));
                }
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private Path chartImagePath(int songIndex, String extension) {
        return chartDirectory.resolve("chart-" + songIndex + "." + extension);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int extensionSeparator = filename.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == filename.length() - 1) {
            return "";
        }
        return filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
    }

    public static class InvalidChartImageException extends RuntimeException {
        public InvalidChartImageException(String message) {
            super(message);
        }
    }
}
