package com.nethub.service;

import com.nethub.entity.*;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemonstrationScenarioService {

    private final DemonstrationScenarioRepository scenarioRepository;
    private final ProjectRepository projectRepository;
    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;
    private final DemonstrationScenarioSeeder scenarioSeeder;

    public List<DemonstrationScenario> getAllScenarios() {
        return scenarioRepository.findAllByOrderByCategoryAscCodeAsc();
    }

    public List<DemonstrationScenario> getScenariosByCategory(String category) {
        return scenarioRepository.findByCategoryOrderByCodeAsc(category.toUpperCase());
    }

    public DemonstrationScenario getScenario(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demonstration scenario not found with id: " + id));
    }

    public DemonstrationScenario getScenario(String identifier) {
        try {
            Long id = Long.parseLong(identifier);
            return scenarioRepository.findById(id)
                    .orElseGet(() -> scenarioRepository.findByCode(identifier.toUpperCase())
                            .orElseThrow(() -> new ResourceNotFoundException("Demonstration scenario not found: " + identifier)));
        } catch (NumberFormatException e) {
            return scenarioRepository.findByCode(identifier.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Demonstration scenario not found: " + identifier));
        }
    }

    @Transactional
    public Project loadScenarioIntoProject(Long scenarioId, Long targetProjectId) {
        return loadScenarioIntoProject(String.valueOf(scenarioId), targetProjectId);
    }

    @Transactional
    public Project loadScenarioIntoProject(String scenarioIdentifier, Long targetProjectId) {
        DemonstrationScenario scenario = getScenario(scenarioIdentifier);
        Long templateProjId = scenario.getTemplateProjectId();

        Project templateProject = projectRepository.findById(templateProjId)
                .orElseThrow(() -> new ResourceNotFoundException("Template project not found: " + templateProjId));

        Project targetProject;
        if (targetProjectId != null && targetProjectId > 0) {
            targetProject = projectRepository.findById(targetProjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target project not found: " + targetProjectId));
            
            // Clean up existing components in target project
            clearProjectComponents(targetProjectId);
            targetProject.setName("[Demo " + scenario.getCode() + "] " + scenario.getName());
            targetProject.setDescription(scenario.getDescription());
            targetProject = projectRepository.save(targetProject);
        } else {
            targetProject = new Project();
            targetProject.setName("[Demo " + scenario.getCode() + "] " + scenario.getName());
            targetProject.setDescription(scenario.getDescription());
            targetProject = projectRepository.save(targetProject);
        }

        // Deep-clone devices, interfaces, connections, and routes
        cloneProjectEntities(templateProjId, targetProject.getId());
        return targetProject;
    }

    @Transactional
    public void resetScenario(Long scenarioId) {
        resetScenario(String.valueOf(scenarioId));
    }

    @Transactional
    public void resetScenario(String scenarioIdentifier) {
        DemonstrationScenario scenario = getScenario(scenarioIdentifier);
        // Delete current scenario record and re-seed
        if (scenario.getTemplateProjectId() != null) {
            clearProjectComponents(scenario.getTemplateProjectId());
            projectRepository.deleteById(scenario.getTemplateProjectId());
        }
        scenarioRepository.delete(scenario);
        scenarioSeeder.seedAllScenarios();
    }

    @Transactional
    public void clearProjectComponents(Long projectId) {
        List<Device> devices = deviceRepository.findByProjectId(projectId);
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();

        // Remove connections
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        connectionRepository.deleteAll(connections);

        // Remove static routes
        List<Route> routes = routeRepository.findByProjectId(projectId);
        routeRepository.deleteAll(routes);

        // Remove interfaces
        if (!deviceIds.isEmpty()) {
            List<NetworkInterface> interfaces = interfaceRepository.findByDeviceIdIn(deviceIds);
            interfaceRepository.deleteAll(interfaces);
        }

        // Remove devices
        deviceRepository.deleteAll(devices);
    }

    private void cloneProjectEntities(Long sourceProjectId, Long targetProjectId) {
        List<Device> sourceDevices = deviceRepository.findByProjectId(sourceProjectId);
        List<Connection> sourceConnections = connectionRepository.findByProjectId(sourceProjectId);
        List<Route> sourceRoutes = routeRepository.findByProjectId(sourceProjectId);

        Map<Long, Long> deviceIdMap = new HashMap<>(); // oldDeviceId -> newDeviceId
        Map<Long, Long> interfaceIdMap = new HashMap<>(); // oldInterfaceId -> newInterfaceId

        // 1. Clone Devices
        for (Device srcDev : sourceDevices) {
            Device newDev = Device.builder()
                    .projectId(targetProjectId)
                    .name(srcDev.getName())
                    .hostname(srcDev.getHostname())
                    .type(srcDev.getType())
                    .status(srcDev.getStatus())
                    .positionX(srcDev.getPositionX())
                    .positionY(srcDev.getPositionY())
                    .defaultGateway(srcDev.getDefaultGateway())
                    .primaryDns(srcDev.getPrimaryDns())
                    .secondaryDns(srcDev.getSecondaryDns())
                    .ssid(srcDev.getSsid())
                    .wirelessChannel(srcDev.getWirelessChannel())
                    .wirelessSecurity(srcDev.getWirelessSecurity())
                    .wirelessKey(srcDev.getWirelessKey())
                    .routingProtocol(srcDev.getRoutingProtocol())
                    .asNumber(srcDev.getAsNumber())
                    .build();

            newDev = deviceRepository.save(newDev);
            deviceIdMap.put(srcDev.getId(), newDev.getId());

            // 2. Clone Interfaces for this device
            List<NetworkInterface> srcIntfs = interfaceRepository.findByDeviceId(srcDev.getId());
            for (NetworkInterface srcIntf : srcIntfs) {
                NetworkInterface newIntf = NetworkInterface.builder()
                        .deviceId(newDev.getId())
                        .name(srcIntf.getName())
                        .macAddress(srcIntf.getMacAddress())
                        .ipAddress(srcIntf.getIpAddress())
                        .subnetMask(srcIntf.getSubnetMask())
                        .gateway(srcIntf.getGateway())
                        .dnsServer(srcIntf.getDnsServer())
                        .status(srcIntf.getStatus())
                        .adminStatus(srcIntf.getAdminStatus())
                        .mtu(srcIntf.getMtu())
                        .speed(srcIntf.getSpeed())
                        .duplex(srcIntf.getDuplex())
                        .vlanId(srcIntf.getVlanId())
                        .portMode(srcIntf.getPortMode())
                        .allowedVlans(srcIntf.getAllowedVlans())
                        .build();

                newIntf = interfaceRepository.save(newIntf);
                interfaceIdMap.put(srcIntf.getId(), newIntf.getId());
            }
        }

        // 3. Clone Connections using new interface IDs
        for (Connection srcConn : sourceConnections) {
            Long newSrcIntfId = interfaceIdMap.get(srcConn.getSourceInterfaceId());
            Long newDstIntfId = interfaceIdMap.get(srcConn.getDestinationInterfaceId());

            if (newSrcIntfId != null && newDstIntfId != null) {
                Connection newConn = new Connection();
                newConn.setProjectId(targetProjectId);
                newConn.setSourceInterfaceId(newSrcIntfId);
                newConn.setDestinationInterfaceId(newDstIntfId);
                newConn.setStatus(srcConn.getStatus());
                connectionRepository.save(newConn);
            }
        }

        // 4. Clone Routes using new device IDs
        for (Route srcRoute : sourceRoutes) {
            Long newDevId = deviceIdMap.get(srcRoute.getDeviceId());
            if (newDevId != null) {
                Route newRoute = Route.builder()
                        .projectId(targetProjectId)
                        .deviceId(newDevId)
                        .destinationNetwork(srcRoute.getDestinationNetwork())
                        .subnetMask(srcRoute.getSubnetMask())
                        .nextHop(srcRoute.getNextHop())
                        .outgoingInterface(srcRoute.getOutgoingInterface())
                        .routeType(srcRoute.getRouteType())
                        .metric(srcRoute.getMetric())
                        .build();
                routeRepository.save(newRoute);
            }
        }
    }
}
