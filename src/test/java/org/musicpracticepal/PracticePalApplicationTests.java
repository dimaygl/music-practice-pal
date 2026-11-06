package org.musicpracticepal;

import static org.hamcrest.Matchers.containsString;
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

import org.junit.jupiter.api.Test;
import org.musicpracticepal.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:practicepal-test;DB_CLOSE_DELAY=-1")
class PracticePalApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongRepository songRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void homePageRendersLoginModal() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Play backing track\"")))
                .andExpect(content().string(containsString("progression-type")))
                .andExpect(content().string(containsString("Jazz turnaround")))
                .andExpect(content().string(containsString("12-bar Blues")))
                .andExpect(content().string(containsString("id=\"scale-type\"")))
                .andExpect(content().string(containsString("F# minor (A major)")))
                .andExpect(content().string(containsString("G minor (A# major)")))
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
                .andExpect(content().string(containsString("Log in to upload")))
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
        assertTrue(songRepository.existsBySongNameAndLinkUrl(songName, linkUrl));
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(linkUrl)));
    }
}
