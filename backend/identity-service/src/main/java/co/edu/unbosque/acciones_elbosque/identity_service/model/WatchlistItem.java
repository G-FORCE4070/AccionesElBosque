package co.edu.unbosque.acciones_elbosque.identity_service.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_items")
@Getter
@NoArgsConstructor
public class WatchlistItem {

    @EmbeddedId
    private WatchlistItemId id;

    private LocalDateTime addedAt;

    public WatchlistItem(WatchlistItemId id) {
        this.id = id;
        this.addedAt = LocalDateTime.now();
    }
}
