package co.edu.unbosque.acciones_elbosque.trading_service.repository;

import co.edu.unbosque.acciones_elbosque.trading_service.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
