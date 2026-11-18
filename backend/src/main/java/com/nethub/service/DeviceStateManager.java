package com.nethub.service;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.engine.hardware.BridgeEngine;
import com.nethub.engine.hardware.HubEngine;
import com.nethub.engine.hardware.WirelessEngine;
import com.nethub.engine.security.AclEngine;
import com.nethub.engine.security.NatEngine;
import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.model.ArpEntry;
import com.nethub.model.MacTableEntry;
import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import com.nethub.model.state.DeviceInspectionDto;
import com.nethub.model.state.InterfaceOperationalDto;
import com.nethub.model.state.InterfaceStats;
import com.nethub.model.state.SocketDto;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceStateManager {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final ArpEngine arpEngine;
    private final SwitchingEngine switchingEngine;
    private final RoutingTableService routingTableService;
    private final HubEngine hubEngine;
    private final BridgeEngine bridgeEngine;
    private final WirelessEngine wirelessEngine;
    private final AclEngine aclEngine;
    private final NatEngine natEngine;

    // Interface statistics: interfaceId -> InterfaceStats
    private final Map<Long, InterfaceStats> statsMap = new ConcurrentHashMap<>();

    // Device active sockets: deviceId -> List<SocketDto>
    private final Map<Long, List<SocketDto>> socketsMap = new ConcurrentHashMap<>();

    @Autowired
    public DeviceStateManager(DeviceRepository deviceRepository,
                              NetworkInterfaceRepository interfaceRepository,
                              ConnectionRepository connectionRepository,
                              ArpEngine arpEngine,
                              SwitchingEngine switchingEngine,
                              RoutingTableService routingTableService,
                              HubEngine hubEngine,
                              BridgeEngine bridgeEngine,
                              WirelessEngine wirelessEngine,
                              AclEngine aclEngine,
                              NatEngine natEngine) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectionRepository = connectionRepository;
        this.arpEngine = arpEngine;
        this.switchingEngine = switchingEngine;
        this.routingTableService = routingTableService;
        this.hubEngine = hubEngine;
        this.bridgeEngine = bridgeEngine;
        this.wirelessEngine = wirelessEngine;
        this.aclEngine = aclEngine;
        this.natEngine = natEngine;
    }

    // Backward-compatible constructor for existing tests
    public DeviceStateManager(DeviceRepository deviceRepository,
                              NetworkInterfaceRepository interfaceRepository,
                              ConnectionRepository connectionRepository,
                              ArpEngine arpEngine,
                              SwitchingEngine switchingEngine,
                              RoutingTableService routingTableService) {
        this.deviceRepository = deviceRepository;
        this.interfaceRepository = interfaceRepository;
        this.connectionRepository = connectionRepository;
        this.arpEngine = arpEngine;
        this.switchingEngine = switchingEngine;
        this.routingTableService = routingTableService;
        this.hubEngine = new HubEngine();
        this.bridgeEngine = new BridgeEngine();
        this.wirelessEngine = new WirelessEngine();
        this.aclEngine = new AclEngine();
        this.natEngine = new NatEngine();
    }

    public InterfaceStats getInterfaceStats(Long interfaceId) {
        return statsMap.computeIfAbsent(interfaceId, k -> new InterfaceStats());
    }

    public void recordPacketSent(Long interfaceId, int bytes) {
        getInterfaceStats(interfaceId).incrementTx(bytes);
    }

    public void recordPacketReceived(Long interfaceId, int bytes) {
        getInterfaceStats(interfaceId).incrementRx(bytes);
    }

    public void recordPacketDropped(Long interfaceId) {
        getInterfaceStats(interfaceId).incrementDrop();
    }

    public void recordError(Long interfaceId) {
        getInterfaceStats(interfaceId).incrementError();
    }

    public DeviceInspectionDto getDeviceInspection(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device not found with id: " + deviceId));

        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(deviceId);
        List<Connection> connections = connectionRepository.findByProjectId(device.getProjectId());

        // Build interface operational DTOs
        List<InterfaceOperationalDto> operationalDtos = new ArrayList<>();
        for (NetworkInterface intf : interfaces) {
            operationalDtos.add(buildInterfaceOperationalState(device, intf, connections));
        }

        // ARP Cache
        List<ArpEntry> arpEntries = arpEngine.getArpTable(deviceId).getAllEntries();

        // MAC Address Table (Switches & Bridges)
        List<MacTableEntry> macEntries = Collections.emptyList();
        if (device.getType() == DeviceType.SWITCH) {
            macEntries = switchingEngine.getMacTable(deviceId).getAllEntries();
        } else if (device.getType() == DeviceType.BRIDGE) {
            macEntries = bridgeEngine.getBridgeTable(deviceId).getAllEntries();
        }

        // Routing Table (Routers, Gateways, PCs, Servers)
        List<RouteEntry> routes = Collections.emptyList();
        if (device.getType() == DeviceType.ROUTER || device.getType() == DeviceType.GATEWAY || device.getType() == DeviceType.PC || device.getType() == DeviceType.SERVER) {
            RoutingTable rt = routingTableService.buildRoutingTable(deviceId);
            if (rt != null) {
                routes = rt.getAllRoutes();
            }
        }

        // Active Sockets
        List<SocketDto> sockets = getDeviceSockets(device);

        // VLAN information for Switches
        Map<String, Object> vlanInfo = new HashMap<>();
        if (device.getType() == DeviceType.SWITCH) {
            Map<Integer, List<String>> vlanPortMap = new HashMap<>();
            for (NetworkInterface intf : interfaces) {
                int vlan = (intf.getVlanId() != null && intf.getVlanId() > 0) ? intf.getVlanId() : 1;
                vlanPortMap.computeIfAbsent(vlan, k -> new ArrayList<>()).add(intf.getName());
            }
            vlanInfo.put("vlanPortMap", vlanPortMap);
            vlanInfo.put("totalVlans", vlanPortMap.size());
        }

        // STP Info
        Map<String, Object> stpInfo = new HashMap<>();
        if (device.getType() == DeviceType.SWITCH) {
            stpInfo.put("enabled", true);
            stpInfo.put("protocol", "IEEE 802.1D Spanning Tree");
            stpInfo.put("bridgePriority", 32768);
            stpInfo.put("bridgeMac", interfaces.isEmpty() ? "00:00:00:00:00:00" : interfaces.get(0).getMacAddress());
            Map<String, String> portStpStates = new HashMap<>();
            for (NetworkInterface intf : interfaces) {
                portStpStates.put(intf.getName(), "FORWARDING");
            }
            stpInfo.put("portStates", portStpStates);
        }

        // Services (for Servers)
        Map<String, Object> services = new HashMap<>();
        if (device.getType() == DeviceType.SERVER) {
            services.put("http", Map.of("enabled", true, "port", 80, "name", "HTTP Web Server"));
            services.put("dns", Map.of("enabled", true, "port", 53, "name", "DNS Domain Resolution Service"));
            services.put("dhcp", Map.of("enabled", false, "port", 67, "name", "DHCP Dynamic Host Configuration"));
            services.put("ftp", Map.of("enabled", true, "port", 21, "name", "FTP File Transfer Protocol"));
            services.put("smtp", Map.of("enabled", true, "port", 25, "name", "SMTP Simple Mail Transfer Protocol"));
        }

        // Hardware details for Hub, Repeater, Bridge, AP, Modem, Gateway
        if (device.getType() == DeviceType.HUB) {
            services.put("hub", Map.of(
                    "layer", 1,
                    "mode", "Multiport Repeater",
                    "collisionDomain", "Single Shared Collision Domain",
                    "collisionCount", hubEngine.getCollisionCount(deviceId),
                    "signalsBroadcasted", hubEngine.getSignalCount(deviceId)
            ));
        } else if (device.getType() == DeviceType.REPEATER) {
            services.put("repeater", Map.of(
                    "layer", 1,
                    "function", "Signal Regeneration and Amplification",
                    "attenuationCompensation", "Full waveform recovery (NRZ/Manchester)"
            ));
        } else if (device.getType() == DeviceType.BRIDGE) {
            services.put("bridge", Map.of(
                    "layer", 2,
                    "function", "Segment Isolation and MAC Filtering",
                    "totalSegments", interfaces.size()
            ));
        } else if (device.getType() == DeviceType.ACCESS_POINT) {
            services.put("wireless", Map.of(
                    "ssid", device.getSsid() != null ? device.getSsid() : "NetHub-WiFi",
                    "channel", device.getWirelessChannel() != null ? device.getWirelessChannel() : 6,
                    "security", device.getWirelessSecurity() != null ? device.getWirelessSecurity() : "WPA2_PSK",
                    "associatedClients", wirelessEngine.getAssociatedClients(deviceId),
                    "mode", "Infrastructure (BSS)"
            ));
        } else if (device.getType() == DeviceType.MODEM) {
            services.put("modem", Map.of(
                    "modulation", "QAM-256 (Downstream) / QPSK (Upstream)",
                    "boundary", "LAN Digital <-> WAN Carrier Signal",
                    "carrierFrequency", "54-860 MHz"
            ));
        } else if (device.getType() == DeviceType.GATEWAY) {
            services.put("gateway", Map.of(
                    "layer", "Transport/Application Gateway",
                    "protocolTranslation", "IPv4 <-> Heterogeneous Network Translation"
            ));
        }

        // Security Info (ACL & NAT)
        if (device.getType() == DeviceType.ROUTER || device.getType() == DeviceType.GATEWAY) {
            services.put("acl", Map.of("totalRules", aclEngine.getRules(deviceId).size(), "rules", aclEngine.getRules(deviceId)));
            services.put("nat", Map.of("totalTranslations", natEngine.getTranslations(deviceId).size(), "table", natEngine.getTranslations(deviceId)));
        }

        return DeviceInspectionDto.builder()
                .deviceId(device.getId())
                .name(device.getName())
                .hostname(device.getHostname() != null ? device.getHostname() : device.getName())
                .type(device.getType())
                .status(device.getStatus())
                .defaultGateway(device.getDefaultGateway())
                .primaryDns(device.getPrimaryDns())
                .secondaryDns(device.getSecondaryDns())
                .interfaces(operationalDtos)
                .arpTable(arpEntries)
                .macTable(macEntries)
                .routingTable(routes)
                .vlanInfo(vlanInfo)
                .stpInfo(stpInfo)
                .activeSockets(sockets)
                .services(services)
                .build();
    }

    public InterfaceOperationalDto buildInterfaceOperationalState(Device device, NetworkInterface intf, List<Connection> connections) {
        InterfaceStatus adminStatus = intf.getAdminStatus() != null ? intf.getAdminStatus() : intf.getStatus();
        InterfaceStatus operationalStatus = InterfaceStatus.DOWN;

        Long connectedDeviceId = null;
        String connectedDeviceName = null;
        String connectedInterfaceName = null;

        if (device.getStatus() == DeviceStatus.ONLINE && adminStatus == InterfaceStatus.UP) {
            Connection conn = connections.stream()
                    .filter(c -> c.getStatus() == ConnectionStatus.ACTIVE &&
                            (c.getSourceInterfaceId().equals(intf.getId()) || c.getDestinationInterfaceId().equals(intf.getId())))
                    .findFirst()
                    .orElse(null);

            if (conn != null) {
                Long peerIntfId = conn.getSourceInterfaceId().equals(intf.getId())
                        ? conn.getDestinationInterfaceId()
                        : conn.getSourceInterfaceId();

                Optional<NetworkInterface> peerIntfOpt = interfaceRepository.findById(peerIntfId);
                if (peerIntfOpt.isPresent()) {
                    NetworkInterface peerIntf = peerIntfOpt.get();
                    connectedInterfaceName = peerIntf.getName();
                    connectedDeviceId = peerIntf.getDeviceId();

                    Optional<Device> peerDevOpt = deviceRepository.findById(peerIntf.getDeviceId());
                    if (peerDevOpt.isPresent()) {
                        Device peerDev = peerDevOpt.get();
                        connectedDeviceName = peerDev.getName();

                        InterfaceStatus peerAdmin = peerIntf.getAdminStatus() != null ? peerIntf.getAdminStatus() : peerIntf.getStatus();
                        if (peerDev.getStatus() == DeviceStatus.ONLINE && peerAdmin == InterfaceStatus.UP) {
                            operationalStatus = InterfaceStatus.UP;
                        }
                    }
                }
            }
        }

        return InterfaceOperationalDto.builder()
                .id(intf.getId())
                .deviceId(intf.getDeviceId())
                .name(intf.getName())
                .macAddress(intf.getMacAddress())
                .ipAddress(intf.getIpAddress())
                .subnetMask(intf.getSubnetMask())
                .gateway(intf.getGateway())
                .dnsServer(intf.getDnsServer())
                .adminStatus(adminStatus)
                .operationalStatus(operationalStatus)
                .mtu(intf.getMtu() != null ? intf.getMtu() : 1500)
                .speed(intf.getSpeed() != null ? intf.getSpeed() : "1Gbps")
                .duplex(intf.getDuplex() != null ? intf.getDuplex() : "FULL")
                .vlanId(intf.getVlanId() != null ? intf.getVlanId() : 1)
                .portMode(intf.getPortMode() != null ? intf.getPortMode() : "ACCESS")
                .allowedVlans(intf.getAllowedVlans() != null ? intf.getAllowedVlans() : "1")
                .connectedDeviceId(connectedDeviceId)
                .connectedDeviceName(connectedDeviceName)
                .connectedInterfaceName(connectedInterfaceName)
                .stats(getInterfaceStats(intf.getId()))
                .build();
    }

    private List<SocketDto> getDeviceSockets(Device device) {
        List<SocketDto> custom = socketsMap.get(device.getId());
        if (custom != null) {
            return custom;
        }

        List<SocketDto> defaults = new ArrayList<>();
        if (device.getType() == DeviceType.SERVER) {
            defaults.add(SocketDto.builder().protocol("TCP").localAddress("0.0.0.0").localPort(80).remoteAddress("0.0.0.0").remotePort(0).state("LISTEN").serviceName("HTTP Web Server").build());
            defaults.add(SocketDto.builder().protocol("UDP").localAddress("0.0.0.0").localPort(53).remoteAddress("0.0.0.0").remotePort(0).state("LISTEN").serviceName("DNS Server").build());
            defaults.add(SocketDto.builder().protocol("TCP").localAddress("0.0.0.0").localPort(21).remoteAddress("0.0.0.0").remotePort(0).state("LISTEN").serviceName("FTP File Server").build());
            defaults.add(SocketDto.builder().protocol("TCP").localAddress("0.0.0.0").localPort(25).remoteAddress("0.0.0.0").remotePort(0).state("LISTEN").serviceName("SMTP Mail Server").build());
        } else if (device.getType() == DeviceType.PC) {
            defaults.add(SocketDto.builder().protocol("UDP").localAddress("0.0.0.0").localPort(68).remoteAddress("0.0.0.0").remotePort(0).state("BOUND").serviceName("DHCP Client").build());
        }
        return defaults;
    }

    public void registerSocket(Long deviceId, SocketDto socket) {
        socketsMap.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(socket);
    }

    public void updateSocketState(Long deviceId, int localPort, String state) {
        List<SocketDto> list = socketsMap.get(deviceId);
        if (list != null) {
            for (SocketDto s : list) {
                if (s.getLocalPort() == localPort) {
                    s.setState(state);
                }
            }
        }
    }

    public void resetAllStats() {
        statsMap.clear();
        socketsMap.clear();
        hubEngine.resetAll();
        bridgeEngine.resetAll();
        wirelessEngine.resetAll();
        aclEngine.resetAll();
        natEngine.resetAll();
    }
}
