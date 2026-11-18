package com.nethub.engine;

import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.model.PathResult;
import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.service.RoutingTableService;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class NetworkPathFinder {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RoutingTableService routingTableService;
    private final RoutingEngine routingEngine;

    public PathResult findPath(Long projectId, Long sourceDeviceId, String destinationIp) {
        Device sourceDevice = deviceRepository.findById(sourceDeviceId).orElse(null);
        if (sourceDevice == null || !sourceDevice.getProjectId().equals(projectId)) {
            return PathResult.builder()
                    .reachable(false)
                    .reason("Source device not found in project")
                    .build();
        }

        if (sourceDevice.getStatus() != DeviceStatus.ONLINE) {
            return PathResult.builder()
                    .reachable(false)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .reason("SOURCE_DEVICE_OFFLINE: Source device " + sourceDevice.getName() + " is offline")
                    .build();
        }

        if (!IpUtils.isValidIPv4(destinationIp)) {
            return PathResult.builder()
                    .reachable(false)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .reason("INVALID_DESTINATION_IP: Invalid destination IP address format " + destinationIp)
                    .build();
        }

        List<Device> allDevices = deviceRepository.findByProjectId(projectId);
        List<Long> deviceIds = allDevices.stream().map(Device::getId).toList();
        List<NetworkInterface> allInterfaces = deviceIds.isEmpty() ? new ArrayList<>() : interfaceRepository.findByDeviceIdIn(deviceIds);
        List<Connection> allConnections = connectionRepository.findByProjectId(projectId);

        Map<Long, Device> deviceMap = new HashMap<>();
        for (Device d : allDevices) deviceMap.put(d.getId(), d);

        Map<Long, NetworkInterface> intfMap = new HashMap<>();
        for (NetworkInterface ni : allInterfaces) intfMap.put(ni.getId(), ni);

        // Locate destination device
        Device destDevice = null;
        NetworkInterface destIntf = null;
        for (NetworkInterface ni : allInterfaces) {
            if (destinationIp.equals(ni.getIpAddress())) {
                destIntf = ni;
                destDevice = deviceMap.get(ni.getDeviceId());
                break;
            }
        }

        if (destDevice == null) {
            return PathResult.builder()
                    .reachable(false)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .reason("DESTINATION_UNREACHABLE: No device found with IP " + destinationIp)
                    .build();
        }

        if (destDevice.getStatus() != DeviceStatus.ONLINE) {
            return PathResult.builder()
                    .reachable(false)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .destinationDevice(destDevice.getName())
                    .reason("DESTINATION_OFFLINE: Target device " + destDevice.getName() + " is offline")
                    .build();
        }

        // Check if source and destination are the same device
        if (sourceDevice.getId().equals(destDevice.getId())) {
            return PathResult.builder()
                    .reachable(true)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .destinationDevice(destDevice.getName())
                    .path(List.of(sourceDevice.getName()))
                    .nodeIds(List.of(sourceDevice.getId()))
                    .build();
        }

        // Graph simulation / path search
        Set<Long> visitedDevices = new HashSet<>();
        List<String> pathNames = new ArrayList<>();
        List<Long> nodeIds = new ArrayList<>();

        boolean success = tracePathRecursive(sourceDevice, destDevice, destinationIp,
                deviceMap, intfMap, allConnections, visitedDevices, pathNames, nodeIds);

        if (!success) {
            return PathResult.builder()
                    .reachable(false)
                    .sourceDevice(sourceDevice.getName())
                    .destinationIp(destinationIp)
                    .destinationDevice(destDevice.getName())
                    .reason("NO_NETWORK_PATH: Unable to find active path from " + sourceDevice.getName() + " to " + destDevice.getName())
                    .build();
        }

        return PathResult.builder()
                .reachable(true)
                .sourceDevice(sourceDevice.getName())
                .destinationIp(destinationIp)
                .destinationDevice(destDevice.getName())
                .path(pathNames)
                .nodeIds(nodeIds)
                .build();
    }

    private boolean tracePathRecursive(Device currentDevice, Device targetDevice, String targetIp,
                                       Map<Long, Device> deviceMap, Map<Long, NetworkInterface> intfMap,
                                       List<Connection> connections, Set<Long> visited,
                                       List<String> pathNames, List<Long> nodeIds) {

        if (visited.contains(currentDevice.getId())) {
            return false;
        }
        visited.add(currentDevice.getId());
        pathNames.add(currentDevice.getName());
        nodeIds.add(currentDevice.getId());

        if (currentDevice.getId().equals(targetDevice.getId())) {
            return true;
        }

        // Find outgoing connections from current device
        List<Connection> activeConns = getActiveConnectionsForDevice(currentDevice.getId(), connections, intfMap);

        for (Connection conn : activeConns) {
            Long nextIntfId = conn.getSourceInterfaceId().equals(getDeviceInterfaceId(currentDevice.getId(), conn, intfMap))
                    ? conn.getDestinationInterfaceId()
                    : conn.getSourceInterfaceId();

            NetworkInterface nextIntf = intfMap.get(nextIntfId);
            if (nextIntf == null || nextIntf.getStatus() != InterfaceStatus.UP) continue;

            Device nextDevice = deviceMap.get(nextIntf.getDeviceId());
            if (nextDevice == null || nextDevice.getStatus() != DeviceStatus.ONLINE) continue;

            // If current device is a Router, verify routing lookup
            if (currentDevice.getType() == DeviceType.ROUTER) {
                RoutingTable table = routingTableService.buildRoutingTable(currentDevice.getId());
                Optional<RouteEntry> route = routingEngine.lookupBestRoute(table, targetIp);
                if (route.isEmpty()) {
                    continue; // Router has no route to target
                }
            }

            if (tracePathRecursive(nextDevice, targetDevice, targetIp, deviceMap, intfMap, connections, visited, pathNames, nodeIds)) {
                return true;
            }
        }

        // Backtrack
        pathNames.remove(pathNames.size() - 1);
        nodeIds.remove(nodeIds.size() - 1);
        return false;
    }

    private Long getDeviceInterfaceId(Long deviceId, Connection conn, Map<Long, NetworkInterface> intfMap) {
        NetworkInterface src = intfMap.get(conn.getSourceInterfaceId());
        if (src != null && src.getDeviceId().equals(deviceId)) return src.getId();
        return conn.getDestinationInterfaceId();
    }

    private List<Connection> getActiveConnectionsForDevice(Long deviceId, List<Connection> connections, Map<Long, NetworkInterface> intfMap) {
        List<Connection> active = new ArrayList<>();
        for (Connection c : connections) {
            if (c.getStatus() != ConnectionStatus.ACTIVE) continue;
            NetworkInterface src = intfMap.get(c.getSourceInterfaceId());
            NetworkInterface dst = intfMap.get(c.getDestinationInterfaceId());
            if (src != null && dst != null && src.getStatus() == InterfaceStatus.UP && dst.getStatus() == InterfaceStatus.UP) {
                if (src.getDeviceId().equals(deviceId) || dst.getDeviceId().equals(deviceId)) {
                    active.add(c);
                }
            }
        }
        return active;
    }
}
