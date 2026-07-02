package com.brewdeck.brewdeck_api.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated statistics")
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/summary")
  @Operation(
      summary = "Get dashboard summary",
      description = "Returns aggregate counts and the average brew-session rating.")
  public ResponseEntity<DashboardSummaryResponse> getSummary() {
    return ResponseEntity.ok(dashboardService.getSummary());
  }
}
