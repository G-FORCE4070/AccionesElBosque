package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.PortfolioResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.PriceServiceUnavailableException;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.model.HoldingId;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.AccountRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock MarketsServiceClient marketsClient;

    PortfolioServiceImpl service;

    UUID userId = UUID.randomUUID();
    String token = "tok";
    Account account = new Account(userId, new BigDecimal("10000.00"));

    @BeforeEach
    void setUp() {
        service = new PortfolioServiceImpl(accountRepository, holdingRepository, marketsClient);
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));
    }

    @Test
    void getPortfolio_noHoldings_returnsEmptyList() {
        given(holdingRepository.findAllByUserId(userId)).willReturn(List.of());

        PortfolioResponse response = service.getPortfolio(userId, token);

        assertThat(response.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(response.getHoldings()).isEmpty();
        assertThat(response.isPricesAvailable()).isTrue();
    }

    @Test
    void getPortfolio_withPrices_returnsHoldingsWithComputedFields() {
        Holding holding = new Holding(new HoldingId(userId, "AAPL"), 10, new BigDecimal("150.00"));
        given(holdingRepository.findAllByUserId(userId)).willReturn(List.of(holding));
        StockPriceDto priceDto = mock(StockPriceDto.class);
        given(priceDto.getPrice()).willReturn(new BigDecimal("160.00"));
        given(marketsClient.getStockPrice("AAPL", token)).willReturn(priceDto);

        PortfolioResponse response = service.getPortfolio(userId, token);

        assertThat(response.isPricesAvailable()).isTrue();
        assertThat(response.getHoldings()).hasSize(1);
        var item = response.getHoldings().get(0);
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("160.00");
        assertThat(item.getTotalValue()).isEqualByComparingTo("1600.00");
        assertThat(item.getGainDollars()).isEqualByComparingTo("100.00");
    }

    @Test
    void getPortfolio_pricesUnavailable_returnsHoldingsWithNullFields() {
        Holding holding = new Holding(new HoldingId(userId, "AAPL"), 10, new BigDecimal("150.00"));
        given(holdingRepository.findAllByUserId(userId)).willReturn(List.of(holding));
        given(marketsClient.getStockPrice("AAPL", token))
                .willThrow(new PriceServiceUnavailableException());

        PortfolioResponse response = service.getPortfolio(userId, token);

        assertThat(response.isPricesAvailable()).isFalse();
        assertThat(response.getHoldings()).hasSize(1);
        var item = response.getHoldings().get(0);
        assertThat(item.getSymbol()).isEqualTo("AAPL");
        assertThat(item.getQuantity()).isEqualTo(10);
        assertThat(item.getAvgCost()).isEqualByComparingTo("150.00");
        assertThat(item.getCurrentPrice()).isNull();
        assertThat(item.getTotalValue()).isNull();
        assertThat(item.getGainPct()).isNull();
        assertThat(item.getGainDollars()).isNull();
    }

    @Test
    void getPortfolio_accountNotFound_createsAccountWithInitialBalance() {
        given(accountRepository.findById(userId)).willReturn(Optional.empty());
        given(accountRepository.save(any())).willReturn(new Account(userId, new BigDecimal("50000.00")));
        given(holdingRepository.findAllByUserId(userId)).willReturn(List.of());

        PortfolioResponse response = service.getPortfolio(userId, token);

        assertThat(response.getBalance()).isEqualByComparingTo("50000.00");
    }
}
