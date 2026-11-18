package com.nethub.service;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.engine.simulation.DiscreteNetworkSimulator;
import com.nethub.engine.simulation.SimContext;
import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.Route;
import com.nethub.model.SimulationResult;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NetworkSimulationService {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RoutingTableService routingTableService;
    private final RoutingEngine routingEngine;
    private final SwitchingEngine switchingEngine;
    private final ArpEngine arpEngine;
    private final NetworkPathFinder pathFinder;
    private final RouteRepository routeRepository;
    private final DeviceStateManager stateManager;
    private final DiscreteNetworkSimulator discreteNetworkSimulator;

    @Autowired
    public NetworkSimulationService(DeviceRepository deviceRepository,
                                    NetworkInterfaceRepository interfaceRepository,
                                    ConnectionRepository connectionRepository,
                                    RoutingTableService routingTableService,
                                    RoutingEngine routingEngine,
                                    SwitchingEngine switchingEngine,
                                    ArpEngine arpEngine,
                                    NetworkPathFinder pathFinder,
                                    RouteRepository routeRepository,
                                    DeviceStateManager stateManager,
                                    DiscreteNetworkSimulator discreteNetworkSimulator) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectionRepository = connectionRepository;
        this.routingTableService = routingTableService;
        this.routingEngine = routingEngine;
        this.switchingEngine = switchingEngine;
        this.arpEngine = arpEngine;
        this.pathFinder = pathFinder;
        this.routeRepository = routeRepository;
        this.stateManager = stateManager;
        this.discreteNetworkSimulator = discreteNetworkSimulator;
    }

    // Backward-compatible 8-argument constructor for existing unit and integration tests
    public NetworkSimulationService(DeviceRepository deviceRepository,
                                    NetworkInterfaceRepository interfaceRepository,
                                    ConnectionRepository connectionRepository,
                                    RoutingTableService routingTableService,
                                    RoutingEngine routingEngine,
                                    SwitchingEngine switchingEngine,
                                    ArpEngine arpEngine,
                                    NetworkPathFinder pathFinder) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectionRepository = connectionRepository;
        this.routingTableService = routingTableService;
        this.routingEngine = routingEngine;
        this.switchingEngine = switchingEngine;
        this.arpEngine = arpEngine;
        this.pathFinder = pathFinder;
        this.routeRepository = null;
        this.stateManager = new DeviceStateManager(deviceRepository, interfaceRepository, connectionRepository, arpEngine, switchingEngine, routingTableService);
        this.discreteNetworkSimulator = new DiscreteNetworkSimulator(arpEngine, switchingEngine, routingEngine, routingTableService, this.stateManager);
    }

    public SimulationResult simulateTransmission(Long projectId, Long sourceDeviceId, String destinationIp) {
        return simulateTransmission(projectId, sourceDeviceId, destinationIp, "ICMP", 64);
    }

    public SimulationResult simulateTransmission(Long projectId, Long sourceDeviceId, String destinationIp, String protocolType, int initialTtl) {
        List<Device> devices = deviceRepository.findByProjectId(projectId);
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();
        List<NetworkInterface> interfaces = deviceIds.isEmpty() ? Collections.emptyList() : interfaceRepository.findByDeviceIdIn(deviceIds);
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        List<Route> staticRoutes = (routeRepository != null) ? routeRepository.findByProjectId(projectId) : Collections.emptyList();

        SimContext ctx = new SimContext(projectId, devices, interfaces, connections, staticRoutes, stateManager);
        return discreteNetworkSimulator.runSimulation(ctx, sourceDeviceId, destinationIp, protocolType, initialTtl);
    }
}
