package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.Project;
import com.nethub.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<List<Project>> getAllProjects() {
        return ApiResponse.success(projectService.getAllProjects(), "Projects fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<Project> getProject(@PathVariable Long id) {
        return ApiResponse.success(projectService.getProject(id), "Project fetched successfully");
    }

    @PostMapping
    public ApiResponse<Project> createProject(@RequestBody Project project) {
        return ApiResponse.success(projectService.createProject(project), "Project created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<Project> updateProject(@PathVariable Long id, @RequestBody Project project) {
        return ApiResponse.success(projectService.updateProject(id, project), "Project updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.success(null, "Project deleted successfully");
    }
}
