package co.edu.unbosque.acciones_elbosque.trading_service.service;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.response.PortfolioResponse;

import java.util.UUID;

public interface PortfolioService {

    PortfolioResponse getPortfolio(UUID userId, String token);
}
