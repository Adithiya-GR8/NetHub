package com.nethub.service;

import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.Route;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.entity.enums.RouteType;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.exception.ValidationException;
import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.RouteRepository;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoutingTableService {

    private final RouteRepository routeRepository;
    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;

    public RoutingTable buildRoutingTable(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        RoutingTable table = new RoutingTable();

        // 1. Add CONNECTED routes for all UP interfaces with configured IP & Mask
        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(deviceId);
        for (NetworkInterface intf : interfaces) {
            if (intf.getStatus() == InterfaceStatus.UP &&
                intf.getIpAddress() != null && !intf.getIpAddress().trim().isEmpty() &&
                intf.getSubnetMask() != null && !intf.getSubnetMask().trim().isEmpty() &&
                IpUtils.isValidIPv4(intf.getIpAddress()) && IpUtils.isValidSubnetMask(intf.getSubnetMask())) {

                String networkIp = IpUtils.getNetworkIp(intf.getIpAddress(), intf.getSubnetMask());

                table.addRoute(RouteEntry.builder()
                        .destinationNetwork(networkIp)
                        .subnetMask(intf.getSubnetMask())
                        .nextHop(null) // Directly connected
                        .outgoingInterface(intf.getName())
                        .metric(0)
                        .routeType(RouteType.CONNECTED)
                        .build());
            }
        }

        // 2. Add STATIC routes configured for this device
        List<Route> staticRoutes = routeRepository.findByDeviceId(deviceId);
        for (Route r : staticRoutes) {
            table.addRoute(RouteEntry.builder()
                    .id(r.getId())
                    .destinationNetwork(r.getDestinationNetwork())
                    .subnetMask(r.getSubnetMask())
                    .nextHop(r.getNextHop())
                    .outgoingInterface(r.getOutgoingInterface())
                    .metric(r.getMetric() != null ? r.getMetric() : 1)
                    .routeType(r.getRouteType() != null ? r.getRouteType() : RouteType.STATIC)
                    .build());
        }

        return table;
    }

    public List<Route> getStaticRoutes(Long deviceId) {
        return routeRepository.findByDeviceId(deviceId);
    }

    public Route addStaticRoute(Long deviceId, Route route) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        if (!IpUtils.isValidIPv4(route.getDestinationNetwork())) {
            throw new ValidationException("Invalid destination network IP: " + route.getDestinationNetwork());
        }
        if (!IpUtils.isValidSubnetMask(route.getSubnetMask())) {
            throw new ValidationException("Invalid subnet mask: " + route.getSubnetMask());
        }
        if (route.getNextHop() != null && !route.getNextHop().trim().isEmpty() && !IpUtils.isValidIPv4(route.getNextHop())) {
            throw new ValidationException("Invalid next hop IP: " + route.getNextHop());
        }

        route.setDeviceId(deviceId);
        route.setProjectId(device.getProjectId());
        route.setRouteType(RouteType.STATIC);
        if (route.getMetric() == null) {
            route.setMetric(1);
        }

        return routeRepository.save(route);
    }

    public void deleteRoute(Long routeId) {
        routeRepository.deleteById(routeId);
    }

    public Map<Long, RoutingTable> getProjectRoutingTables(Long projectId) {
        List<Device> devices = deviceRepository.findByProjectId(projectId);
        Map<Long, RoutingTable> projectTables = new HashMap<>();

        for (Device d : devices) {
            if (d.getType() == DeviceType.ROUTER) {
                projectTables.put(d.getId(), buildRoutingTable(d.getId()));
            }
        }

        return projectTables;
    }
}
