package co.edu.unbosque.acciones_elbosque.identity_service.controller;

import co.edu.unbosque.acciones_elbosque.identity_service.dto.response.WatchlistResponse;
import co.edu.unbosque.acciones_elbosque.identity_service.exception.PremiumRequiredException;
import co.edu.unbosque.acciones_elbosque.identity_service.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public ResponseEntity<WatchlistResponse> getWatchlist(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String plan = extractPlan(auth);
        requirePremium(plan);
        return ResponseEntity.ok(watchlistService.getWatchlist(userId));
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<Void> addSymbol(@PathVariable String symbol, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String plan = extractPlan(auth);
        watchlistService.addSymbol(userId, plan, symbol);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeSymbol(@PathVariable String symbol, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String plan = extractPlan(auth);
        watchlistService.removeSymbol(userId, plan, symbol);
        return ResponseEntity.noContent().build();
    }

    private String extractPlan(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("PLAN_"))
                .map(a -> a.substring("PLAN_".length()))
                .findFirst()
                .orElse("BASIC");
    }

    private void requirePremium(String plan) {
        if (!"PREMIUM".equalsIgnoreCase(plan)) {
            throw new PremiumRequiredException("Funcionalidad exclusiva para usuarios Premium");
        }
    }
}
