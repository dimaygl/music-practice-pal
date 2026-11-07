package org.musicpracticepal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.musicpracticepal.model.Song;
import org.musicpracticepal.model.SongLink;
import org.musicpracticepal.repository.LessonRepository;
import org.musicpracticepal.repository.SongRepository;
import org.musicpracticepal.repository.SongLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:practicepal-test;DB_CLOSE_DELAY=-1"
})
class PracticePalApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongLinkRepository songLinkRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void sampleSongsAndLessonsArePersistedInJpa() {
        assertEquals(List.of("song-1"), songRepository.findAllByOrderByPositionAsc().stream()
                .map(Song::getName)
                .toList());
        assertEquals(1, lessonRepository.findAllByOrderByPositionAsc().size());
        assertEquals("lesson-1", lessonRepository.findAllByOrderByPositionAsc().getFirst().getName());
        assertNull(lessonRepository.findAllByOrderByPositionAsc().getFirst().getLinkUrl());
    }

    @Test
    void homePageRendersLoginModal() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Play backing track\"")))
                .andExpect(content().string(containsString("progression-type")))
                .andExpect(content().string(containsString("Jazz turnaround")))
                .andExpect(content().string(containsString("12-bar Blues")))
                .andExpect(content().string(containsString("Quick-change Blues")))
                .andExpect(content().string(containsString("Jazz Blues")))
                .andExpect(content().string(containsString("id=\"scale-type\"")))
                .andExpect(content().string(containsString("F# minor (A major)")))
                .andExpect(content().string(containsString("G minor (Bb major)")))
                .andExpect(content().string(containsString("data-progressions=")))
                .andExpect(content().string(containsString("progression-tempo")))
                .andExpect(content().string(containsString("/js/chord-progression.js")))
                .andExpect(content().string(containsString("Log in to Practice Pal")))
                .andExpect(content().string(containsString("name=\"username\"")));
    }

    @Test
    void pianoSampleIsServedForProgressionPlayback() throws Exception {
        mockMvc.perform(get("/audio/piano/C4.mp3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"));
    }

    @Test
    void songChartsPageOffersLoginForUploads() throws Exception {
        mockMvc.perform(get("/song-charts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Log in to Practice Pal")))
                .andExpect(content().string(containsString("id=\"login-modal\"")));
    }

    @Test
    void uploadedChartIsStoredInDatabaseAndServedBySongId() throws Exception {
        Song song = songRepository.findAllByOrderByPositionAsc().getFirst();
        byte[] imageBytes = pngImage();

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(multipart("/song-charts/0/upload")
                        .file(new MockMultipartFile("chart", "chart.png", "image/png", imageBytes))
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/song-charts"));

        Song persistedSong = songRepository.findById(song.getId()).orElseThrow();
        assertArrayEquals(imageBytes, persistedSong.getSongChart());
        assertEquals("image/png", persistedSong.getSongChartContentType());
        mockMvc.perform(get("/songs/" + song.getId() + "/chart-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void defaultAdminCanLogInAndAnonymousUploadIsRedirectedToLogin() throws Exception {
        mockMvc.perform(multipart("/song-charts/0/upload")
                        .file(new MockMultipartFile("chart", "chart.png", "image/png", new byte[0]))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?loginRequired"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(multipart("/song-charts/0/upload")
                        .file(new MockMultipartFile("chart", "chart.png", "image/png", new byte[0]))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/song-charts"));
    }

    @Test
    void adminCanStoreSongLinkAndAnonymousCannot() throws Exception {
        List<Song> songs = songRepository.findAllByOrderByPositionAsc();
        int songIndex = Math.floorMod(
                LocalDate.now().toEpochDay() - LocalDate.of(1970, 1, 5).toEpochDay(), songs.size());
        String songName = songs.get(songIndex).getName();
        String linkUrl = "https://example.com/practice-link";

        mockMvc.perform(post("/songs/" + songIndex + "/links")
                        .param("url", linkUrl)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?loginRequired"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a song link")));

        mockMvc.perform(post("/songs/" + songIndex + "/links")
                        .param("url", linkUrl)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertTrue(songLinkRepository.existsBySong_NameAndLinkUrl(songName, linkUrl));
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(linkUrl)));
    }

    @Test
    void adminCanUpdateSingleLessonLink() throws Exception {
        org.musicpracticepal.model.Lesson lesson = lessonRepository.findAllByOrderByPositionAsc().getFirst();
        String linkUrl = "https://example.com/lesson";

        mockMvc.perform(post("/lessons/" + lesson.getId() + "/link")
                        .param("url", linkUrl)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?loginRequired"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/lessons/" + lesson.getId() + "/link")
                        .param("url", linkUrl)
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/"));

        assertEquals(linkUrl, lessonRepository.findById(lesson.getId()).orElseThrow().getLinkUrl());
        mockMvc.perform(post("/lessons/" + lesson.getId() + "/link")
                        .param("url", "")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertNull(lessonRepository.findById(lesson.getId()).orElseThrow().getLinkUrl());
    }

    @Test
    void adminCanAddAndRemoveLessonsAndAnonymousCannot() throws Exception {
        String lessonName = "Temporary Lesson";
        mockMvc.perform(get("/lessons"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Daily Tasks")))
                .andExpect(content().string(containsString("Songs")))
                .andExpect(content().string(containsString("Lessons")));

        mockMvc.perform(post("/lessons")
                        .param("name", lessonName)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?loginRequired"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/lessons")
                        .param("name", "  " + lessonName + "  ")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/lessons"));

        org.musicpracticepal.model.Lesson addedLesson =
                lessonRepository.findAllByOrderByPositionAsc().stream()
                        .filter(lesson -> lesson.getName().equals(lessonName))
                        .findFirst()
                        .orElseThrow();
        mockMvc.perform(get("/lessons").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(lessonName)));

        mockMvc.perform(post("/lessons/" + addedLesson.getId() + "/remove")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/lessons"));

        assertTrue(lessonRepository.findById(addedLesson.getId()).isEmpty());
        List<org.musicpracticepal.model.Lesson> remainingLessons =
                lessonRepository.findAllByOrderByPositionAsc();
        for (int position = 0; position < remainingLessons.size(); position++) {
            assertEquals(position, remainingLessons.get(position).getPosition());
        }
    }

    @Test
    void adminCanAddAndRemoveSongsAndAnonymousCannot() throws Exception {
        mockMvc.perform(post("/songs")
                        .param("name", "Temporary Song")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?loginRequired"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/song-charts").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a song")))
                .andExpect(content().string(containsString("Remove song")));

        mockMvc.perform(post("/songs")
                        .param("name", "  Temporary Song  ")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/song-charts"));

        Song addedSong = songRepository.findByName("Temporary Song").orElseThrow();
        assertTrue(addedSong.getPosition() >= 0);
        String linkUrl = "https://example.com/temporary-song";
        songLinkRepository.saveAndFlush(new SongLink(addedSong, linkUrl));

        mockMvc.perform(post("/songs/" + addedSong.getId() + "/remove")
                        .session(session)
                        .with(csrf()))
                .andExpect(redirectedUrl("/song-charts"));

        assertTrue(songRepository.findById(addedSong.getId()).isEmpty());
        assertTrue(!songLinkRepository.existsBySong_NameAndLinkUrl("Temporary Song", linkUrl));
        List<Song> remainingSongs = songRepository.findAllByOrderByPositionAsc();
        for (int position = 0; position < remainingSongs.size(); position++) {
            assertEquals(position, remainingSongs.get(position).getPosition());
        }
    }

    private static byte[] pngImage() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }
}
