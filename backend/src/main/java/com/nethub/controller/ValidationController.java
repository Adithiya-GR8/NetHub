package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/validate")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping
    public ApiResponse<Map<String, Object>> validateProject(@PathVariable Long projectId) {
        return ApiResponse.success(validationService.validateProject(projectId), "Validation completed");
    }
}
