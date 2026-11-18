package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.DemonstrationScenario;
import com.nethub.entity.Project;
import com.nethub.service.DemonstrationScenarioSeeder;
import com.nethub.service.DemonstrationScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class DemonstrationScenarioController {

    private final DemonstrationScenarioService scenarioService;
    private final DemonstrationScenarioSeeder scenarioSeeder;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DemonstrationScenario>>> getScenarios(
            @RequestParam(required = false) String category) {
        List<DemonstrationScenario> scenarios = (category != null && !category.trim().isEmpty())
                ? scenarioService.getScenariosByCategory(category.trim())
                : scenarioService.getAllScenarios();
        return ResponseEntity.ok(ApiResponse.success(scenarios, "Demonstration scenarios retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemonstrationScenario>> getScenario(@PathVariable String id) {
        DemonstrationScenario scenario = scenarioService.getScenario(id);
        return ResponseEntity.ok(ApiResponse.success(scenario, "Scenario retrieved successfully"));
    }

    @PostMapping("/{id}/load")
    public ResponseEntity<ApiResponse<Project>> loadScenario(
            @PathVariable String id,
            @RequestParam(required = false) Long targetProjectId) {
        Project loadedProject = scenarioService.loadScenarioIntoProject(id, targetProjectId);
        return ResponseEntity.ok(ApiResponse.success(loadedProject, "Scenario loaded successfully into workspace"));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<ApiResponse<Void>> resetScenario(@PathVariable String id) {
        scenarioService.resetScenario(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Scenario reset to default state successfully"));
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Void>> reseedScenarios() {
        scenarioSeeder.seedAllScenarios();
        return ResponseEntity.ok(ApiResponse.success(null, "Demonstration scenarios re-seeded successfully"));
    }
}
