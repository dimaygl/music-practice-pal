package org.musicpracticepal.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.musicpracticepal.model.Song;
import org.musicpracticepal.model.SongLink;
import org.musicpracticepal.repository.SongRepository;
import org.musicpracticepal.repository.SongLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SongService {

    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif");
    private static final String DATABASE_IMAGE_PATH = "/songs/%d/chart-image";

    private final SongRepository songRepository;
    private final SongLinkRepository songLinkRepository;

    public SongService(
            SongRepository songRepository,
            SongLinkRepository songLinkRepository) {
        this.songRepository = songRepository;
        this.songLinkRepository = songLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<String> findBySongName(String songName) {
        return songLinkRepository.findBySong_NameOrderByIdAsc(songName).stream()
                .map(SongLink::getLinkUrl)
                .toList();
    }

    @Transactional
    public boolean addLink(String songName, String linkUrl) {
        if (songLinkRepository.existsBySong_NameAndLinkUrl(songName, linkUrl)) {
            return false;
        }
        Song song = songRepository.findByName(songName)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songName));
        songLinkRepository.save(new SongLink(song, linkUrl));
        return true;
    }

    @Transactional(readOnly = true)
    public String chartImageUrl(int songIndex) {
        List<Song> songs = songRepository.findAllByOrderByPositionAsc();
        if (songIndex < 0 || songIndex >= songs.size()) {
            return null;
        }
        Song song = songs.get(songIndex);
        if (song.getSongChart() != null) {
            return DATABASE_IMAGE_PATH.formatted(song.getId());
        }
        return null;
    }

    @Transactional
    public void storeChart(Long songId, MultipartFile upload) throws IOException {
        if (upload.isEmpty()) {
            throw new InvalidChartImageException("Choose an image to upload.");
        }

        String extension = extensionOf(upload.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new InvalidChartImageException("Upload a PNG, JPG, JPEG, or GIF image.");
        }
        byte[] imageData;
        try (InputStream input = upload.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new InvalidChartImageException("The selected file is not a readable image.");
            }
        } catch (InvalidChartImageException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidChartImageException("The selected file is not a readable image.");
        }
        imageData = upload.getBytes();
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
        song.setSongChart(imageData);
        song.setSongChartContentType(contentTypeFor(extension));
        songRepository.save(song);
    }

    @Transactional(readOnly = true)
    public ChartImage chartImage(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));
        if (song.getSongChart() == null) {
            return null;
        }
        return new ChartImage(song.getSongChart(), song.getSongChartContentType());
    }

    private static String contentTypeFor(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> throw new IllegalArgumentException("Unsupported image extension: " + extension);
        };
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

    public record ChartImage(byte[] data, String contentType) {}
}
