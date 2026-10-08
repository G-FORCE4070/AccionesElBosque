package co.edu.unbosque.acciones_elbosque.trading_service.repository;

import co.edu.unbosque.acciones_elbosque.trading_service.model.Holding;
import co.edu.unbosque.acciones_elbosque.trading_service.model.HoldingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, HoldingId> {

    @Query("SELECT h FROM Holding h WHERE h.id.userId = :userId")
    List<Holding> findAllByUserId(@Param("userId") UUID userId);
}
