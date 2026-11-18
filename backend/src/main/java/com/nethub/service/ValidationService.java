package com.nethub.service;

import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.Route;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.ProjectRepository;
import com.nethub.repository.RouteRepository;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ValidationService {
    private final ProjectRepository projectRepository;
    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;

    public Map<String, Object> validateProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        List<Device> devices = deviceRepository.findByProjectId(projectId);
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();
        List<NetworkInterface> interfaces = deviceIds.isEmpty() ? new ArrayList<>() : interfaceRepository.findByDeviceIdIn(deviceIds);
        List<Connection> connections = connectionRepository.findByProjectId(projectId);
        List<Route> staticRoutes = routeRepository.findByProjectId(projectId);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<Long, Device> deviceMap = new HashMap<>();
        for (Device d : devices) {
            deviceMap.put(d.getId(), d);
            if (d.getName() == null || d.getName().trim().isEmpty()) {
                errors.add("Device ID " + d.getId() + " is missing a name.");
            }
        }

        Set<String> macs = new HashSet<>();
        Set<String> ips = new HashSet<>();
        Map<Long, List<NetworkInterface>> deviceInterfaceMap = new HashMap<>();

        for (NetworkInterface ni : interfaces) {
            deviceInterfaceMap.computeIfAbsent(ni.getDeviceId(), k -> new ArrayList<>()).add(ni);

            // MAC validation
            if (ni.getMacAddress() == null || ni.getMacAddress().trim().isEmpty()) {
                errors.add("Interface " + ni.getName() + " on device " + getDeviceName(ni.getDeviceId(), deviceMap) + " is missing a MAC address.");
            } else if (!macs.add(ni.getMacAddress().toUpperCase().trim())) {
                errors.add("Duplicate MAC address found: " + ni.getMacAddress());
            }

            // IP & Subnet validation
            if (ni.getIpAddress() != null && !ni.getIpAddress().trim().isEmpty()) {
                String ip = ni.getIpAddress().trim();
                if (!IpUtils.isValidIPv4(ip)) {
                    errors.add("Invalid IPv4 address '" + ip + "' on interface " + ni.getName() + " of device " + getDeviceName(ni.getDeviceId(), deviceMap));
                } else if (!ips.add(ip)) {
                    errors.add("Duplicate IP address found in project: " + ip);
                }

                if (ni.getSubnetMask() != null && !ni.getSubnetMask().trim().isEmpty()) {
                    if (!IpUtils.isValidSubnetMask(ni.getSubnetMask().trim())) {
                        errors.add("Invalid subnet mask '" + ni.getSubnetMask() + "' on interface " + ni.getName() + " of device " + getDeviceName(ni.getDeviceId(), deviceMap));
                    }
                }

                if (ni.getGateway() != null && !ni.getGateway().trim().isEmpty()) {
                    String gw = ni.getGateway().trim();
                    if (!IpUtils.isValidIPv4(gw)) {
                        errors.add("Invalid Gateway IP '" + gw + "' on interface " + ni.getName() + " of device " + getDeviceName(ni.getDeviceId(), deviceMap));
                    } else if (ni.getSubnetMask() != null && IpUtils.isValidSubnetMask(ni.getSubnetMask().trim())) {
                        if (!IpUtils.isIpInSubnet(gw, ip, ni.getSubnetMask().trim())) {
                            errors.add("Gateway IP " + gw + " does not belong to interface subnet (" + IpUtils.getNetworkIp(ip, ni.getSubnetMask().trim()) + ") on device " + getDeviceName(ni.getDeviceId(), deviceMap));
                        }
                    }
                }
            }
        }

        // Router interface overlapping network check
        for (Device d : devices) {
            if (d.getType() == DeviceType.ROUTER) {
                List<NetworkInterface> rIntfs = deviceInterfaceMap.getOrDefault(d.getId(), Collections.emptyList());
                List<String> routerNets = new ArrayList<>();
                for (NetworkInterface rIntf : rIntfs) {
                    if (rIntf.getIpAddress() != null && rIntf.getSubnetMask() != null &&
                        IpUtils.isValidIPv4(rIntf.getIpAddress()) && IpUtils.isValidSubnetMask(rIntf.getSubnetMask())) {
                        String net = IpUtils.getNetworkIp(rIntf.getIpAddress(), rIntf.getSubnetMask());
                        if (routerNets.contains(net)) {
                            errors.add("Router " + d.getName() + " has multiple interfaces configured on the same subnet: " + net);
                        } else {
                            routerNets.add(net);
                        }
                    }
                }
            }
        }

        // Validate Static Routes
        for (Route r : staticRoutes) {
            if (!IpUtils.isValidIPv4(r.getDestinationNetwork())) {
                errors.add("Static route ID " + r.getId() + " on device " + getDeviceName(r.getDeviceId(), deviceMap) + " has invalid destination network: " + r.getDestinationNetwork());
            }
            if (!IpUtils.isValidSubnetMask(r.getSubnetMask())) {
                errors.add("Static route ID " + r.getId() + " on device " + getDeviceName(r.getDeviceId(), deviceMap) + " has invalid subnet mask: " + r.getSubnetMask());
            }
            if (r.getNextHop() != null && !r.getNextHop().trim().isEmpty() && !IpUtils.isValidIPv4(r.getNextHop())) {
                errors.add("Static route ID " + r.getId() + " on device " + getDeviceName(r.getDeviceId(), deviceMap) + " has invalid next hop IP: " + r.getNextHop());
            }
        }

        // Connection validation
        Set<Long> interfaceIds = new HashSet<>();
        for (NetworkInterface ni : interfaces) {
            interfaceIds.add(ni.getId());
        }

        for (Connection c : connections) {
            if (!interfaceIds.contains(c.getSourceInterfaceId())) {
                errors.add("Connection " + c.getId() + " references invalid source interface " + c.getSourceInterfaceId());
            }
            if (!interfaceIds.contains(c.getDestinationInterfaceId())) {
                errors.add("Connection " + c.getId() + " references invalid destination interface " + c.getDestinationInterfaceId());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isValid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);

        return result;
    }

    private String getDeviceName(Long deviceId, Map<Long, Device> deviceMap) {
        Device d = deviceMap.get(deviceId);
        return d != null ? d.getName() : "ID " + deviceId;
    }
}
