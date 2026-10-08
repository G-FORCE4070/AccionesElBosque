package co.edu.unbosque.acciones_elbosque.markets_service.service.impl;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.AlpacaClockDto;
import co.edu.unbosque.acciones_elbosque.markets_service.service.AlpacaClockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
public class AlpacaClockServiceImpl implements AlpacaClockService {

    private static final String PAPER_CLOCK_URL = "https://paper-api.alpaca.markets/v2/clock";
    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final RestClient restClient;
    private final boolean testMode;

    public AlpacaClockServiceImpl(
            @Value("${alpaca.market-data.api-key}") String apiKey,
            @Value("${alpaca.market-data.secret-key}") String secretKey) {
        this.testMode = "test-key".equals(apiKey);
        this.restClient = RestClient.builder()
                .defaultHeader("APCA-API-KEY-ID", apiKey)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public boolean isMarketOpen() {
        if (testMode) {
            return isWithinMarketHours();
        }
        try {
            AlpacaClockDto clock = restClient.get()
                    .uri(PAPER_CLOCK_URL)
                    .retrieve()
                    .body(AlpacaClockDto.class);
            if (clock != null) {
                return clock.isOpen();
            }
            log.warn("Alpaca clock returned null body, falling back to time-based check");
            return isWithinMarketHours();
        } catch (RestClientException e) {
            log.warn("Could not fetch Alpaca clock: {}, falling back to time-based check", e.getMessage());
            return isWithinMarketHours();
        }
    }

    private boolean isWithinMarketHours() {
        ZonedDateTime now = ZonedDateTime.now(ET);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
}
