package co.edu.unbosque.acciones_elbosque.trading_service.service.impl;

import co.edu.unbosque.acciones_elbosque.trading_service.client.MarketsServiceClient;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.StockPriceDto;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.HoldingItem;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.PortfolioResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.AccountRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.repository.HoldingRepository;
import co.edu.unbosque.acciones_elbosque.trading_service.exception.PriceServiceUnavailableException;
import co.edu.unbosque.acciones_elbosque.trading_service.service.PortfolioService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("50000.00");

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final MarketsServiceClient marketsClient;

    public PortfolioServiceImpl(
            AccountRepository accountRepository,
            HoldingRepository holdingRepository,
            MarketsServiceClient marketsClient) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.marketsClient = marketsClient;
    }

    @Override
    public PortfolioResponse getPortfolio(UUID userId, String token) {
        Account account = accountRepository.findById(userId)
                .orElseGet(() -> accountRepository.save(new Account(userId, INITIAL_BALANCE, Instant.now())));

        List<Holding> holdings = holdingRepository.findAllByUserId(userId);
        boolean pricesAvailable = true;
        List<HoldingItem> items = new ArrayList<>();

        for (Holding h : holdings) {
            try {
                StockPriceDto price = marketsClient.getStockPrice(h.getId().getSymbol(), token);
                items.add(toHoldingItem(h, price.getPrice()));
            } catch (PriceServiceUnavailableException | RestClientException e) {
                pricesAvailable = false;
                items.add(toHoldingItemNoPrices(h));
            }
        }

        return PortfolioResponse.builder()
                .balance(account.getBalance())
                .pricesAvailable(pricesAvailable)
                .holdings(items)
                .build();
    }

    private HoldingItem toHoldingItemNoPrices(Holding holding) {
        return HoldingItem.builder()
                .symbol(holding.getId().getSymbol())
                .quantity(holding.getQuantity())
                .avgCost(holding.getAvgCost())
                .currentPrice(null)
                .totalValue(null)
                .gainPct(null)
                .gainDollars(null)
                .build();
    }

    private HoldingItem toHoldingItem(Holding holding, BigDecimal currentPrice) {
        int qty = holding.getQuantity();
        BigDecimal avgCost = holding.getAvgCost();
        BigDecimal totalValue = currentPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal costBasis = avgCost.multiply(BigDecimal.valueOf(qty));
        BigDecimal gainDollars = totalValue.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gainPct = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : gainDollars.divide(costBasis, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        return HoldingItem.builder()
                .symbol(holding.getId().getSymbol())
                .quantity(qty)
                .avgCost(avgCost)
                .currentPrice(currentPrice)
                .totalValue(totalValue.setScale(2, RoundingMode.HALF_UP))
                .gainPct(gainPct)
                .gainDollars(gainDollars)
                .build();
    }
}
