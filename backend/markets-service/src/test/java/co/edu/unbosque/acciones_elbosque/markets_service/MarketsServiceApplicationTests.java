package co.edu.unbosque.acciones_elbosque.markets_service;

import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaMarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class MarketsServiceApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
			.withInitScript("test-init.sql");

	@MockitoBean
	AlpacaMarketDataService alpacaMarketDataService;

	@Test
	void contextLoads() {
	}

}
