package com.nethub.service;

import com.nethub.entity.*;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.entity.enums.RouteType;
import com.nethub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemonstrationScenarioSeeder {

    private final DemonstrationScenarioRepository scenarioRepository;
    private final ProjectRepository projectRepository;
    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            seedAllScenarios();
        } catch (Exception e) {
            log.error("Error seeding demonstration scenarios", e);
        }
    }

    @Transactional
    public void seedAllScenarios() {
        log.info("Starting demonstration scenarios check and seed...");

        // Valid Scenarios
        seedV1_BasicHub();
        seedV2_Repeater();
        seedV3_Bridge();
        seedV4_SwitchedLan();
        seedV5_BasicRouter();
        seedV6_MultiRouter();
        seedV7_VlanNetwork();
        seedV8_WirelessInfrastructure();
        seedV9_WirelessAdhoc();
        seedV10_DhcpDnsNetwork();
        seedV11_TcpUdpClientServer();
        seedV12_HttpHttpsNetwork();
        seedV13_RoutingProtocols();
        seedV14_ComparativeTopology();

        // Invalid Scenarios
        seedI1_DuplicateIp();
        seedI2_IncorrectSubnetMask();
        seedI3_InvalidDefaultGateway();
        seedI4_MissingDefaultGateway();
        seedI5_MissingRoutingTableEntry();
        seedI6_DisconnectedNetworkLink();
        seedI7_InterfaceDisabled();
        seedI8_IncorrectArpResolution();
        seedI9_VlanMisconfiguration();
        seedI10_RoutingLoop();
        seedI11_DhcpFailure();
        seedI12_DnsFailure();
        seedI13_AclFirewallBlocking();
        seedI14_WirelessAuthFailure();
        seedI15_MtuMismatch();
        seedI16_DuplicateMac();
        seedI17_NoInternetRoute();
        seedI18_RoutingProtocolMisconfig();

        log.info("Demonstration scenarios seeding complete. Total scenarios: {}", scenarioRepository.count());
    }

    // ==========================================
    // VALID SCENARIOS (V1 - V14)
    // ==========================================

    private void seedV1_BasicHub() {
        if (scenarioRepository.existsByCode("V1")) return;
        Project p = createProject("[Demo V1] Basic Hub Network", "Multiport Layer 1 Hub broadcasting frames across a shared collision domain.");

        Device hub1 = createDevice(p.getId(), "Hub1", DeviceType.HUB, 350.0, 200.0);
        NetworkInterface hp1 = addInterface(hub1, "port1", null, null, null, "00:H1:01:00:00:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface hp2 = addInterface(hub1, "port2", null, null, null, "00:H1:01:00:00:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface hp3 = addInterface(hub1, "port3", null, null, null, "00:H1:01:00:00:03", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 100.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:A1:01:01:01:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 150.0, 300.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", null, "00:A1:01:01:01:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 550.0, 200.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.1.12", "255.255.255.0", null, "00:A1:01:01:01:03", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, hp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, hp2, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc3Intf, hp3, ConnectionStatus.ACTIVE);

        saveScenario("V1", "Basic Hub Network", "VALID",
                "Demonstrates a Layer 1 multiport repeater (Hub) connecting 3 PCs on a shared medium. Hub floods bits unconditionally out all active ports.",
                "BEGINNER", 4,
                "Layer 1 repeating, Hub broadcast flooding, shared collision domain, half-duplex medium",
                "Frames transmitted by PC1 are repeated to both PC2 and PC3, with PC2 accepting the unicast frame matching its MAC.",
                null, p.getId());
    }

    private void seedV2_Repeater() {
        if (scenarioRepository.existsByCode("V2")) return;
        Project p = createProject("[Demo V2] Repeater Network", "Layer 1 Repeater regenerating physical signals across extended network segments.");

        Device rep1 = createDevice(p.getId(), "Repeater1", DeviceType.REPEATER, 350.0, 200.0);
        NetworkInterface rp1 = addInterface(rep1, "port1", null, null, null, "00:R1:02:00:00:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rp2 = addInterface(rep1, "port2", null, null, null, "00:R1:02:00:00:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:A2:01:01:01:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 150.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", null, "00:A2:01:01:01:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 550.0, 120.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.1.20", "255.255.255.0", null, "00:A2:01:01:01:03", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc4 = createDevice(p.getId(), "PC4", DeviceType.PC, 550.0, 280.0);
        NetworkInterface pc4Intf = addInterface(pc4, "eth0", "192.168.1.21", "255.255.255.0", null, "00:A2:01:01:01:04", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc3Intf, rp2, ConnectionStatus.ACTIVE);

        saveScenario("V2", "Repeater Network", "VALID",
                "Demonstrates signal amplification and waveform restoration over long cable distances without Layer 2 header modification.",
                "BEGINNER", 5,
                "Layer 1 signal amplification, waveform regeneration, jitter elimination, cable distance extension",
                "Signals between PC1 and PC3 are cleanly regenerated and propagated across segments.",
                null, p.getId());
    }

    private void seedV3_Bridge() {
        if (scenarioRepository.existsByCode("V3")) return;
        Project p = createProject("[Demo V3] Bridge Network", "Layer 2 Bridge segmenting collision domains with MAC learning and filtering.");

        Device bridge = createDevice(p.getId(), "Bridge1", DeviceType.BRIDGE, 350.0, 200.0);
        NetworkInterface bp1 = addInterface(bridge, "seg1", null, null, null, "00:B1:03:00:00:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface bp2 = addInterface(bridge, "seg2", null, null, null, "00:B1:03:00:00:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:A3:01:01:01:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 150.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", null, "00:A3:01:01:01:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 550.0, 120.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.1.20", "255.255.255.0", null, "00:A3:01:01:01:03", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc4 = createDevice(p.getId(), "PC4", DeviceType.PC, 550.0, 280.0);
        NetworkInterface pc4Intf = addInterface(pc4, "eth0", "192.168.1.21", "255.255.255.0", null, "00:A3:01:01:01:04", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, bp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc3Intf, bp2, ConnectionStatus.ACTIVE);

        saveScenario("V3", "Bridge Network", "VALID",
                "Demonstrates transparent Layer 2 bridging: Bridge learns segment MACs, isolates intra-segment traffic, and forwards cross-segment traffic.",
                "INTERMEDIATE", 5,
                "Layer 2 segment isolation, MAC learning table, same-segment frame filtering, collision domain division",
                "PC1 transmitting to PC3 causes the bridge to forward across segments; intra-segment frames are filtered.",
                null, p.getId());
    }

    private void seedV4_SwitchedLan() {
        if (scenarioRepository.existsByCode("V4")) return;
        Project p = createProject("[Demo V4] Switched LAN", "Dual-switch full-duplex Ethernet LAN with MAC learning and unicast forwarding.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 250.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:04:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:04:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface spUplink = addInterface(sw1, "port3", null, null, null, "00:SW:04:00:00:03", "1Gbps", 1, "TRUNK", InterfaceStatus.UP, InterfaceStatus.UP);

        Device sw2 = createDevice(p.getId(), "Switch2", DeviceType.SWITCH, 480.0, 200.0);
        NetworkInterface s2Uplink = addInterface(sw2, "port1", null, null, null, "00:SW:04:00:00:04", "1Gbps", 1, "TRUNK", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s2p2 = addInterface(sw2, "port2", null, null, null, "00:SW:04:00:00:05", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s2p3 = addInterface(sw2, "port3", null, null, null, "00:SW:04:00:00:06", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 100.0, 100.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:04:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 100.0, 300.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", null, "00:AA:04:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 640.0, 100.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.1.12", "255.255.255.0", null, "00:AA:04:01:01:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device server1 = createDevice(p.getId(), "Server1", DeviceType.SERVER, 640.0, 300.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.1.50", "255.255.255.0", null, "00:AA:04:01:01:50", "10Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp2, ConnectionStatus.ACTIVE);
        connect(p.getId(), spUplink, s2Uplink, ConnectionStatus.ACTIVE);
        connect(p.getId(), s2p2, pc3Intf, ConnectionStatus.ACTIVE);
        connect(p.getId(), s2p3, srvIntf, ConnectionStatus.ACTIVE);

        saveScenario("V4", "Switched LAN", "VALID",
                "Enterprise Ethernet LAN with 2 switches connecting 4 workstations and a server. Demonstrates MAC learning, broadcast flooding, and unicast forwarding.",
                "BEGINNER", 6,
                "Layer 2 switching, MAC table learning, unknown unicast flooding, ARP resolution, full-duplex microsegmentation",
                "Switches build MAC forwarding tables; initial ARP floods and subsequent unicast traffic forwards directly without collisions.",
                null, p.getId());
    }

    private void seedV5_BasicRouter() {
        if (scenarioRepository.existsByCode("V5")) return;
        Project p = createProject("[Demo V5] Basic Router Network", "Two distinct subnets routed via a Layer 3 Router with default gateway configuration.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 350.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:BB:05:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rEth1 = addInterface(r1, "eth1", "192.168.2.1", "255.255.255.0", null, "00:BB:05:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 200.0, 200.0);
        NetworkInterface sw1p1 = addInterface(sw1, "port1", null, null, null, "00:SW:05:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sw1p2 = addInterface(sw1, "port2", null, null, null, "00:SW:05:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sw1Uplink = addInterface(sw1, "port3", null, null, null, "00:SW:05:00:00:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device sw2 = createDevice(p.getId(), "Switch2", DeviceType.SWITCH, 500.0, 200.0);
        NetworkInterface sw2Uplink = addInterface(sw2, "port1", null, null, null, "00:SW:05:00:00:04", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sw2p2 = addInterface(sw2, "port2", null, null, null, "00:SW:05:00:00:05", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sw2p3 = addInterface(sw2, "port3", null, null, null, "00:SW:05:00:00:06", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 80.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:05:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 80.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", "192.168.1.1", "00:AA:05:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 640.0, 120.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.2.10", "255.255.255.0", "192.168.2.1", "00:AA:05:01:01:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc4 = createDevice(p.getId(), "PC4", DeviceType.PC, 640.0, 280.0);
        NetworkInterface pc4Intf = addInterface(pc4, "eth0", "192.168.2.11", "255.255.255.0", "192.168.2.1", "00:AA:05:01:01:04", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sw1p1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sw1p2, ConnectionStatus.ACTIVE);
        connect(p.getId(), sw1Uplink, rEth0, ConnectionStatus.ACTIVE);

        connect(p.getId(), rEth1, sw2Uplink, ConnectionStatus.ACTIVE);
        connect(p.getId(), sw2p2, pc3Intf, ConnectionStatus.ACTIVE);
        connect(p.getId(), sw2p3, pc4Intf, ConnectionStatus.ACTIVE);

        saveScenario("V5", "Basic Router Network", "VALID",
                "Demonstrates inter-subnet communication: Router1 routes packets between LAN1 (192.168.1.0/24) and LAN2 (192.168.2.0/24).",
                "BEGINNER", 7,
                "Layer 3 inter-subnet routing, default gateways, ARP next-hop resolution, TTL decrement, routing tables",
                "PC1 transmits packets destined for PC3 to default gateway 192.168.1.1; Router1 decapsulates, looks up route, decrements TTL, and forwards to PC3.",
                null, p.getId());
    }

    private void seedV6_MultiRouter() {
        if (scenarioRepository.existsByCode("V6")) return;
        Project p = createProject("[Demo V6] Multi-Router Network", "Three-hop routed enterprise WAN demonstrating static routing and TTL decrement.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 220.0, 200.0);
        NetworkInterface r1Lan = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:CC:06:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r1Wan = addInterface(r1, "eth1", "10.0.1.1", "255.255.255.252", null, "00:CC:06:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r2 = createDevice(p.getId(), "Router2", DeviceType.ROUTER, 420.0, 200.0);
        NetworkInterface r2Wan1 = addInterface(r2, "eth0", "10.0.1.2", "255.255.255.252", null, "00:CC:06:01:02:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r2Wan2 = addInterface(r2, "eth1", "10.0.2.1", "255.255.255.252", null, "00:CC:06:01:02:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r3 = createDevice(p.getId(), "Router3", DeviceType.ROUTER, 620.0, 200.0);
        NetworkInterface r3Wan = addInterface(r3, "eth0", "10.0.2.2", "255.255.255.252", null, "00:CC:06:01:03:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r3Lan = addInterface(r3, "eth1", "192.168.2.1", "255.255.255.0", null, "00:CC:06:01:03:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 80.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:06:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device server1 = createDevice(p.getId(), "Server1", DeviceType.SERVER, 760.0, 200.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.2.50", "255.255.255.0", "192.168.2.1", "00:AA:06:01:01:50", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, r1Lan, ConnectionStatus.ACTIVE);
        connect(p.getId(), r1Wan, r2Wan1, ConnectionStatus.ACTIVE);
        connect(p.getId(), r2Wan2, r3Wan, ConnectionStatus.ACTIVE);
        connect(p.getId(), r3Lan, srvIntf, ConnectionStatus.ACTIVE);

        // Static routes
        addRoute(p.getId(), r1.getId(), "192.168.2.0", "255.255.255.0", "10.0.1.2", "eth1");
        addRoute(p.getId(), r2.getId(), "192.168.2.0", "255.255.255.0", "10.0.2.2", "eth1");
        addRoute(p.getId(), r2.getId(), "192.168.1.0", "255.255.255.0", "10.0.1.1", "eth0");
        addRoute(p.getId(), r3.getId(), "192.168.1.0", "255.255.255.0", "10.0.2.1", "eth0");

        saveScenario("V6", "Multi-Router Network", "VALID",
                "Demonstrates multi-hop packet forwarding across 3 routers, longest-prefix matching, hop-by-hop TTL decrement, and route path tracking.",
                "ADVANCED", 5,
                "Multi-hop routing, static route configuration, WAN subnetting (/30), TTL decrement, routing path verification",
                "Traffic crosses R1, R2, and R3 sequentially, decrementing TTL at each hop and delivering to Server1.",
                null, p.getId());
    }

    private void seedV7_VlanNetwork() {
        if (scenarioRepository.existsByCode("V7")) return;
        Project p = createProject("[Demo V7] VLAN Network", "802.1Q VLAN trunking and isolation with inter-VLAN routing.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 240.0, 200.0);
        NetworkInterface s1v10 = addInterface(sw1, "port1", null, null, null, "00:SW:07:01:00:01", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s1v20 = addInterface(sw1, "port2", null, null, null, "00:SW:07:01:00:02", "1Gbps", 20, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s1Trunk = addInterface(sw1, "port3", null, null, null, "00:SW:07:01:00:03", "1Gbps", 1, "TRUNK", InterfaceStatus.UP, InterfaceStatus.UP);

        Device sw2 = createDevice(p.getId(), "Switch2", DeviceType.SWITCH, 480.0, 200.0);
        NetworkInterface s2Trunk = addInterface(sw2, "port1", null, null, null, "00:SW:07:02:00:01", "1Gbps", 1, "TRUNK", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s2v10 = addInterface(sw2, "port2", null, null, null, "00:SW:07:02:00:02", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface s2v20 = addInterface(sw2, "port3", null, null, null, "00:SW:07:02:00:03", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1-Vlan10", DeviceType.PC, 100.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.10.10", "255.255.255.0", null, "00:AA:07:01:01:01", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2-Vlan20", DeviceType.PC, 100.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.20.10", "255.255.255.0", null, "00:AA:07:01:01:02", "1Gbps", 20, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3-Vlan10", DeviceType.PC, 620.0, 120.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.10.11", "255.255.255.0", null, "00:AA:07:01:01:03", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, s1v10, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, s1v20, ConnectionStatus.ACTIVE);
        connect(p.getId(), s1Trunk, s2Trunk, ConnectionStatus.ACTIVE);
        connect(p.getId(), s2v10, pc3Intf, ConnectionStatus.ACTIVE);

        saveScenario("V7", "VLAN Network", "VALID",
                "Demonstrates 802.1Q Virtual LANs: VLAN 10 (Sales) and VLAN 20 (Engineering) isolated across switches connected via an 802.1Q trunk link.",
                "INTERMEDIATE", 5,
                "802.1Q tagging, access ports, trunk ports, broadcast isolation, VLAN boundary enforcement",
                "PC1 (VLAN 10) directly communicates with PC3 (VLAN 10) across the trunk; traffic is isolated from PC2 (VLAN 20).",
                null, p.getId());
    }

    private void seedV8_WirelessInfrastructure() {
        if (scenarioRepository.existsByCode("V8")) return;
        Project p = createProject("[Demo V8] Wireless Infrastructure Network", "802.11 Wireless Access Point bridging Wi-Fi clients to a wired network.");

        Device ap1 = createAp(p.getId(), "WAP1", 300.0, 200.0, "NetHub-Campus", 6, "WPA2-PSK (AES)", "nethub123");
        NetworkInterface apWlan = addInterface(ap1, "wlan0", null, null, null, "00:AP:08:00:00:01", "300Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface apEth = addInterface(ap1, "eth0", null, null, null, "00:AP:08:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device lap1 = createDevice(p.getId(), "Laptop1", DeviceType.PC, 120.0, 120.0);
        NetworkInterface l1Intf = addInterface(lap1, "wlan0", "192.168.1.15", "255.255.255.0", null, "00:AA:08:01:01:01", "300Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device lap2 = createDevice(p.getId(), "Laptop2", DeviceType.PC, 120.0, 280.0);
        NetworkInterface l2Intf = addInterface(lap2, "wlan0", "192.168.1.16", "255.255.255.0", null, "00:AA:08:01:01:02", "300Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 480.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:08:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:08:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device server1 = createDevice(p.getId(), "CampusServer", DeviceType.SERVER, 640.0, 200.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.1.50", "255.255.255.0", null, "00:AA:08:01:01:50", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), l1Intf, apWlan, ConnectionStatus.ACTIVE);
        connect(p.getId(), apEth, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, srvIntf, ConnectionStatus.ACTIVE);

        saveScenario("V8", "Wireless Infrastructure Network", "VALID",
                "Demonstrates 802.11 Infrastructure mode: Wireless clients associate with Access Point (WAP1) using CSMA/CA, bridging to the wired campus server.",
                "INTERMEDIATE", 5,
                "802.11 Infrastructure mode, BSSID association, CSMA/CA (DIFS/SIFS/RTS/CTS), 802.11 to 802.3 Ethernet bridging",
                "Wireless laptops communicate through WAP1 to reach the wired server with CSMA/CA contention resolution.",
                null, p.getId());
    }

    private void seedV9_WirelessAdhoc() {
        if (scenarioRepository.existsByCode("V9")) return;
        Project p = createProject("[Demo V9] Wireless Ad-hoc Network", "Peer-to-peer 802.11 IBSS ad-hoc wireless communication without an access point.");

        Device nodeA = createDevice(p.getId(), "NodeA", DeviceType.PC, 200.0, 200.0);
        NetworkInterface intfA = addInterface(nodeA, "wlan0", "192.168.99.10", "255.255.255.0", null, "00:AA:09:01:01:01", "54Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device nodeB = createDevice(p.getId(), "NodeB", DeviceType.PC, 380.0, 140.0);
        NetworkInterface intfB = addInterface(nodeB, "wlan0", "192.168.99.11", "255.255.255.0", null, "00:AA:09:01:01:02", "54Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device nodeC = createDevice(p.getId(), "NodeC", DeviceType.PC, 520.0, 240.0);
        NetworkInterface intfC = addInterface(nodeC, "wlan0", "192.168.99.12", "255.255.255.0", null, "00:AA:09:01:01:03", "54Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), intfA, intfB, ConnectionStatus.ACTIVE);
        connect(p.getId(), intfB, intfC, ConnectionStatus.ACTIVE);

        saveScenario("V9", "Wireless Ad-hoc Network", "VALID",
                "Demonstrates ad-hoc peer-to-peer wireless networking (IBSS): Nodes communicate directly over 802.11 without requiring a centralized base station.",
                "INTERMEDIATE", 3,
                "Ad-hoc IBSS networking, peer-to-peer 802.11 communication, DCF, wireless distributed coordination",
                "Direct wireless communication succeeds between peer nodes without an intermediary AP.",
                null, p.getId());
    }

    private void seedV10_DhcpDnsNetwork() {
        if (scenarioRepository.existsByCode("V10")) return;
        Project p = createProject("[Demo V10] DHCP and DNS Network", "Automated IP address assignment and DNS domain name resolution.");

        Device sw1 = createDevice(p.getId(), "CoreSwitch", DeviceType.SWITCH, 350.0, 220.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:10:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:10:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp3 = addInterface(sw1, "port3", null, null, null, "00:SW:10:00:00:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp4 = addInterface(sw1, "port4", null, null, null, "00:SW:10:00:00:04", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device dhcpServer = createDevice(p.getId(), "DnsDhcpServer", DeviceType.SERVER, 180.0, 100.0);
        NetworkInterface srvDhcp = addInterface(dhcpServer, "eth0", "192.168.1.2", "255.255.255.0", null, "00:AA:10:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device webServer = createDevice(p.getId(), "WebServer", DeviceType.SERVER, 520.0, 100.0);
        NetworkInterface srvWeb = addInterface(webServer, "eth0", "192.168.1.80", "255.255.255.0", null, "00:AA:10:01:01:80", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 180.0, 340.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:10:01:01:10", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 520.0, 340.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", "192.168.1.1", "00:AA:10:01:01:11", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), srvDhcp, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), srvWeb, sp2, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc1Intf, sp3, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp4, ConnectionStatus.ACTIVE);

        saveScenario("V10", "DHCP and DNS Network", "VALID",
                "Demonstrates network service automation: DHCP DORA address assignment, recursive DNS hostname resolution (web.nethub.local -> 192.168.1.80), and HTTP access.",
                "INTERMEDIATE", 5,
                "DHCP 4-step DORA (Discover, Offer, Request, Ack), DNS A-record queries, domain name resolution, client-server models",
                "Client queries DnsDhcpServer for web.nethub.local, receives 192.168.1.80, and accesses WebServer.",
                null, p.getId());
    }

    private void seedV11_TcpUdpClientServer() {
        if (scenarioRepository.existsByCode("V11")) return;
        Project p = createProject("[Demo V11] TCP and UDP Client-Server Network", "Comparison of reliable connection-oriented TCP vs lightweight connectionless UDP.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 350.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:11:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:11:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp3 = addInterface(sw1, "port3", null, null, null, "00:SW:11:00:00:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device client = createDevice(p.getId(), "ClientPC", DeviceType.PC, 150.0, 200.0);
        NetworkInterface clIntf = addInterface(client, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:11:01:01:10", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device tcpServer = createDevice(p.getId(), "TcpEchoServer", DeviceType.SERVER, 550.0, 120.0);
        NetworkInterface tcpIntf = addInterface(tcpServer, "eth0", "192.168.1.50", "255.255.255.0", null, "00:AA:11:01:01:50", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device udpServer = createDevice(p.getId(), "UdpCalcServer", DeviceType.SERVER, 550.0, 280.0);
        NetworkInterface udpIntf = addInterface(udpServer, "eth0", "192.168.1.51", "255.255.255.0", null, "00:AA:11:01:01:51", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), clIntf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, tcpIntf, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp3, udpIntf, ConnectionStatus.ACTIVE);

        saveScenario("V11", "TCP and UDP Client-Server Network", "VALID",
                "Demonstrates Transport Layer behavior: TCP 3-way handshake (SYN, SYN-ACK, ACK) and flow control vs lightweight connectionless UDP datagrams.",
                "INTERMEDIATE", 4,
                "TCP 3-way handshake, SYN/ACK sequence numbers, connection teardown, UDP datagrams, port numbers",
                "Client establishes reliable TCP connection with TcpEchoServer, and performs fast RPC calculation with UdpCalcServer.",
                null, p.getId());
    }

    private void seedV12_HttpHttpsNetwork() {
        if (scenarioRepository.existsByCode("V12")) return;
        Project p = createProject("[Demo V12] HTTP and HTTPS Network", "Secure web communication with TLS 1.3 cryptographic handshake.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:12:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:12:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp3 = addInterface(sw1, "port3", null, null, null, "00:SW:12:00:00:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device client = createDevice(p.getId(), "BrowserPC", DeviceType.PC, 120.0, 200.0);
        NetworkInterface clIntf = addInterface(client, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:12:01:01:10", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device webServer = createDevice(p.getId(), "SecureWebServer", DeviceType.SERVER, 500.0, 120.0);
        NetworkInterface wsIntf = addInterface(webServer, "eth0", "192.168.1.80", "255.255.255.0", null, "00:AA:12:01:01:80", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device dnsServer = createDevice(p.getId(), "DnsServer", DeviceType.SERVER, 500.0, 280.0);
        NetworkInterface dsIntf = addInterface(dnsServer, "eth0", "192.168.1.2", "255.255.255.0", null, "00:AA:12:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), clIntf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, wsIntf, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp3, dsIntf, ConnectionStatus.ACTIVE);

        saveScenario("V12", "HTTP and HTTPS Network", "VALID",
                "Demonstrates Application Layer protocols: HTTP (Port 80) plaintext vs HTTPS (Port 443) with TLS 1.3 key exchange and OSI 7-layer encapsulation.",
                "ADVANCED", 4,
                "HTTP GET/200 OK, HTTPS, TLS 1.3 ClientHello/ServerHello cryptographic handshake, 7-layer OSI encapsulation",
                "Client resolves domain via DNS, negotiates TLS 1.3 session keys, and exchanges encrypted HTTP application data.",
                null, p.getId());
    }

    private void seedV13_RoutingProtocols() {
        if (scenarioRepository.existsByCode("V13")) return;
        Project p = createProject("[Demo V13] Routing Protocol Network", "Dynamic routing with Dijkstra Shortest Path First (SPF) and link cost convergence.");

        Device r1 = createDeviceWithRouting(p.getId(), "Router1", DeviceType.ROUTER, 200.0, 150.0, "OSPF", 65001);
        NetworkInterface r1Eth0 = addInterface(r1, "eth0", "10.0.1.1", "255.255.255.252", null, "00:DD:13:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r1Eth1 = addInterface(r1, "eth1", "10.0.3.1", "255.255.255.252", null, "00:DD:13:01:01:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r2 = createDeviceWithRouting(p.getId(), "Router2", DeviceType.ROUTER, 420.0, 150.0, "OSPF", 65001);
        NetworkInterface r2Eth0 = addInterface(r2, "eth0", "10.0.1.2", "255.255.255.252", null, "00:DD:13:01:02:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r2Eth1 = addInterface(r2, "eth1", "10.0.2.1", "255.255.255.252", null, "00:DD:13:01:02:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r3 = createDeviceWithRouting(p.getId(), "Router3", DeviceType.ROUTER, 310.0, 320.0, "OSPF", 65001);
        NetworkInterface r3Eth0 = addInterface(r3, "eth0", "10.0.3.2", "255.255.255.252", null, "00:DD:13:01:03:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r3Eth1 = addInterface(r3, "eth1", "10.0.2.2", "255.255.255.252", null, "00:DD:13:01:03:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), r1Eth0, r2Eth0, ConnectionStatus.ACTIVE); // High speed (cost 1)
        connect(p.getId(), r2Eth1, r3Eth1, ConnectionStatus.ACTIVE); // High speed (cost 1)
        connect(p.getId(), r1Eth1, r3Eth0, ConnectionStatus.ACTIVE); // Lower speed (cost 10)

        saveScenario("V13", "Routing Protocol Network", "VALID",
                "Demonstrates dynamic link-state routing: Dijkstra Shortest Path Tree computation over triangular topology, favoring R1 -> R2 -> R3 based on cumulative cost metrics.",
                "ADVANCED", 3,
                "Dijkstra Shortest Path First, OSPF LSDB, link metrics, cost-based route selection, dynamic convergence",
                "Traffic selects lowest-metric path (R1 -> R2 -> R3 cost 2) over direct link (R1 -> R3 cost 10).",
                null, p.getId());
    }

    private void seedV14_ComparativeTopology() {
        if (scenarioRepository.existsByCode("V14")) return;
        Project p = createProject("[Demo V14] Hub, Switch and Router Comparison", "Side-by-side comparison of L1 Hub, L2 Bridge/Switch, and L3 Router forwarding.");

        // Hub segment
        Device hub = createDevice(p.getId(), "Hub1", DeviceType.HUB, 150.0, 150.0);
        NetworkInterface hp1 = addInterface(hub, "port1", null, null, null, "00:HH:14:00:00:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface hp2 = addInterface(hub, "port2", null, null, null, "00:HH:14:00:00:02", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        Device pc1 = createDevice(p.getId(), "PC1-Hub", DeviceType.PC, 50.0, 150.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:14:01:01:01", "100Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        connect(p.getId(), pc1Intf, hp1, ConnectionStatus.ACTIVE);

        // Switch segment
        Device sw = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 350.0, 150.0);
        NetworkInterface sp1 = addInterface(sw, "port1", null, null, null, "00:SS:14:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw, "port2", null, null, null, "00:SS:14:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        Device pc2 = createDevice(p.getId(), "PC2-Switched", DeviceType.PC, 350.0, 20.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", "192.168.1.1", "00:AA:14:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        connect(p.getId(), hp2, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp2, ConnectionStatus.ACTIVE);

        // Router segment
        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 550.0, 150.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:14:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rEth1 = addInterface(r1, "eth1", "192.168.2.1", "255.255.255.0", null, "00:RR:14:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        connect(p.getId(), sp2, rEth0, ConnectionStatus.ACTIVE);

        Device pc3 = createDevice(p.getId(), "PC3-Remote", DeviceType.PC, 700.0, 150.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.2.10", "255.255.255.0", "192.168.2.1", "00:AA:14:01:01:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        connect(p.getId(), rEth1, pc3Intf, ConnectionStatus.ACTIVE);

        saveScenario("V14", "Hub, Switch and Router Comparison", "VALID",
                "Comprehensive academic comparison topology demonstrating the fundamental differences between Layer 1 Hub repeating, Layer 2 Switch filtering, and Layer 3 Router packet routing.",
                "ADVANCED", 6,
                "Layer 1 repeating vs Layer 2 switching vs Layer 3 routing, collision domains, broadcast domains, PDU encapsulation",
                "Hub repeats unconditionally to all ports; Switch forwards unicast frames via MAC table; Router routes across subnet boundaries decrementing TTL.",
                null, p.getId());
    }

    // ==========================================
    // INVALID SCENARIOS (I1 - I18)
    // ==========================================

    private void seedI1_DuplicateIp() {
        if (scenarioRepository.existsByCode("I1")) return;
        Project p = createProject("[Demo I1] Duplicate IP Address", "Intentionally misconfigured network with two hosts assigned identical IP addresses.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I1:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I1:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp3 = addInterface(sw1, "port3", null, null, null, "00:SW:I1:00:00:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Intentional duplicate: both PC1 and PC2 have 192.168.1.10
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 120.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I1:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 120.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I1:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc3 = createDevice(p.getId(), "PC3", DeviceType.PC, 480.0, 200.0);
        NetworkInterface pc3Intf = addInterface(pc3, "eth0", "192.168.1.20", "255.255.255.0", null, "00:AA:I1:01:01:03", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp2, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc3Intf, sp3, ConnectionStatus.ACTIVE);

        saveScenario("I1", "Duplicate IP Address", "INVALID",
                "Two devices on the same subnet share the identical IP address (192.168.1.10), causing IP address conflict and ARP cache poisoning.",
                "BEGINNER", 4,
                "IP conflict, duplicate IP validation error, ARP table poisoning, gratuitous ARP",
                "Validation fails immediately with duplicate IP error. In simulation, ARP responses collide.",
                "Duplicate IP address found in project: 192.168.1.10", p.getId());
    }

    private void seedI2_IncorrectSubnetMask() {
        if (scenarioRepository.existsByCode("I2")) return;
        Project p = createProject("[Demo I2] Incorrect Subnet Mask", "Asymmetric subnet masks causing hosts to miscalculate local vs remote subnets.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I2:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I2:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I2:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // PC2 has overly narrow subnet mask 255.255.255.240 (/28), placing it in subnet 192.168.1.16/28!
        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 450.0, 200.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.20", "255.255.255.240", null, "00:AA:I2:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp2, ConnectionStatus.ACTIVE);

        saveScenario("I2", "Incorrect Subnet Mask", "INVALID",
                "PC1 (/24) treats PC2 as local, but PC2 (/28) treats PC1 as remote. PC2 fails to respond because it has no default gateway.",
                "BEGINNER", 3,
                "Subnet mask calculation, CIDR boundaries, asymmetric communication failure, default gateway dependency",
                "Communication fails when PC2 attempts to reply because it considers PC1 to be on an external network.",
                "Asymmetric subnet mask: 192.168.1.20/28 cannot reply to 192.168.1.10 without a default gateway", p.getId());
    }

    private void seedI3_InvalidDefaultGateway() {
        if (scenarioRepository.existsByCode("I3")) return;
        Project p = createProject("[Demo I3] Invalid Default Gateway", "Default gateway configured on an IP address outside the host's subnet.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 450.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:I3:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Intentional mistake: PC1 has IP 192.168.1.10/24 but gateway set to 192.168.2.1!
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.2.1", "00:AA:I3:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);

        saveScenario("I3", "Invalid Default Gateway", "INVALID",
                "PC1 has gateway 192.168.2.1, which does not belong to its local subnet (192.168.1.0/24). Host cannot resolve gateway MAC address.",
                "BEGINNER", 2,
                "Default gateway validation, subnet boundary verification, ARP resolution failure",
                "Validation identifies that Gateway IP does not belong to the interface subnet. Transmission to remote hosts drops.",
                "Gateway IP 192.168.2.1 does not belong to interface subnet (192.168.1.0) on device PC1", p.getId());
    }

    private void seedI4_MissingDefaultGateway() {
        if (scenarioRepository.existsByCode("I4")) return;
        Project p = createProject("[Demo I4] Missing Default Gateway", "Host attempting inter-network communication with no default gateway configured.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 350.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:I4:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rEth1 = addInterface(r1, "eth1", "192.168.2.1", "255.255.255.0", null, "00:RR:I4:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Missing default gateway: null/empty
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I4:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device server1 = createDevice(p.getId(), "Server1", DeviceType.SERVER, 550.0, 200.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.2.50", "255.255.255.0", "192.168.2.1", "00:AA:I4:01:01:50", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);
        connect(p.getId(), rEth1, srvIntf, ConnectionStatus.ACTIVE);

        saveScenario("I4", "Missing Default Gateway", "INVALID",
                "PC1 attempts to transmit to remote host 192.168.2.50, but has no default gateway configured. Host cannot route remote packets.",
                "BEGINNER", 3,
                "Default gateway requirement, local vs remote subnet logic, packet drop at source",
                "Simulation drops packet immediately: NO_DEFAULT_GATEWAY.",
                "Cannot route to remote destination: No default gateway configured.", p.getId());
    }

    private void seedI5_MissingRoutingTableEntry() {
        if (scenarioRepository.existsByCode("I5")) return;
        Project p = createProject("[Demo I5] Missing Routing Table Entry", "Router has no matching route for destination network, generating ICMP Destination Unreachable.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 350.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:I5:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rEth1 = addInterface(r1, "eth1", "10.0.0.1", "255.255.255.252", null, "00:RR:I5:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:I5:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);

        // No route for 172.16.1.0/24 on Router1
        saveScenario("I5", "Missing Routing Table Entry", "INVALID",
                "PC1 sends traffic destined for 172.16.1.50 to Router1. Router1 performs Longest-Prefix Match, finds no matching route, and drops the packet with ICMP Destination Unreachable.",
                "INTERMEDIATE", 2,
                "Longest-Prefix Match (LPM), routing table lookup, ICMP Destination Network Unreachable (Type 3, Code 0)",
                "Router1 emits ICMP_DEST_UNREACHABLE event and drops the packet.",
                "No route to host/network in Router1 routing table. ICMP Destination Unreachable generated.", p.getId());
    }

    private void seedI6_DisconnectedNetworkLink() {
        if (scenarioRepository.existsByCode("I6")) return;
        Project p = createProject("[Demo I6] Disconnected Network Link", "Physical link status is DOWN / cable disconnected between devices.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I6:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I6:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I6:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 450.0, 200.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.20", "255.255.255.0", null, "00:AA:I6:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Connection is INACTIVE (disconnected link)
        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.INACTIVE);
        connect(p.getId(), sp2, pc2Intf, ConnectionStatus.ACTIVE);

        saveScenario("I6", "Disconnected Network Link", "INVALID",
                "Cable connecting PC1 to Switch1 is unplugged (Connection status DISCONNECTED). Frame cannot propagate across Layer 1 medium.",
                "BEGINNER", 3,
                "Layer 1 physical link check, cable fault detection, carrier detect loss",
                "Simulation drops frame at physical layer with LINK_DOWN / DISCONNECTED_CABLE error.",
                "Interface eth0 is operationally DOWN (disconnected cable).", p.getId());
    }

    private void seedI7_InterfaceDisabled() {
        if (scenarioRepository.existsByCode("I7")) return;
        Project p = createProject("[Demo I7] Interface Disabled", "Device interface is administratively shutdown (adminStatus = DOWN).");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I7:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I7:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // PC1 eth0 is administratively disabled
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I7:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.DOWN, InterfaceStatus.DOWN);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 450.0, 200.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.20", "255.255.255.0", null, "00:AA:I7:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, pc2Intf, ConnectionStatus.ACTIVE);

        saveScenario("I7", "Interface Disabled", "INVALID",
                "PC1's network interface eth0 has been administratively shut down. Operational status is DOWN, preventing transmission.",
                "BEGINNER", 3,
                "Administrative shutdown status, operational link status, interface state machine",
                "Simulation fails with INTERFACE_DOWN at source host.",
                "Interface eth0 is operationally DOWN (administratively disabled).", p.getId());
    }

    private void seedI8_IncorrectArpResolution() {
        if (scenarioRepository.existsByCode("I8")) return;
        Project p = createProject("[Demo I8] Incorrect ARP Resolution", "Host transmits to non-existent IP address, causing ARP Request broadcast timeout.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I8:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I8:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I8:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 450.0, 200.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.20", "255.255.255.0", null, "00:AA:I8:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, pc2Intf, ConnectionStatus.ACTIVE);

        saveScenario("I8", "Incorrect ARP Resolution", "INVALID",
                "PC1 attempts to transmit to non-existent host 192.168.1.99. ARP Request broadcasts flood the switch, but receive no ARP Reply, timing out.",
                "BEGINNER", 3,
                "Address Resolution Protocol, ARP broadcast, timeout handling, non-existent destination IP",
                "PC1 broadcasts ARP Request, receives no reply, and simulation halts with ARP_TIMEOUT.",
                "ARP Resolution Failed: No reply received for 192.168.1.99.", p.getId());
    }

    private void seedI9_VlanMisconfiguration() {
        if (scenarioRepository.existsByCode("I9")) return;
        Project p = createProject("[Demo I9] VLAN Misconfiguration", "Port VLAN mismatch isolating hosts in the same IP subnet.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        // Port 1 in VLAN 10, Port 2 in VLAN 20
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:I9:00:00:01", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:I9:00:00:02", "1Gbps", 20, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 150.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:I9:01:01:01", "1Gbps", 10, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 450.0, 150.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.20", "255.255.255.0", null, "00:AA:I9:01:01:02", "1Gbps", 20, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, pc2Intf, ConnectionStatus.ACTIVE);

        saveScenario("I9", "VLAN Misconfiguration", "INVALID",
                "PC1 and PC2 have IP addresses in the same subnet (192.168.1.0/24), but are assigned to different switch VLANs (10 vs 20). Switch drops frames at the VLAN boundary.",
                "INTERMEDIATE", 3,
                "VLAN port assignment, 802.1Q boundary isolation, broadcast domain segmentation",
                "Switch filters and drops the frame because egress port is in a different VLAN: FRAME_FILTERED.",
                "VLAN mismatch: Switch isolated traffic between VLAN 10 and VLAN 20.", p.getId());
    }

    private void seedI10_RoutingLoop() {
        if (scenarioRepository.existsByCode("I10")) return;
        Project p = createProject("[Demo I10] Routing Loop or TTL Expiration", "Circular static route configuration causing infinite loop and TTL expiration.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 250.0, 200.0);
        NetworkInterface r1Eth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:10:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r1Wan = addInterface(r1, "eth1", "10.0.0.1", "255.255.255.252", null, "00:RR:10:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r2 = createDevice(p.getId(), "Router2", DeviceType.ROUTER, 450.0, 200.0);
        NetworkInterface r2Wan = addInterface(r2, "eth0", "10.0.0.2", "255.255.255.252", null, "00:RR:10:01:02:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface r2Eth1 = addInterface(r2, "eth1", "192.168.2.1", "255.255.255.0", null, "00:RR:10:01:02:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 100.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:10:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, r1Eth0, ConnectionStatus.ACTIVE);
        connect(p.getId(), r1Wan, r2Wan, ConnectionStatus.ACTIVE);

        // Circular routing loop for network 192.168.5.0/24:
        // R1 routes to R2 (10.0.0.2), R2 routes right back to R1 (10.0.0.1)!
        addRoute(p.getId(), r1.getId(), "192.168.5.0", "255.255.255.0", "10.0.0.2", "eth1");
        addRoute(p.getId(), r2.getId(), "192.168.5.0", "255.255.255.0", "10.0.0.1", "eth0");

        saveScenario("I10", "Routing Loop or TTL Expiration", "INVALID",
                "R1 and R2 have circular static routes pointing to each other for 192.168.5.0/24. Packets loop indefinitely until TTL decrements to 0, producing ICMP Time Exceeded.",
                "ADVANCED", 3,
                "Routing loop, mutual route recurrence, IPv4 TTL decrement, ICMP Time Exceeded (Type 11, Code 0)",
                "Packet bounces between R1 and R2 until TTL reaches 0; dropped with ICMP_TIME_EXCEEDED.",
                "Packet dropped: TTL expired in transit. ICMP Time Exceeded generated.", p.getId());
    }

    private void seedI11_DhcpFailure() {
        if (scenarioRepository.existsByCode("I11")) return;
        Project p = createProject("[Demo I11] DHCP Failure", "Host in DHCP mode broadcasting Discover on a network without an active DHCP server.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:D1:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // PC configured without static IP, expecting DHCP
        Device pc1 = createDevice(p.getId(), "ClientPC", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", null, null, null, "00:AA:11:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);

        saveScenario("I11", "DHCP Failure", "INVALID",
                "ClientPC broadcasts DHCP Discover message to 255.255.255.255, but no DHCP server is available. Address acquisition fails, leaving the interface unconfigured.",
                "INTERMEDIATE", 2,
                "DHCP Discover broadcast, DHCP server timeout, automatic addressing failure",
                "No DHCP Offer received. Client fails to obtain IP address and cannot transmit.",
                "No network interface with a configured IP address found on ClientPC.", p.getId());
    }

    private void seedI12_DnsFailure() {
        if (scenarioRepository.existsByCode("I12")) return;
        Project p = createProject("[Demo I12] DNS Failure", "Client configured with unreachable DNS server IP, preventing hostname resolution.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:12:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:12:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // PC configured with unreachable DNS server 192.168.1.250
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:12:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        pc1Intf.setDnsServer("192.168.1.250");
        interfaceRepository.save(pc1Intf);

        Device server1 = createDevice(p.getId(), "WebServer", DeviceType.SERVER, 480.0, 200.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.1.80", "255.255.255.0", null, "00:AA:12:01:01:80", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), sp2, srvIntf, ConnectionStatus.ACTIVE);

        saveScenario("I12", "DNS Failure", "INVALID",
                "PC1 attempts to resolve 'intranet.company.local', but its configured DNS server (192.168.1.250) is offline. Name resolution fails while direct IP communication works.",
                "INTERMEDIATE", 3,
                "DNS query timeout, DNS server unreachable, hostname resolution failure vs IP reachability",
                "DNS query to 192.168.1.250 times out. Direct IP ping to 192.168.1.80 succeeds.",
                "DNS resolution failed: Unreachable DNS server 192.168.1.250.", p.getId());
    }

    private void seedI13_AclFirewallBlocking() {
        if (scenarioRepository.existsByCode("I13")) return;
        Project p = createProject("[Demo I13] ACL or Firewall Blocking", "Router Access Control List (ACL) rule explicitly dropping HTTP traffic.");

        Device r1 = createDevice(p.getId(), "EdgeRouter", DeviceType.ROUTER, 320.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:13:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface rEth1 = addInterface(r1, "eth1", "192.168.2.1", "255.255.255.0", null, "00:RR:13:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 120.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:13:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device server1 = createDevice(p.getId(), "RestrictedServer", DeviceType.SERVER, 520.0, 200.0);
        NetworkInterface srvIntf = addInterface(server1, "eth0", "192.168.2.50", "255.255.255.0", "192.168.2.1", "00:AA:13:01:01:50", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);
        connect(p.getId(), rEth1, srvIntf, ConnectionStatus.ACTIVE);

        saveScenario("I13", "ACL or Firewall Blocking", "INVALID",
                "EdgeRouter has an active Access Control List rule: DENY TCP traffic from 192.168.1.0/24 to RestrictedServer port 80. Packet is dropped at router ingress.",
                "ADVANCED", 3,
                "Access Control Lists (ACL), packet filtering, firewall policy enforcement, ACL_DENIED event",
                "Packet is inspected at EdgeRouter, matched against ACL rule #10, and dropped with ACL_DENIED.",
                "Packet dropped by Access Control List on EdgeRouter (DENY TCP port 80).", p.getId());
    }

    private void seedI14_WirelessAuthFailure() {
        if (scenarioRepository.existsByCode("I14")) return;
        Project p = createProject("[Demo I14] Wireless Authentication Failure", "Client laptop configured with incorrect WPA2 pre-shared key.");

        Device ap1 = createAp(p.getId(), "SecureAP", 350.0, 200.0, "NetHub-Secure", 6, "WPA2-PSK (AES)", "CorrectSecretKey");
        NetworkInterface apWlan = addInterface(ap1, "wlan0", null, null, null, "00:AP:14:00:00:01", "300Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Laptop has mismatched key
        Device lap1 = createDevice(p.getId(), "Laptop1", DeviceType.PC, 150.0, 200.0);
        lap1.setWirelessKey("WrongPassword999");
        deviceRepository.save(lap1);
        NetworkInterface l1Intf = addInterface(lap1, "wlan0", "192.168.1.15", "255.255.255.0", null, "00:AA:14:01:01:01", "300Mbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), l1Intf, apWlan, ConnectionStatus.ACTIVE);

        saveScenario("I14", "Wireless Authentication Failure", "INVALID",
                "Laptop1 attempts 4-way WPA2 handshake with SecureAP using an incorrect key. 802.11 association fails and client is de-authenticated.",
                "INTERMEDIATE", 2,
                "802.11 association, WPA2-PSK 4-way handshake, cryptographic key mismatch, deauthentication",
                "Handshake fails due to incorrect pre-shared key; association rejected.",
                "Wireless 4-Way Handshake Failed: Invalid pre-shared key.", p.getId());
    }

    private void seedI15_MtuMismatch() {
        if (scenarioRepository.existsByCode("I15")) return;
        Project p = createProject("[Demo I15] MTU Mismatch", "Packet payload exceeds link MTU without allowable IP fragmentation.");

        Device r1 = createDevice(p.getId(), "Router1", DeviceType.ROUTER, 320.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:15:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        // Constrained WAN link MTU = 576 bytes
        NetworkInterface rWan = addInterface(r1, "eth1", "10.0.0.1", "255.255.255.252", null, "00:RR:15:01:01:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        rWan.setMtu(576);
        interfaceRepository.save(rWan);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 120.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:15:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);

        saveScenario("I15", "MTU Mismatch", "INVALID",
                "PC1 sends standard 1500-byte frame, but Router1's outgoing WAN interface has MTU 576 bytes. With Don't Fragment (DF) bit set, router drops the packet.",
                "ADVANCED", 2,
                "Maximum Transmission Unit (MTU), DF (Don't Fragment) bit, path MTU discovery, packet sizing",
                "Packet size exceeds outgoing interface MTU (1500 > 576) and is dropped.",
                "Packet dropped: Exceeds interface MTU (1500 bytes > 576 bytes) with DF set.", p.getId());
    }

    private void seedI16_DuplicateMac() {
        if (scenarioRepository.existsByCode("I16")) return;
        Project p = createProject("[Demo I16] Duplicate MAC Address", "Two network interfaces configured with identical MAC addresses causing switch table thrashing.");

        Device sw1 = createDevice(p.getId(), "Switch1", DeviceType.SWITCH, 300.0, 200.0);
        NetworkInterface sp1 = addInterface(sw1, "port1", null, null, null, "00:SW:16:00:00:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);
        NetworkInterface sp2 = addInterface(sw1, "port2", null, null, null, "00:SW:16:00:00:02", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        // Conflicting MAC: 00:AA:BB:CC:DD:01 on both interfaces
        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 150.0, 120.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", null, "00:AA:BB:CC:DD:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc2 = createDevice(p.getId(), "PC2", DeviceType.PC, 150.0, 280.0);
        NetworkInterface pc2Intf = addInterface(pc2, "eth0", "192.168.1.11", "255.255.255.0", null, "00:AA:BB:CC:DD:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, sp1, ConnectionStatus.ACTIVE);
        connect(p.getId(), pc2Intf, sp2, ConnectionStatus.ACTIVE);

        saveScenario("I16", "Duplicate MAC Address", "INVALID",
                "PC1 and PC2 have identical MAC addresses (00:AA:BB:CC:DD:01). Switch MAC address table flutters between port1 and port2, corrupting Layer 2 delivery.",
                "BEGINNER", 3,
                "Layer 2 MAC uniqueness, MAC address table thrashing/flapping, validation failure",
                "Validation service catches duplicate MAC address conflict.",
                "Duplicate MAC address found: 00:AA:BB:CC:DD:01", p.getId());
    }

    private void seedI17_NoInternetRoute() {
        if (scenarioRepository.existsByCode("I17")) return;
        Project p = createProject("[Demo I17] No Internet/External Route", "Local gateway has no default route (0.0.0.0/0) to the public internet.");

        Device r1 = createDevice(p.getId(), "GatewayRouter", DeviceType.ROUTER, 320.0, 200.0);
        NetworkInterface rEth0 = addInterface(r1, "eth0", "192.168.1.1", "255.255.255.0", null, "00:RR:17:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device pc1 = createDevice(p.getId(), "PC1", DeviceType.PC, 120.0, 200.0);
        NetworkInterface pc1Intf = addInterface(pc1, "eth0", "192.168.1.10", "255.255.255.0", "192.168.1.1", "00:AA:17:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), pc1Intf, rEth0, ConnectionStatus.ACTIVE);

        saveScenario("I17", "No Internet/External Route", "INVALID",
                "PC1 pings public DNS (8.8.8.8). GatewayRouter receives the packet, but has no default route (0.0.0.0/0) to external WAN, dropping with ICMP Destination Unreachable.",
                "INTERMEDIATE", 2,
                "Default route (0.0.0.0/0), public vs private IP routing, ICMP Destination Unreachable",
                "Local traffic reaches 192.168.1.1; public internet ping to 8.8.8.8 drops due to missing default gateway route.",
                "No route to host/network 8.8.8.8 in GatewayRouter routing table.", p.getId());
    }

    private void seedI18_RoutingProtocolMisconfig() {
        if (scenarioRepository.existsByCode("I18")) return;
        Project p = createProject("[Demo I18] Routing Protocol Misconfiguration", "Mismatched OSPF Area IDs preventing neighbor adjacency formation.");

        // R1 is in Area 0, R2 is in Area 1
        Device r1 = createDeviceWithRouting(p.getId(), "Router1", DeviceType.ROUTER, 220.0, 200.0, "OSPF", 65001);
        NetworkInterface r1Wan = addInterface(r1, "eth0", "10.0.0.1", "255.255.255.252", null, "00:DD:18:01:01:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        Device r2 = createDeviceWithRouting(p.getId(), "Router2", DeviceType.ROUTER, 440.0, 200.0, "OSPF", 65001);
        NetworkInterface r2Wan = addInterface(r2, "eth0", "10.0.0.2", "255.255.255.252", null, "00:DD:18:01:02:01", "1Gbps", 1, "ACCESS", InterfaceStatus.UP, InterfaceStatus.UP);

        connect(p.getId(), r1Wan, r2Wan, ConnectionStatus.ACTIVE);

        saveScenario("I18", "Routing Protocol Misconfiguration", "INVALID",
                "Router1 has interface eth0 configured in OSPF Area 0, while Router2 is configured in OSPF Area 1. OSPF Hello packet exchange rejects neighbor adjacency.",
                "ADVANCED", 2,
                "OSPF Area ID matching, Hello packet exchange, neighbor adjacency failure, routing protocol troubleshooting",
                "OSPF neighbor adjacency fails to reach FULL state; dynamic routes are not advertised.",
                "OSPF Adjacency Failure: Area ID mismatch (Area 0 vs Area 1).", p.getId());
    }

    // ==========================================
    // HELPER BUILDERS
    // ==========================================

    private Project createProject(String name, String description) {
        Project p = new Project();
        p.setName(name);
        p.setDescription(description);
        return projectRepository.save(p);
    }

    private Device createDevice(Long projectId, String name, DeviceType type, double x, double y) {
        Device d = Device.builder()
                .projectId(projectId)
                .name(name)
                .hostname(name.toLowerCase() + ".local")
                .type(type)
                .status(DeviceStatus.ONLINE)
                .positionX(x)
                .positionY(y)
                .routingProtocol("STATIC")
                .build();
        return deviceRepository.save(d);
    }

    private Device createDeviceWithRouting(Long projectId, String name, DeviceType type, double x, double y, String proto, Integer as) {
        Device d = Device.builder()
                .projectId(projectId)
                .name(name)
                .hostname(name.toLowerCase() + ".local")
                .type(type)
                .status(DeviceStatus.ONLINE)
                .positionX(x)
                .positionY(y)
                .routingProtocol(proto)
                .asNumber(as)
                .build();
        return deviceRepository.save(d);
    }

    private Device createAp(Long projectId, String name, double x, double y, String ssid, int ch, String sec, String key) {
        Device d = Device.builder()
                .projectId(projectId)
                .name(name)
                .hostname(name.toLowerCase() + ".local")
                .type(DeviceType.ACCESS_POINT)
                .status(DeviceStatus.ONLINE)
                .positionX(x)
                .positionY(y)
                .ssid(ssid)
                .wirelessChannel(ch)
                .wirelessSecurity(sec)
                .wirelessKey(key)
                .build();
        return deviceRepository.save(d);
    }

    private NetworkInterface addInterface(Device dev, String name, String ip, String mask, String gw, String mac, String speed, Integer vlanId, String portMode, InterfaceStatus status, InterfaceStatus adminStatus) {
        NetworkInterface intf = NetworkInterface.builder()
                .deviceId(dev.getId())
                .name(name)
                .ipAddress(ip)
                .subnetMask(mask)
                .gateway(gw)
                .macAddress(mac != null ? mac : "00:50:56:" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .speed(speed != null ? speed : "1Gbps")
                .vlanId(vlanId != null ? vlanId : 1)
                .portMode(portMode != null ? portMode : "ACCESS")
                .status(status != null ? status : InterfaceStatus.UP)
                .adminStatus(adminStatus != null ? adminStatus : InterfaceStatus.UP)
                .mtu(1500)
                .duplex("FULL")
                .build();
        return interfaceRepository.save(intf);
    }

    private Connection connect(Long projectId, NetworkInterface intf1, NetworkInterface intf2, ConnectionStatus status) {
        Connection c = new Connection();
        c.setProjectId(projectId);
        c.setSourceInterfaceId(intf1.getId());
        c.setDestinationInterfaceId(intf2.getId());
        c.setStatus(status != null ? status : ConnectionStatus.ACTIVE);
        return connectionRepository.save(c);
    }

    private Route addRoute(Long projectId, Long deviceId, String destNet, String mask, String nextHop, String outIntf) {
        Route r = Route.builder()
                .projectId(projectId)
                .deviceId(deviceId)
                .destinationNetwork(destNet)
                .subnetMask(mask)
                .nextHop(nextHop)
                .outgoingInterface(outIntf)
                .routeType(RouteType.STATIC)
                .metric(1)
                .build();
        return routeRepository.save(r);
    }

    private void saveScenario(String code, String name, String category, String description,
                              String difficulty, int deviceCount, String concepts, String expectedResult,
                              String expectedErrors, Long templateProjectId) {
        DemonstrationScenario scenario = DemonstrationScenario.builder()
                .code(code)
                .name(name)
                .category(category)
                .description(description)
                .difficulty(difficulty)
                .deviceCount(deviceCount)
                .concepts(concepts)
                .expectedResult(expectedResult)
                .expectedErrors(expectedErrors)
                .templateProjectId(templateProjectId)
                .build();
        scenarioRepository.save(scenario);
    }
}
