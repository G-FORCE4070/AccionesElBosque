package co.edu.unbosque.acciones_elbosque.trading_service;

import co.edu.unbosque.acciones_elbosque.trading_service.service.AlpacaTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TradingServiceApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
			.withInitScript("test-init.sql");

	@MockitoBean
	AlpacaTokenService alpacaTokenService;

	@Test
	void contextLoads() {
	}
}
