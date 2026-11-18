package com.nethub.engine.simulation;

import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.Route;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.model.SimulationEvent;
import com.nethub.service.DeviceStateManager;
import lombok.Getter;

import java.util.*;

@Getter
public class SimContext {
    private final Long projectId;
    private final List<Device> devices;
    private final List<NetworkInterface> interfaces;
    private final List<Connection> connections;
    private final List<Route> staticRoutes;
    private final DeviceStateManager stateManager;

    private final Map<Long, Device> deviceById = new HashMap<>();
    private final Map<String, Device> deviceByName = new HashMap<>();
    private final Map<Long, NetworkInterface> interfaceById = new HashMap<>();
    private final Map<String, NetworkInterface> interfaceByMac = new HashMap<>();
    private final Map<String, NetworkInterface> interfaceByIp = new HashMap<>();
    private final Map<Long, List<NetworkInterface>> interfacesByDevice = new HashMap<>();

    private final List<SimulationEvent> events = new ArrayList<>();
    private int stepCounter = 0;

    public SimContext(Long projectId,
                      List<Device> devices,
                      List<NetworkInterface> interfaces,
                      List<Connection> connections,
                      List<Route> staticRoutes,
                      DeviceStateManager stateManager) {
        this.projectId = projectId;
        this.devices = devices;
        this.interfaces = interfaces;
        this.connections = connections;
        this.staticRoutes = staticRoutes;
        this.stateManager = stateManager;

        for (Device d : devices) {
            deviceById.put(d.getId(), d);
            deviceByName.put(d.getName(), d);
        }

        for (NetworkInterface ni : interfaces) {
            interfaceById.put(ni.getId(), ni);
            if (ni.getMacAddress() != null) {
                interfaceByMac.put(ni.getMacAddress().toUpperCase().trim(), ni);
            }
            if (ni.getIpAddress() != null && !ni.getIpAddress().trim().isEmpty()) {
                interfaceByIp.put(ni.getIpAddress().trim(), ni);
            }
            interfacesByDevice.computeIfAbsent(ni.getDeviceId(), k -> new ArrayList<>()).add(ni);
        }
    }

    public void addEvent(SimulationEvent event) {
        stepCounter++;
        event.setStepIndex(stepCounter);
        events.add(event);
    }

    public NetworkInterface findPeerInterface(Long interfaceId) {
        for (Connection c : connections) {
            if (c.getStatus() == ConnectionStatus.ACTIVE) {
                if (c.getSourceInterfaceId().equals(interfaceId)) {
                    return interfaceById.get(c.getDestinationInterfaceId());
                } else if (c.getDestinationInterfaceId().equals(interfaceId)) {
                    return interfaceById.get(c.getSourceInterfaceId());
                }
            }
        }
        return null;
    }

    public boolean isInterfaceOperationallyUp(NetworkInterface ni) {
        if (ni == null) return false;
        Device dev = deviceById.get(ni.getDeviceId());
        if (dev == null || dev.getStatus() != DeviceStatus.ONLINE) return false;

        InterfaceStatus admin = ni.getAdminStatus() != null ? ni.getAdminStatus() : ni.getStatus();
        if (admin != InterfaceStatus.UP) return false;

        NetworkInterface peer = findPeerInterface(ni.getId());
        if (peer == null) return false;

        Device peerDev = deviceById.get(peer.getDeviceId());
        if (peerDev == null || peerDev.getStatus() != DeviceStatus.ONLINE) return false;

        InterfaceStatus peerAdmin = peer.getAdminStatus() != null ? peer.getAdminStatus() : peer.getStatus();
        return peerAdmin == InterfaceStatus.UP;
    }

    public List<NetworkInterface> getDeviceInterfaces(Long deviceId) {
        return interfacesByDevice.getOrDefault(deviceId, Collections.emptyList());
    }

    public Device getDevice(Long deviceId) {
        return deviceById.get(deviceId);
    }

    public NetworkInterface getInterface(Long interfaceId) {
        return interfaceById.get(interfaceId);
    }
}

