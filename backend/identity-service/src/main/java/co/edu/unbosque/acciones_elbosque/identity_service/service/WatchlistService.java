package co.edu.unbosque.acciones_elbosque.identity_service.service;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.WatchlistResponse;

import java.util.UUID;

public interface WatchlistService {

    WatchlistResponse getWatchlist(UUID userId);

    void addSymbol(UUID userId, String plan, String symbol);

    void removeSymbol(UUID userId, String plan, String symbol);
}
