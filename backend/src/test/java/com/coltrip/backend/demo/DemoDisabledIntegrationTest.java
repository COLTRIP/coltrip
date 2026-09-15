package com.coltrip.backend.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class DemoDisabledIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired DemoAccessPolicy demo;

    @Test void defaultOffPreservesPublicReadsAndClientCannotEnableDemo() throws Exception {
        assertFalse(demo.enabled());
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/spots/recommendations/current").param("demo", "true").header("X-Demo", "true"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/visits/1/complete").param("demo", "true").header("X-Demo", "true"))
                .andExpect(status().isUnauthorized());
    }
}
