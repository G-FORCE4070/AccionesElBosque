package co.edu.unbosque.acciones_elbosque.audit_service;

import co.edu.unbosque.acciones_elbosque.audit_service.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AuditServiceApplicationTests {

	@MockitoBean
	AuditEventRepository auditEventRepository;

	@Test
	void contextLoads() {
	}

}
