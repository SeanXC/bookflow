package com.bookflow.backend.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookflow.backend.appointment.AppointmentRepository;
import com.bookflow.backend.appointment.AppointmentStatus;
import com.bookflow.backend.availability.AvailabilityService;
import com.bookflow.backend.availability.AvailableSlot;
import com.bookflow.backend.common.exception.AppointmentConflictException;
import com.bookflow.backend.common.exception.InvalidOperationException;
import com.bookflow.backend.service.ServiceRepository;
import com.bookflow.backend.staff.Staff;
import com.bookflow.backend.staff.StaffRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, noRollbackFor = InvalidOperationException.class)
public class BookingToolExecutor {

	private final ServiceRepository serviceRepository;
	private final StaffRepository staffRepository;
	private final AvailabilityService availabilityService;
	private final AppointmentRepository appointmentRepository;
	private final JsonMapper jsonMapper;

	public String execute(Long tenantId, LlmToolCall toolCall) {
		return execute(tenantId, toolCall.name(), toolCall.argumentsJson());
	}

	public String execute(Long tenantId, String toolName, String argumentsJson) {
		JsonNode arguments = parseArguments(argumentsJson);
		if (arguments == null) {
			return error("Invalid tool arguments");
		}
		return switch (toolName) {
			case BookingToolContract.LIST_SERVICES -> listServices(tenantId);
			case BookingToolContract.LIST_STAFF -> listStaff(tenantId);
			case BookingToolContract.LIST_SLOTS -> listSlots(tenantId, arguments);
			case BookingToolContract.PROPOSE_BOOKING -> proposeBooking(tenantId, arguments);
			default -> throw new InvalidOperationException("Unknown booking tool: " + toolName);
		};
	}

	private String listServices(Long tenantId) {
		ArrayNode services = jsonMapper.createArrayNode();
		for (com.bookflow.backend.service.Service service : serviceRepository
				.findAllByTenantIdAndActiveOrderByNameAscIdAsc(tenantId, true)) {
			ObjectNode node = services.addObject();
			node.put("id", service.getId());
			node.put("name", service.getName());
			if (service.getDescription() != null) {
				node.put("description", service.getDescription());
			}
			node.set("price", jsonMapper.valueToTree(service.getPrice()));
			node.put("durationMinutes", service.getDurationMinutes());
		}
		return write(services);
	}

	private String listStaff(Long tenantId) {
		ArrayNode staffMembers = jsonMapper.createArrayNode();
		for (Staff staff : staffRepository
				.findAllByTenantIdAndActiveOrderByLastNameAscFirstNameAscIdAsc(
						tenantId,
						true)) {
			ObjectNode node = staffMembers.addObject();
			node.put("id", staff.getId());
			node.put("firstName", staff.getFirstName());
			node.put("lastName", staff.getLastName());
		}
		return write(staffMembers);
	}

	private String listSlots(Long tenantId, JsonNode arguments) {
		Long staffId = requiredLong(arguments, "staffId");
		Long serviceId = requiredLong(arguments, "serviceId");
		LocalDate fromDate = requiredDate(arguments, "from");
		LocalDate toDate = requiredDate(arguments, "to");
		if (staffId == null || serviceId == null || fromDate == null || toDate == null) {
			return error("Invalid tool arguments");
		}

		Staff staff = findActiveStaff(tenantId, staffId);
		if (staff == null) {
			return error("Staff not found: " + staffId);
		}
		com.bookflow.backend.service.Service service = findActiveService(tenantId, serviceId);
		if (service == null) {
			return error("Service not found: " + serviceId);
		}

		try {
			ArrayNode slots = jsonMapper.createArrayNode();
			for (AvailableSlot slot : availabilityService.calculateSlots(
					tenantId,
					staff,
					service,
					fromDate,
					toDate)) {
				ObjectNode node = slots.addObject();
				node.put("startTime", slot.startTime().toString());
				node.put("endTime", slot.endTime().toString());
			}
			return write(slots);
		} catch (InvalidOperationException exception) {
			return error(exception.getMessage());
		}
	}

	private String proposeBooking(Long tenantId, JsonNode arguments) {
		Long staffId = requiredLong(arguments, "staffId");
		Long serviceId = requiredLong(arguments, "serviceId");
		Instant startTime = requiredInstant(arguments, "startTime");
		String firstName = requiredText(arguments, "firstName");
		String lastName = requiredText(arguments, "lastName");
		String email = requiredText(arguments, "email");
		String phone = requiredText(arguments, "phone");
		if (staffId == null
				|| serviceId == null
				|| startTime == null
				|| firstName == null
				|| lastName == null
				|| email == null
				|| phone == null) {
			return error("Invalid tool arguments");
		}

		Staff staff = findActiveStaff(tenantId, staffId);
		if (staff == null) {
			return error("Staff not found: " + staffId);
		}
		com.bookflow.backend.service.Service service = findActiveService(tenantId, serviceId);
		if (service == null) {
			return error("Service not found: " + serviceId);
		}

		Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);
		try {
			availabilityService.assertRequestedSlotIsAvailable(
					tenantId,
					staffId,
					service.getDurationMinutes(),
					startTime);
		} catch (InvalidOperationException exception) {
			return error(exception.getMessage());
		}

		long conflicts = appointmentRepository.countConflictingAppointments(
				tenantId,
				staffId,
				AppointmentStatus.CANCELLED,
				startTime,
				endTime);
		if (conflicts > 0) {
			return error(new AppointmentConflictException().getMessage());
		}

		ObjectNode proposal = jsonMapper.createObjectNode();
		proposal.put("status", "proposed");
		proposal.put("requiresConfirmation", true);
		proposal.put("staffId", staff.getId());
		proposal.put("staffFirstName", staff.getFirstName());
		proposal.put("staffLastName", staff.getLastName());
		proposal.put("serviceId", service.getId());
		proposal.put("serviceName", service.getName());
		proposal.set("price", jsonMapper.valueToTree(service.getPrice()));
		proposal.put("durationMinutes", service.getDurationMinutes());
		proposal.put("startTime", startTime.toString());
		proposal.put("endTime", endTime.toString());
		proposal.put("firstName", firstName);
		proposal.put("lastName", lastName);
		proposal.put("email", email);
		proposal.put("phone", phone);
		String notes = optionalText(arguments, "notes");
		if (notes != null) {
			proposal.put("notes", notes);
		}
		return write(proposal);
	}

	private Staff findActiveStaff(Long tenantId, Long staffId) {
		return staffRepository.findByIdAndTenantId(staffId, tenantId)
				.filter(Staff::isActive)
				.orElse(null);
	}

	private com.bookflow.backend.service.Service findActiveService(Long tenantId, Long serviceId) {
		return serviceRepository.findByIdAndTenantId(serviceId, tenantId)
				.filter(com.bookflow.backend.service.Service::isActive)
				.orElse(null);
	}

	private JsonNode parseArguments(String argumentsJson) {
		try {
			JsonNode node = jsonMapper.readTree(
					argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson);
			if (node == null || !node.isObject()) {
				return null;
			}
			return node;
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private Long requiredLong(JsonNode arguments, String field) {
		JsonNode node = arguments.get(field);
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		if (node.isNumber()) {
			return node.asLong();
		}
		if (node.isString()) {
			try {
				return Long.parseLong(node.asString().trim());
			} catch (NumberFormatException exception) {
				return null;
			}
		}
		return null;
	}

	private LocalDate requiredDate(JsonNode arguments, String field) {
		String value = requiredText(arguments, field);
		if (value == null) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private Instant requiredInstant(JsonNode arguments, String field) {
		String value = requiredText(arguments, field);
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private String requiredText(JsonNode arguments, String field) {
		return optionalText(arguments, field);
	}

	private String optionalText(JsonNode arguments, String field) {
		JsonNode node = arguments.get(field);
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		String value = node.asString();
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String error(String message) {
		ObjectNode node = jsonMapper.createObjectNode();
		node.put("error", message);
		return write(node);
	}

	private String write(JsonNode node) {
		return jsonMapper.writeValueAsString(node);
	}
}
