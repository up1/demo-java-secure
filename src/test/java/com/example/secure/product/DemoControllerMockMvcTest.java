package com.example.secure.product;

import com.example.secure.config.SecurityConfig;
import com.example.secure.global.RateLimitingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
@Import({SecurityConfig.class, ProductService.class, RateLimitingService.class, UrlSecurityService.class}) // Import UrlSecurityService
class DemoControllerMockMvcTest {

    // --- Test Data ---
    private final String user1 = "user1";
    private final String pass1 = "password";
    @Autowired
    private MockMvc mockMvc;

    // ===============================================================
    // API7: Server Side Request Forgery (SSRF) Tests
    // ===============================================================

    @Test
    void userShouldBeAllowedToFetchSafeExternalUrl() throws Exception {
        // This host is explicitly listed in the UrlSecurityService allow-list
        String safeUrl = "https://jsonplaceholder.typicode.com/todos/1";

        mockMvc.perform(get("/api/v1/fetch-external")
                        .param("url", safeUrl)
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isOk()) // API7 Check (Success)
                // Check for expected content (part of the truncated JSON response)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("userId")));
    }

    @Test
    void userShouldBeForbiddenToFetchDisallowedHost() throws Exception {
        // This host is NOT in the allow-list
        String unsafeUrl = "https://www.google.com";

        mockMvc.perform(get("/api/v1/fetch-external")
                        .param("url", unsafeUrl)
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isBadRequest()) // API7 Check (Failure - Blocked by allow-list)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not safe or is restricted")));
    }

    @Test
    void userShouldBeForbiddenToFetchLocalhost() throws Exception {
        // Attempting to access the local machine (critical SSRF vector)
        String localhostUrl = "http://127.0.0.1/admin-config";

        // Note: The DNS resolution in the test environment should block this due to the
        // InetAddress.isLoopbackAddress() check in UrlSecurityService.
        mockMvc.perform(get("/api/v1/fetch-external")
                        .param("url", localhostUrl)
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isBadRequest()) // API7 Check (Failure - Blocked by IP check)
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not safe or is restricted")));
    }

}