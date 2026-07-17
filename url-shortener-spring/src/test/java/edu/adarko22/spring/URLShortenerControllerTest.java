package edu.adarko22.spring;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class URLShortenerControllerTest {
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        // Manually build MockMvc without needing auto-configuration annotations
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void testShortenAndRedirectSuccess() throws Exception {
        var originalUrl = "https://www.example.com";
        var request = new URLShortenerController.ShortenerRequest(originalUrl);
        var jsonRequest = objectMapper.writeValueAsString(request);
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shorten")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        var jsonResponse = result.getResponse().getContentAsString();
        var response = objectMapper.readValue(jsonResponse, URLShortenerController.ShortenerResponse.class);

        mockMvc.perform(MockMvcRequestBuilders.get(response.shortUrl()))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.header().string("location", originalUrl));
    }

    @Test
    void testShortenFailure() throws Exception {
        var badOriginalUrl = "bad-uri..format .com";
        var request = new URLShortenerController.ShortenerRequest(badOriginalUrl);
        var jsonRequest = objectMapper.writeValueAsString(request);
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shorten")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void testRedirectFailure() throws Exception {
        var badShortUrl = "https://localhost/non-existing-hash";
        mockMvc.perform(MockMvcRequestBuilders.get(badShortUrl))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}