package org.musicpracticepal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest
class PracticePalApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void homePageRendersLoginModal() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Log in to Practice Pal")))
                .andExpect(content().string(containsString("name=\"username\"")));
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
}
