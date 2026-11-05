package org.musicpracticepal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SongChartStorage {

    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif");

    private final Path directory;

    @Autowired
    public SongChartStorage(@Value("${practice-pal.upload-dir:uploads/song-charts}") String directory) {
        this(Path.of(directory));
    }

    SongChartStorage(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public Path directory() {
        return directory;
    }

    String imageUrl(int songIndex) {
        for (String extension : IMAGE_EXTENSIONS) {
            Path image = imagePath(songIndex, extension);
            if (Files.isRegularFile(image)) {
                return "/uploaded-charts/" + image.getFileName();
            }
        }
        return null;
    }

    void store(int songIndex, MultipartFile upload) throws IOException {
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

        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "chart-", ".upload");
        try {
            try (InputStream input = upload.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporaryFile, imagePath(songIndex, extension),
                    StandardCopyOption.REPLACE_EXISTING);
            for (String existingExtension : IMAGE_EXTENSIONS) {
                if (!existingExtension.equals(extension)) {
                    Files.deleteIfExists(imagePath(songIndex, existingExtension));
                }
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private Path imagePath(int songIndex, String extension) {
        return directory.resolve("chart-" + songIndex + "." + extension);
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

    static class InvalidChartImageException extends RuntimeException {
        InvalidChartImageException(String message) {
            super(message);
        }
    }
}
