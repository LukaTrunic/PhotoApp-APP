package hr.algebra.photoapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoGalleryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void gallery_isAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/photos"))
                .andExpect(status().isOk())
                .andExpect(view().name("photos"))
                .andExpect(content().string(containsString("Search Photos")));
    }
}
