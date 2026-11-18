package com.nethub.service;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.model.*;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NetworkSimulationService {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RoutingTableService routingTableService;
    private final RoutingEngine routingEngine;
    private final SwitchingEngine switchingEngine;
    private final ArpEngine arpEngine;
    private final NetworkPathFinder pathFinder;

    public SimulationResult simulateTransmission(Long projectId, Long sourceDeviceId, String destinationIp) {
        List<SimulationEvent> events = new ArrayList<>();

        // 1. Validate Source Device
        Device sourceDevice = deviceRepository.findById(sourceDeviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source device not found with id: " + sourceDeviceId));

        if (sourceDevice.getStatus() != DeviceStatus.ONLINE) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(sourceDevice.getName())
                    .description("Source device is OFFLINE")
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("SOURCE_DEVICE_OFFLINE")
                    .errorMessage("Source device " + sourceDevice.getName() + " is offline.")
                    .events(events)
                    .build();
        }

        events.add(SimulationEvent.builder()
                .type("SOURCE_VALIDATED")
                .device(sourceDevice.getName())
                .description("Validated source device " + sourceDevice.getName() + " (ONLINE)")
                .build());

        // 2. Validate Source Interface
        List<NetworkInterface> sourceInterfaces = interfaceRepository.findByDeviceId(sourceDeviceId);
        NetworkInterface sourceIntf = sourceInterfaces.stream()
                .filter(i -> i.getStatus() == InterfaceStatus.UP && i.getIpAddress() != null && !i.getIpAddress().isEmpty())
                .findFirst()
                .orElse(null);

        if (sourceIntf == null) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(sourceDevice.getName())
                    .description("No active network interface with configured IP address found")
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("INTERFACE_DOWN")
                    .errorMessage("Source device has no active interface with a configured IP.")
                    .events(events)
                    .build();
        }

        String sourceIp = sourceIntf.getIpAddress();
        String sourceMask = sourceIntf.getSubnetMask();

        if (!IpUtils.isValidIPv4(destinationIp)) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(sourceDevice.getName())
                    .description("Invalid destination IP format: " + destinationIp)
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("INVALID_DESTINATION_IP")
                    .errorMessage("Invalid destination IP address format.")
                    .events(events)
                    .build();
        }

        // 3. Locate Target Device in Topology
        List<Device> allDevices = deviceRepository.findByProjectId(projectId);
        List<Long> deviceIds = allDevices.stream().map(Device::getId).toList();
        List<NetworkInterface> allInterfaces = deviceIds.isEmpty() ? new ArrayList<>() : interfaceRepository.findByDeviceIdIn(deviceIds);

        Map<Long, Device> deviceMap = new HashMap<>();
        for (Device d : allDevices) deviceMap.put(d.getId(), d);

        NetworkInterface targetIntf = null;
        Device targetDevice = null;
        for (NetworkInterface ni : allInterfaces) {
            if (destinationIp.trim().equals(ni.getIpAddress())) {
                targetIntf = ni;
                targetDevice = deviceMap.get(ni.getDeviceId());
                break;
            }
        }

        if (targetDevice == null) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(sourceDevice.getName())
                    .description("Destination IP " + destinationIp + " is unreachable (no matching interface in project)")
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("DESTINATION_UNREACHABLE")
                    .errorMessage("Destination IP " + destinationIp + " not found on any network interface.")
                    .events(events)
                    .build();
        }

        if (targetDevice.getStatus() != DeviceStatus.ONLINE) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(targetDevice.getName())
                    .description("Destination device " + targetDevice.getName() + " is OFFLINE")
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("DESTINATION_UNREACHABLE")
                    .errorMessage("Target device " + targetDevice.getName() + " is offline.")
                    .events(events)
                    .build();
        }

        events.add(SimulationEvent.builder()
                .type("DESTINATION_IDENTIFIED")
                .device(targetDevice.getName())
                .description("Destination identified as " + targetDevice.getName() + " (" + destinationIp + ")")
                .build());

        // 4. Calculate Network Path
        PathResult pathRes = pathFinder.findPath(projectId, sourceDeviceId, destinationIp);
        if (!pathRes.isReachable()) {
            events.add(SimulationEvent.builder()
                    .type("ERROR")
                    .device(sourceDevice.getName())
                    .description("Path calculation failed: " + pathRes.getReason())
                    .build());
            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("NO_NETWORK_PATH")
                    .errorMessage(pathRes.getReason())
                    .events(events)
                    .build();
        }

        // 5. Determine Local vs Remote
        boolean isLocal = IpUtils.isLocal(sourceIp, sourceMask, destinationIp);

        if (isLocal) {
            events.add(SimulationEvent.builder()
                    .type("SUBNET_CHECK")
                    .device(sourceDevice.getName())
                    .description("Destination " + destinationIp + " is in local subnet (" + IpUtils.getNetworkIp(sourceIp, sourceMask) + "/" + IpUtils.maskToCidr(sourceMask) + ")")
                    .build());

            // ARP Resolution for Destination IP
            events.add(SimulationEvent.builder()
                    .type("ARP_REQUEST")
                    .device(sourceDevice.getName())
                    .description("Broadcasting ARP Request: Who has " + destinationIp + "? Tell " + sourceIp)
                    .details(Map.of("targetIp", destinationIp, "destinationMac", EthernetFrame.BROADCAST_MAC))
                    .build());

            arpEngine.updateArpCache(sourceDeviceId, destinationIp, targetIntf.getMacAddress());
            arpEngine.updateArpCache(targetDevice.getId(), sourceIp, sourceIntf.getMacAddress());

            events.add(SimulationEvent.builder()
                    .type("ARP_REPLY")
                    .device(targetDevice.getName())
                    .description("ARP Reply from " + targetDevice.getName() + ": " + destinationIp + " is at " + targetIntf.getMacAddress())
                    .details(Map.of("ip", destinationIp, "mac", targetIntf.getMacAddress()))
                    .build());

            // Create Ethernet Frame
            EthernetFrame frame = EthernetFrame.builder()
                    .sourceMac(sourceIntf.getMacAddress())
                    .destinationMac(targetIntf.getMacAddress())
                    .etherType(EthernetFrame.ETHERTYPE_IPV4)
                    .payload(IpPacket.builder()
                            .sourceIp(sourceIp)
                            .destinationIp(destinationIp)
                            .ttl(64)
                            .protocol(IpPacket.PROTOCOL_RAW)
                            .build())
                    .build();

            events.add(SimulationEvent.builder()
                    .type("FRAME_CREATED")
                    .device(sourceDevice.getName())
                    .description("Ethernet Frame created [SrcMAC: " + sourceIntf.getMacAddress() + " -> DstMAC: " + targetIntf.getMacAddress() + "]")
                    .build());

            // Switch forwarding along path
            simulateL2Forwarding(pathRes.getPath(), allDevices, events, frame);

            events.add(SimulationEvent.builder()
                    .type("DELIVERED")
                    .device(targetDevice.getName())
                    .description("Frame successfully delivered to " + targetDevice.getName() + " (" + destinationIp + ")")
                    .build());

            return SimulationResult.builder()
                    .success(true)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .path(pathRes.getPath())
                    .events(events)
                    .build();
        } else {
            // Remote Subnet Transmission
            events.add(SimulationEvent.builder()
                    .type("SUBNET_CHECK")
                    .device(sourceDevice.getName())
                    .description("Destination " + destinationIp + " is in a REMOTE subnet. Routing required via Default Gateway.")
                    .build());

            String gatewayIp = sourceIntf.getGateway();
            if (gatewayIp == null || gatewayIp.trim().isEmpty()) {
                events.add(SimulationEvent.builder()
                        .type("ERROR")
                        .device(sourceDevice.getName())
                        .description("No default gateway configured on interface " + sourceIntf.getName())
                        .build());
                return SimulationResult.builder()
                        .success(false)
                        .source(sourceDevice.getName())
                        .destination(destinationIp)
                        .errorCode("NO_DEFAULT_GATEWAY")
                        .errorMessage("No default gateway configured on interface " + sourceIntf.getName())
                        .events(events)
                        .build();
            }

            // Find Gateway Interface & Device
            NetworkInterface gatewayIntf = null;
            Device gatewayDevice = null;
            for (NetworkInterface ni : allInterfaces) {
                if (gatewayIp.trim().equals(ni.getIpAddress())) {
                    gatewayIntf = ni;
                    gatewayDevice = deviceMap.get(ni.getDeviceId());
                    break;
                }
            }

            if (gatewayDevice == null || gatewayIntf == null) {
                events.add(SimulationEvent.builder()
                        .type("ERROR")
                        .device(sourceDevice.getName())
                        .description("Gateway IP " + gatewayIp + " is unreachable in topology")
                        .build());
                return SimulationResult.builder()
                        .success(false)
                        .source(sourceDevice.getName())
                        .destination(destinationIp)
                        .errorCode("ARP_RESOLUTION_FAILED")
                        .errorMessage("Unable to resolve MAC address for default gateway " + gatewayIp)
                        .events(events)
                        .build();
            }

            // ARP for Default Gateway
            events.add(SimulationEvent.builder()
                    .type("ARP_REQUEST")
                    .device(sourceDevice.getName())
                    .description("Broadcasting ARP Request for Gateway: Who has " + gatewayIp + "? Tell " + sourceIp)
                    .build());

            arpEngine.updateArpCache(sourceDeviceId, gatewayIp, gatewayIntf.getMacAddress());

            events.add(SimulationEvent.builder()
                    .type("ARP_REPLY")
                    .device(gatewayDevice.getName())
                    .description("Gateway ARP Reply: " + gatewayIp + " is at " + gatewayIntf.getMacAddress())
                    .build());

            // Create initial frame to Gateway
            EthernetFrame frame = EthernetFrame.builder()
                    .sourceMac(sourceIntf.getMacAddress())
                    .destinationMac(gatewayIntf.getMacAddress())
                    .etherType(EthernetFrame.ETHERTYPE_IPV4)
                    .payload(IpPacket.builder()
                            .sourceIp(sourceIp)
                            .destinationIp(destinationIp)
                            .ttl(64)
                            .protocol(IpPacket.PROTOCOL_RAW)
                            .build())
                    .build();

            events.add(SimulationEvent.builder()
                    .type("FRAME_CREATED")
                    .device(sourceDevice.getName())
                    .description("Frame created for Gateway [SrcMAC: " + sourceIntf.getMacAddress() + " -> DstMAC: " + gatewayIntf.getMacAddress() + "]")
                    .build());

            // Router Route Lookup
            RoutingTable routerTable = routingTableService.buildRoutingTable(gatewayDevice.getId());
            Optional<RouteEntry> routeOpt = routingEngine.lookupBestRoute(routerTable, destinationIp);

            if (routeOpt.isEmpty()) {
                events.add(SimulationEvent.builder()
                        .type("ERROR")
                        .device(gatewayDevice.getName())
                        .description("No route to destination network " + destinationIp + " in routing table")
                        .build());
                return SimulationResult.builder()
                        .success(false)
                        .source(sourceDevice.getName())
                        .destination(destinationIp)
                        .errorCode("NO_ROUTE")
                        .errorMessage("Router " + gatewayDevice.getName() + " has no route to " + destinationIp)
                        .events(events)
                        .build();
            }

            RouteEntry route = routeOpt.get();
            events.add(SimulationEvent.builder()
                    .type("ROUTE_LOOKUP")
                    .device(gatewayDevice.getName())
                    .description("Route lookup success: Matched " + route.getDestinationNetwork() + "/" + route.getPrefixLength() + " via interface " + route.getOutgoingInterface() + " (Type: " + route.getRouteType() + ")")
                    .details(Map.of("destNet", route.getDestinationNetwork(), "outInterface", route.getOutgoingInterface(), "type", route.getRouteType().toString()))
                    .build());

            // Router outgoing interface MAC
            List<NetworkInterface> routerIntfs = interfaceRepository.findByDeviceId(gatewayDevice.getId());
            NetworkInterface routerOutIntf = routerIntfs.stream()
                    .filter(i -> i.getName().equalsIgnoreCase(route.getOutgoingInterface()))
                    .findFirst()
                    .orElse(gatewayIntf);

            // Re-encapsulation at Router: SrcMAC = routerOutIntf.mac, DstMAC = targetIntf.mac
            frame.setSourceMac(routerOutIntf.getMacAddress());
            frame.setDestinationMac(targetIntf.getMacAddress());

            events.add(SimulationEvent.builder()
                    .type("FRAME_REENCAPSULATION")
                    .device(gatewayDevice.getName())
                    .description("Router re-encapsulated frame: [SrcMAC: " + routerOutIntf.getMacAddress() + " -> DstMAC: " + targetIntf.getMacAddress() + "]")
                    .build());

            // Forward across switches to target
            simulateL2Forwarding(pathRes.getPath(), allDevices, events, frame);

            events.add(SimulationEvent.builder()
                    .type("DELIVERED")
                    .device(targetDevice.getName())
                    .description("Frame successfully delivered across router to " + targetDevice.getName() + " (" + destinationIp + ")")
                    .build());

            return SimulationResult.builder()
                    .success(true)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .path(pathRes.getPath())
                    .events(events)
                    .build();
        }
    }

    private void simulateL2Forwarding(List<String> pathDeviceNames, List<Device> allDevices, List<SimulationEvent> events, EthernetFrame frame) {
        Map<String, Device> nameMap = new HashMap<>();
        for (Device d : allDevices) nameMap.put(d.getName(), d);

        for (String devName : pathDeviceNames) {
            Device dev = nameMap.get(devName);
            if (dev != null && dev.getType() == DeviceType.SWITCH) {
                // MAC Learning & Forwarding simulation on Switch
                MacAddressTable macTable = switchingEngine.getMacTable(dev.getId());
                macTable.learn(frame.getSourceMac(), "port1");

                events.add(SimulationEvent.builder()
                        .type("FRAME_FORWARD")
                        .device(dev.getName())
                        .description("Switch " + dev.getName() + " learned " + frame.getSourceMac() + " and forwarded frame to " + frame.getDestinationMac())
                        .details(Map.of("switch", dev.getName(), "srcMac", frame.getSourceMac(), "dstMac", frame.getDestinationMac()))
                        .build());
            }
        }
    }
}
