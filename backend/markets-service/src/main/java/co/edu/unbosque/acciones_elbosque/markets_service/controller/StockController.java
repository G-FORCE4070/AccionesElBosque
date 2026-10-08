package co.edu.unbosque.acciones_elbosque.markets_service.controller;

import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.MarketStatusResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.dto.response.StockResponse;
import co.edu.unbosque.acciones_elbosque.markets_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/stocks")
    public ResponseEntity<List<StockResponse>> getStocks(
            @RequestParam(required = false) String markets,
            @RequestParam(required = false) String sectors) {
        return ResponseEntity.ok(stockService.getStocks(markets, sectors));
    }

    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<StockResponse> getStock(@PathVariable String symbol) {
        return ResponseEntity.ok(stockService.getStock(symbol));
    }

    @GetMapping("/status/{symbol}")
    public ResponseEntity<MarketStatusResponse> getMarketStatus(@PathVariable String symbol) {
        return ResponseEntity.ok(stockService.getMarketStatus(symbol));
    }
}
