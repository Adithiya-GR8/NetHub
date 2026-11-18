package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.dto.SimulateRequest;
import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.SwitchingEngine;
import com.nethub.entity.NetworkInterface;
import com.nethub.model.ArpEntry;
import com.nethub.model.MacTableEntry;
import com.nethub.model.PathResult;
import com.nethub.model.RoutingTable;
import com.nethub.model.SimulationResult;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.service.NetworkSimulationService;
import com.nethub.service.RoutingTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NetworkingController {

    private final NetworkInterfaceRepository interfaceRepository;
    private final DeviceRepository deviceRepository;
    private final SwitchingEngine switchingEngine;
    private final ArpEngine arpEngine;
    private final RoutingTableService routingTableService;
    private final NetworkSimulationService simulationService;
    private final NetworkPathFinder pathFinder;

    @GetMapping("/projects/{projectId}/network/interfaces")
    public ResponseEntity<ApiResponse<List<NetworkInterface>>> getProjectInterfaces(@PathVariable Long projectId) {
        var devices = deviceRepository.findByProjectId(projectId);
        var deviceIds = devices.stream().map(d -> d.getId()).toList();
        List<NetworkInterface> interfaces = deviceIds.isEmpty() ? new ArrayList<>() : interfaceRepository.findByDeviceIdIn(deviceIds);
        return ResponseEntity.ok(ApiResponse.success(interfaces, "Project interfaces retrieved successfully"));
    }

    @GetMapping("/projects/{projectId}/routing-tables")
    public ResponseEntity<ApiResponse<Map<Long, RoutingTable>>> getProjectRoutingTables(@PathVariable Long projectId) {
        Map<Long, RoutingTable> tables = routingTableService.getProjectRoutingTables(projectId);
        return ResponseEntity.ok(ApiResponse.success(tables, "Project routing tables retrieved successfully"));
    }

    @GetMapping("/devices/{deviceId}/mac-table")
    public ResponseEntity<ApiResponse<List<MacTableEntry>>> getDeviceMacTable(@PathVariable Long deviceId) {
        List<MacTableEntry> entries = switchingEngine.getMacTable(deviceId).getAllEntries();
        return ResponseEntity.ok(ApiResponse.success(entries, "MAC address table retrieved successfully"));
    }

    @GetMapping("/devices/{deviceId}/arp-table")
    public ResponseEntity<ApiResponse<List<ArpEntry>>> getDeviceArpTable(@PathVariable Long deviceId) {
        List<ArpEntry> entries = arpEngine.getArpTable(deviceId).getAllEntries();
        return ResponseEntity.ok(ApiResponse.success(entries, "ARP table retrieved successfully"));
    }

    @GetMapping("/devices/{deviceId}/routes")
    public ResponseEntity<ApiResponse<RoutingTable>> getDeviceRoutingTable(@PathVariable Long deviceId) {
        RoutingTable table = routingTableService.buildRoutingTable(deviceId);
        return ResponseEntity.ok(ApiResponse.success(table, "Routing table retrieved successfully"));
    }

    @PostMapping("/projects/{projectId}/simulate")
    public ResponseEntity<ApiResponse<SimulationResult>> simulateTransmission(
            @PathVariable Long projectId,
            @Valid @RequestBody SimulateRequest request) {

        SimulationResult result = simulationService.simulateTransmission(
                projectId, request.getSourceDeviceId(), request.getDestinationIp());
        return ResponseEntity.ok(ApiResponse.success(result, "Simulation processed successfully"));
    }

    @GetMapping("/projects/{projectId}/paths")
    public ResponseEntity<ApiResponse<PathResult>> getNetworkPath(
            @PathVariable Long projectId,
            @RequestParam Long sourceDeviceId,
            @RequestParam String destinationIp) {

        PathResult result = pathFinder.findPath(projectId, sourceDeviceId, destinationIp);
        return ResponseEntity.ok(ApiResponse.success(result, "Network path computed successfully"));
    }
}
