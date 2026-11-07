package org.musicpracticepal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
        "spring.datasource.url=jdbc:h2:mem:practicepal-test;DB_CLOSE_DELAY=-1",
        "music-practice-pal.upload-dir=target/test-song-charts"
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
        assertEquals(
                List.of("Ain't No Sunshine", "Wind Cries Mary"),
                songRepository.findAllByOrderByPositionAsc().stream()
                        .map(song -> song.getName())
                        .toList());
        assertEquals(1, lessonRepository.findAllByOrderByPositionAsc().size());
        assertEquals(
                List.of("https://justinjohnsonguitar.com/programs/backporch-blues-bootcamp"
                        + "?utm_source=product&utm_medium=share&utm_campaign=content_share"
                        + "&utm_term=3584842&utm_content=copy"),
                lessonRepository.findAllByOrderByPositionAsc().getFirst().getLinks());
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
        int songIndex = Math.floorMod(
                LocalDate.now().toEpochDay() - LocalDate.of(1970, 1, 5).toEpochDay(), 2);
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

        String songName = songIndex == 0 ? "Ain't No Sunshine" : "Wind Cries Mary";
        assertTrue(songLinkRepository.existsBySong_NameAndLinkUrl(songName, linkUrl));
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(linkUrl)));
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
}
