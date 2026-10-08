package co.edu.unbosque.acciones_elbosque.markets_service.repository;

import co.edu.unbosque.acciones_elbosque.markets_service.model.MarketSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketScheduleRepository extends JpaRepository<MarketSchedule, String> {

    Optional<MarketSchedule> findByMarketCodeAndActiveTrue(String marketCode);
}
