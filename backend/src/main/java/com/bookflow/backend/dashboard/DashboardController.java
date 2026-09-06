package com.bookflow.backend.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookflow.backend.dashboard.dto.DashboardSummaryResponse;
import com.bookflow.backend.security.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Owner analytics")
public class DashboardController {

	private final DashboardService dashboardService;
	private final CurrentUserProvider currentUserProvider;

	@GetMapping("/summary")
	@Operation(summary = "Get the current tenant's dashboard summary")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Summary returned"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "OWNER role required")
	})
	public ResponseEntity<DashboardSummaryResponse> getSummary() {
		return ResponseEntity.ok(
				dashboardService.getSummary(currentUserProvider.getTenantId()));
	}
}
