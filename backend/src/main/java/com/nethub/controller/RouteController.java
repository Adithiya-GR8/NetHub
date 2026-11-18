package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.entity.Route;
import com.nethub.service.RoutingTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RouteController {

    private final RoutingTableService routingTableService;

    @GetMapping("/devices/{deviceId}/static-routes")
    public ResponseEntity<ApiResponse<List<Route>>> getStaticRoutes(@PathVariable Long deviceId) {
        List<Route> routes = routingTableService.getStaticRoutes(deviceId);
        return ResponseEntity.ok(ApiResponse.success(routes, "Static routes retrieved successfully"));
    }

    @PostMapping("/devices/{deviceId}/routes")
    public ResponseEntity<ApiResponse<Route>> addStaticRoute(@PathVariable Long deviceId, @RequestBody Route route) {
        Route created = routingTableService.addStaticRoute(deviceId, route);
        return ResponseEntity.ok(ApiResponse.success(created, "Static route added successfully"));
    }

    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<ApiResponse<String>> deleteRoute(@PathVariable Long routeId) {
        routingTableService.deleteRoute(routeId);
        return ResponseEntity.ok(ApiResponse.success("Route deleted", "Static route deleted successfully"));
    }
}
