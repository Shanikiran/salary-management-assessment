package com.acme.salary.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The SPA fallback must forward client-side routes to index.html while
 * leaving the API alone. (The actual index.html file only exists after
 * the Angular production build, so these tests assert the forward, not
 * the file content — the file itself is verified against the packaged jar.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpaControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void clientSideRoutesForwardToIndexHtml() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/employees/EMP-00001"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/import"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiRoutesAreNotForwarded() throws Exception {
        // /api/health is a real endpoint; it must answer normally, not forward.
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null));
    }
}
