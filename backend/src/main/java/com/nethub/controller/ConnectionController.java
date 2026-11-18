package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.Connection;
import com.nethub.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @GetMapping("/projects/{projectId}/connections")
    public ApiResponse<List<Connection>> getConnections(@PathVariable Long projectId) {
        return ApiResponse.success(connectionService.getConnectionsByProjectId(projectId), "Connections fetched successfully");
    }

    @PostMapping("/projects/{projectId}/connections")
    public ApiResponse<Connection> createConnection(@PathVariable Long projectId, @RequestBody Connection connection) {
        return ApiResponse.success(connectionService.createConnection(projectId, connection), "Connection created successfully");
    }

    @DeleteMapping("/connections/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ApiResponse.success(null, "Connection deleted successfully");
    }
}
