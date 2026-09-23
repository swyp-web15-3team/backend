package com.team3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import com.team3.auth.token.RefreshTokenRepository;
import com.team3.collection.CollectionRepository;
import com.team3.collection.CollectionWhiskyRepository;
import com.team3.planner.PlannerItemRepository;
import com.team3.planner.PlannerRepository;
import com.team3.exchange.ExchangeRateRepository;
import com.team3.curation.CurationRepository;
import com.team3.curation.CurationWhiskyRepository;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.WhiskyCategoryRepository;
import com.team3.whisky.WhiskyOriginRepository;
import com.team3.whisky.WhiskyRegionRepository;
import com.team3.whisky.WhiskyRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BackendApplicationTests {

    private final MockMvc mvc;

    BackendApplicationTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private UserRepository users;

    @MockitoBean
    private UserAgreementRepository agreements;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private WhiskyCategoryRepository whiskyCategories;

    @MockitoBean
    private CollectionRepository collections;

    @MockitoBean
    private WhiskyRepository whiskies;

    @MockitoBean
    private PriceHistoryRepository priceHistories;

    @MockitoBean
    private SaleProductRepository saleProducts;

    @MockitoBean
    private WhiskyOriginRepository whiskyOrigins;

    @MockitoBean
    private WhiskyRegionRepository whiskyRegions;

    @MockitoBean
    private CollectionWhiskyRepository collectionWhiskies;

    @MockitoBean
    private PlannerRepository planners;

    @MockitoBean
    private PlannerItemRepository plannerItems;

    @MockitoBean
    private ExchangeRateRepository exchangeRates;

    @MockitoBean
    private CurationRepository curationRepository;

    @MockitoBean
    private CurationWhiskyRepository curationWhiskies;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @Test
    void contextLoads() {
    }

    @Test
    void usesUtcTimeZone() {
        assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void healthEndpointDoesNotRequireAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
