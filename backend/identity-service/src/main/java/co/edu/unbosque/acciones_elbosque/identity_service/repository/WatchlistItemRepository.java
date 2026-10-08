package co.edu.unbosque.acciones_elbosque.identity_service.repository;

import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItem;
import co.edu.unbosque.acciones_elbosque.identity_service.model.WatchlistItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, WatchlistItemId> {

    List<WatchlistItem> findByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdSymbol(UUID userId, String symbol);

    void deleteByIdUserIdAndIdSymbol(UUID userId, String symbol);
}
