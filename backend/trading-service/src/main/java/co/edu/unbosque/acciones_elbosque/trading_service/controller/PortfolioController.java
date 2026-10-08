package co.edu.unbosque.acciones_elbosque.trading_service.controller;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.PortfolioResponse;
import co.edu.unbosque.acciones_elbosque.trading_service.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(
            Authentication auth,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = UUID.fromString(auth.getName());
        String token = authHeader.substring(7);
        return ResponseEntity.ok(portfolioService.getPortfolio(userId, token));
    }
}
