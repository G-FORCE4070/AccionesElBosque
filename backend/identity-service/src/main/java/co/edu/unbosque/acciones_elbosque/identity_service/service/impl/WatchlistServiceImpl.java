package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.WatchlistResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidSymbolException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.PremiumRequiredException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.WatchlistDuplicateException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItem;
import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItemId;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.WatchlistItemRepository;
import co.edu.unbosque.acciones_elbosque.identity_service.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private static final Set<String> VALID_SYMBOLS =
            Set.of("AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA", "JNJ", "JPM", "XOM");

    private final WatchlistItemRepository watchlistItemRepository;

    @Override
    public WatchlistResponse getWatchlist(UUID userId) {
        List<String> symbols = watchlistItemRepository.findByIdUserId(userId)
                .stream()
                .map(item -> item.getId().getSymbol())
                .toList();
        return new WatchlistResponse(symbols);
    }

    @Override
    @Transactional
    public void addSymbol(UUID userId, String plan, String symbol) {
        requirePremium(plan);
        String upper = symbol.toUpperCase();
        if (!VALID_SYMBOLS.contains(upper)) {
            throw new InvalidSymbolException("Símbolo no válido: " + symbol);
        }
        if (watchlistItemRepository.existsByIdUserIdAndIdSymbol(userId, upper)) {
            throw new WatchlistDuplicateException("El símbolo ya está en tu watchlist");
        }
        watchlistItemRepository.save(new WatchlistItem(new WatchlistItemId(userId, upper)));
    }

    @Override
    @Transactional
    public void removeSymbol(UUID userId, String plan, String symbol) {
        requirePremium(plan);
        watchlistItemRepository.deleteByIdUserIdAndIdSymbol(userId, symbol.toUpperCase());
    }

    private void requirePremium(String plan) {
        if (!"PREMIUM".equalsIgnoreCase(plan)) {
            throw new PremiumRequiredException("Funcionalidad exclusiva para usuarios Premium");
        }
    }
}
