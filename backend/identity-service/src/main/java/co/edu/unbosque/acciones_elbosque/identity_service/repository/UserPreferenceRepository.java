package co.edu.unbosque.acciones_elbosque.identity_service.repository;

import co.edu.unbosque.acciones_elbosque.identity_service.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
}
