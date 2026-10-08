package co.edu.unbosque.acciones_elbosque.identity_service.service.impl;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.WatchlistResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.InvalidSymbolException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.PremiumRequiredException;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.WatchlistDuplicateException;
import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItem;
import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItemId;
import co.edu.unbosque.acciones_elbosque.identity_service.repository.WatchlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceImplTest {

    @Mock WatchlistItemRepository watchlistItemRepository;

    @InjectMocks WatchlistServiceImpl watchlistService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void addSymbol_premium_validSymbol_savesItem() {
        given(watchlistItemRepository.existsByIdUserIdAndIdSymbol(userId, "AAPL")).willReturn(false);

        watchlistService.addSymbol(userId, "PREMIUM", "AAPL");

        verify(watchlistItemRepository).save(any(WatchlistItem.class));
    }

    @Test
    void addSymbol_premium_duplicateSymbol_throws409() {
        given(watchlistItemRepository.existsByIdUserIdAndIdSymbol(userId, "AAPL")).willReturn(true);

        assertThatThrownBy(() -> watchlistService.addSymbol(userId, "PREMIUM", "AAPL"))
                .isInstanceOf(WatchlistDuplicateException.class);
    }

    @Test
    void removeSymbol_premium_deletesItem() {
        watchlistService.removeSymbol(userId, "PREMIUM", "AAPL");

        verify(watchlistItemRepository).deleteByIdUserIdAndIdSymbol(userId, "AAPL");
    }

    @Test
    void getWatchlist_returnsSymbolList() {
        WatchlistItem item1 = new WatchlistItem(new WatchlistItemId(userId, "AAPL"));
        WatchlistItem item2 = new WatchlistItem(new WatchlistItemId(userId, "MSFT"));
        given(watchlistItemRepository.findByIdUserId(userId)).willReturn(List.of(item1, item2));

        WatchlistResponse response = watchlistService.getWatchlist(userId);

        assertThat(response.symbols()).containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    void addSymbol_basicPlan_throws403() {
        assertThatThrownBy(() -> watchlistService.addSymbol(userId, "BASIC", "AAPL"))
                .isInstanceOf(PremiumRequiredException.class);
    }

    @Test
    void addSymbol_invalidSymbol_throws400() {
        assertThatThrownBy(() -> watchlistService.addSymbol(userId, "PREMIUM", "INVALID"))
                .isInstanceOf(InvalidSymbolException.class);
    }
}
