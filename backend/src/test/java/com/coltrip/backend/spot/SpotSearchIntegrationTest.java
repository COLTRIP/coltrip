package com.coltrip.backend.spot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.like.SpotLike;
import com.coltrip.backend.domain.spot.*;
import com.coltrip.backend.domain.user.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class SpotSearchIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired JwtProvider jwt;
    MockMvc mvc;
    String prefix;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        prefix = "search-" + UUID.randomUUID();
    }

    @Test
    void partialMatchIncludesMissingDataAndAllModes() throws Exception {
        var spot = spot(prefix + " 국립해양박물관");
        em.persist(SpotMode.builder().spot(spot).mode(Mode.SENSORY).build());
        em.persist(SpotMode.builder().spot(spot).mode(Mode.TRANQUIL).build());
        flush();
        mvc.perform(get("/api/spots/search").param("keyword", "  " + prefix + " 국립해양  "))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.spots", hasSize(1)))
                .andExpect(jsonPath("$.spots[0].id").value(spot.getId()))
                .andExpect(jsonPath("$.spots[0].modes", containsInAnyOrder("SENSORY", "TRANQUIL")))
                .andExpect(jsonPath("$.spots[0].quietScore").isEmpty())
                .andExpect(jsonPath("$.spots[0].imageUrl").isEmpty())
                .andExpect(jsonPath("$.spots[0].isLiked").value(false));
    }

    @Test
    void pagesAreStableForDuplicateNamesAndPreserveUnclassifiedPlaces() throws Exception {
        var first = spot(prefix + " A");
        spot(prefix + " A");
        spot(prefix + " B");
        flush();
        mvc.perform(get("/api/spots/search").param("keyword", prefix).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots[0].id").value(first.getId()))
                .andExpect(jsonPath("$.spots[0].modes", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/spots/search").param("keyword", prefix).param("size", "1").param("page", "2"))
                .andExpect(jsonPath("$.spots[0].name").value(prefix + " B"))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/spots/search").param("keyword", prefix).param("page", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void wildcardsAreLiteralAndEnglishIgnoresCase() throws Exception {
        spot(prefix + " 100%_! Cafe");
        spot(prefix + " 100ABC Cafe");
        flush();
        mvc.perform(get("/api/spots/search").param("keyword", prefix + " 100%_! cafe"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots", hasSize(1)));
    }

    @Test
    void noResultsReturnsEmptyList() throws Exception {
        mvc.perform(get("/api/spots/search").param("keyword", prefix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void validatesKeywordAndPagination() throws Exception {
        mvc.perform(get("/api/spots/search")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MissingParameterException"));
        for (String keyword : new String[]{"", "  ", "a".repeat(101)}) {
            mvc.perform(get("/api/spots/search").param("keyword", keyword)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("InvalidSearchRequestException"));
        }
        for (String[] input : new String[][]{{"page", "-1"}, {"page", "10001"}, {"size", "0"}, {"size", "51"}, {"page", "abc"}}) {
            mvc.perform(get("/api/spots/search").param("keyword", prefix).param(input[0], input[1]))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/spots/search").param("keyword", "a".repeat(100)).param("size", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void likesArePersonalizedAndInvalidTokenRemainsPublic() throws Exception {
        var spot = spot(prefix);
        var user = User.builder().googleSub(prefix).email(prefix + "@example.test").build();
        em.persist(user);
        em.persist(SpotLike.builder().spot(spot).user(user).build());
        flush();
        mvc.perform(get("/api/spots/search").param("keyword", prefix)
                        .header("Authorization", "Bearer " + jwt.createAccessToken(user.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots[0].isLiked").value(true));
        mvc.perform(get("/api/spots/search").param("keyword", prefix).header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots[0].isLiked").value(false));
    }

    private TouristSpot spot(String name) {
        var spot = TouristSpot.builder().tourApiContentId(UUID.randomUUID().toString()).name(name)
                .address("부산광역시").latitude(new BigDecimal("35.1")).longitude(new BigDecimal("129.1"))
                .category(Category.GALLERY).build();
        em.persist(spot);
        return spot;
    }

    private void flush() {
        em.flush();
        em.clear();
    }
}
