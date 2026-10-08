package co.edu.unbosque.acciones_elbosque.markets_service.repository;

import co.edu.unbosque.acciones_elbosque.markets_service.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {

    List<Stock> findByActiveTrueAndMarketCodeInAndSectorIn(List<String> marketCodes, List<String> sectors);

    List<Stock> findByActiveTrueAndMarketCodeIn(List<String> marketCodes);

    List<Stock> findByActiveTrueAndSectorIn(List<String> sectors);

    List<Stock> findByActiveTrue();
}
