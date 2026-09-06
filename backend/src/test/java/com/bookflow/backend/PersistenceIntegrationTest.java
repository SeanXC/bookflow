package com.bookflow.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.Appointment;
import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.customer.Customer;
import com.bookflow.backend.customer.CustomerRepository;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;
import com.bookflow.backend.tenant.Tenant;
import com.bookflow.backend.tenant.TenantRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
	"bookflow.security.jwt.secret=test-only-secret-at-least-32-characters"
})
@Import(TestcontainersConfiguration.class)
@Transactional
class PersistenceIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private StaffRepository staffRepository;

	@Autowired
	private ServiceRepository serviceRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Test
	void flywayCreatesTheSixCoreTables() {
		Long tableCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN (
					'tenants',
					'users',
					'staff',
					'services',
					'customers',
					'appointments'
				  )
				""", Long.class);
		Long migrationCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE success = TRUE
				""", Long.class);

		assertEquals(6L, tableCount);
		assertEquals(1L, migrationCount);
	}

	@Test
	void customerSearchNeverReturnsAnotherTenantsCustomers() {
		Tenant tenantA = saveTenant("Studio A", "studio-a@example.com");
		Tenant tenantB = saveTenant("Studio B", "studio-b@example.com");
		customerRepository.save(new Customer(
				tenantA,
				"Emma",
				"Smith",
				"emma.a@example.com",
				"111",
				null));
		customerRepository.save(new Customer(
				tenantB,
				"Emma",
				"Jones",
				"emma.b@example.com",
				"222",
				null));
		customerRepository.flush();

		Page<Customer> result = customerRepository.searchByTenantId(
				tenantA.getId(),
				"Emma",
				PageRequest.of(0, 20));

		assertEquals(1, result.getTotalElements());
		assertEquals("emma.a@example.com", result.getContent().getFirst().getEmail());
		assertEquals(tenantA.getId(), result.getContent().getFirst().getTenant().getId());
	}

	@Test
	void appointmentFiltersRemainTenantScopedAndFetchDisplayRelationships() {
		Tenant tenantA = saveTenant("Studio A", "studio-a@example.com");
		Tenant tenantB = saveTenant("Studio B", "studio-b@example.com");
		Appointment appointmentA = saveAppointment(
				tenantA,
				"Emma",
				"Anna",
				"Haircut",
				Instant.parse("2026-09-12T14:00:00Z"));
		Long staffId = appointmentA.getStaff().getId();
		saveAppointment(
				tenantB,
				"Emma",
				"Sophie",
				"Haircut",
				Instant.parse("2026-09-12T14:00:00Z"));
		entityManager.flush();
		entityManager.clear();

		Page<Appointment> result =
				appointmentRepository.findAllByTenantIdAndFilters(
						tenantA.getId(),
						staffId,
						null,
						null,
						null,
						PageRequest.of(0, 20));

		assertEquals(1, result.getTotalElements());
		Appointment loaded = result.getContent().getFirst();
		assertEquals("Emma", loaded.getCustomer().getFirstName());
		assertEquals("Anna", loaded.getStaff().getFirstName());
		assertEquals("Haircut", loaded.getService().getName());
	}

	@Test
	void databaseRejectsAnAppointmentWithACustomerFromAnotherTenant() {
		Tenant tenantA = saveTenant("Studio A", "studio-a@example.com");
		Tenant tenantB = saveTenant("Studio B", "studio-b@example.com");
		Customer customerB = customerRepository.saveAndFlush(new Customer(
				tenantB,
				"Emma",
				"Smith",
				"emma@example.com",
				null,
				null));
		Staff staffA = staffRepository.saveAndFlush(new Staff(
				tenantA,
				null,
				"Anna",
				"Smith",
				null));
		com.bookflow.backend.service.Service serviceA =
				serviceRepository.saveAndFlush(new com.bookflow.backend.service.Service(
						tenantA,
						"Haircut",
						null,
						new BigDecimal("30.00"),
						60));
		Instant startTime = Instant.parse("2026-09-12T14:00:00Z");
		Appointment crossTenantAppointment = new Appointment(
				tenantA,
				customerB,
				staffA,
				serviceA,
				startTime,
				startTime.plusSeconds(60 * 60),
				null);

		assertThrows(
				DataIntegrityViolationException.class,
				() -> appointmentRepository.saveAndFlush(crossTenantAppointment));
	}

	private Tenant saveTenant(String name, String email) {
		return tenantRepository.saveAndFlush(new Tenant(name, email, null));
	}

	private Appointment saveAppointment(
			Tenant tenant,
			String customerFirstName,
			String staffFirstName,
			String serviceName,
			Instant startTime) {
		Customer customer = customerRepository.save(new Customer(
				tenant,
				customerFirstName,
				"Smith",
				customerFirstName.toLowerCase()
						+ "."
						+ tenant.getId()
						+ "@example.com",
				null,
				null));
		Staff staff = staffRepository.save(new Staff(
				tenant,
				null,
				staffFirstName,
				"Smith",
				null));
		com.bookflow.backend.service.Service service =
				serviceRepository.save(new com.bookflow.backend.service.Service(
						tenant,
						serviceName,
						null,
						new BigDecimal("30.00"),
						60));
		return appointmentRepository.save(new Appointment(
				tenant,
				customer,
				staff,
				service,
				startTime,
				startTime.plusSeconds(60 * 60),
				null));
	}
}
