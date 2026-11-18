package com.nethub;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.engine.routing.DijkstraEngine.GraphEdge;
import com.nethub.engine.routing.RipEngine;
import com.nethub.engine.security.AclEngine;
import com.nethub.engine.security.FirewallEngine;
import com.nethub.engine.security.NatEngine;
import com.nethub.engine.simulation.DiscreteNetworkSimulator;
import com.nethub.entity.*;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.entity.enums.RouteType;
import com.nethub.model.*;
import com.nethub.repository.*;
import com.nethub.service.DeviceStateManager;
import com.nethub.service.NetworkSimulationService;
import com.nethub.service.RoutingTableService;
import com.nethub.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RealisticNetworkSimulationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private NetworkInterfaceRepository interfaceRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RoutingTableService routingTableService;
    @Autowired
    private RoutingEngine routingEngine;
    @Autowired
    private SwitchingEngine switchingEngine;
    @Autowired
    private ArpEngine arpEngine;
    @Autowired
    private NetworkPathFinder pathFinder;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private DeviceStateManager stateManager;
    @Autowired
    private DiscreteNetworkSimulator discreteSimulator;
    @Autowired
    private com.nethub.engine.hardware.HubEngine hubEngine;
    @Autowired
    private com.nethub.engine.hardware.BridgeEngine bridgeEngine;
    @Autowired
    private com.nethub.engine.hardware.WirelessEngine wirelessEngine;
    @Autowired
    private com.nethub.engine.hardware.MacControlEngine macControlEngine;
    @Autowired
    private com.nethub.engine.switching.SwitchingTechniquesEngine switchingTechniquesEngine;
    @Autowired
    private com.nethub.engine.routing.DijkstraEngine dijkstraEngine;
    @Autowired
    private com.nethub.engine.routing.OspfEngine ospfEngine;
    @Autowired
    private com.nethub.engine.routing.RipEngine ripEngine;
    @Autowired
    private com.nethub.engine.routing.BgpEngine bgpEngine;
    @Autowired
    private com.nethub.engine.routing.AodvEngine aodvEngine;
    @Autowired
    private com.nethub.engine.transport.SocketApiEngine socketApiEngine;
    @Autowired
    private com.nethub.engine.protocols.ApplicationProtocolEngine applicationProtocolEngine;
    @Autowired
    private com.nethub.engine.security.NatEngine natEngine;
    @Autowired
    private com.nethub.engine.security.AclEngine aclEngine;
    @Autowired
    private com.nethub.engine.security.FirewallEngine firewallEngine;
    @Autowired
    private com.nethub.engine.osi.OsiLayerVisualizer osiLayerVisualizer;
    @Autowired
    private com.nethub.service.DeviceTerminalService terminalService;

    private NetworkSimulationService simulationService;

    @BeforeEach
    public void setUp() {
        switchingEngine.resetAll();
        arpEngine.resetAll();
        stateManager.resetAllStats();
        simulationService = new NetworkSimulationService(
                deviceRepository, interfaceRepository, connectionRepository,
                routingTableService, routingEngine, switchingEngine, arpEngine,
                pathFinder, routeRepository, stateManager, discreteSimulator);
    }

    // Scenario 1: Two PCs communicating on the same subnet
    @Test
    public void testTwoPcsSameSubnet() {
        Project p = projectRepository.save(new Project(null, null, "S1 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:01:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:01:01:01:02", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertTrue(res.isSuccess());
        assertEquals("PC1", res.getSource());
        assertEquals("192.168.1.20", res.getDestination());
    }

    // Scenario 2: First-time ARP resolution (dynamic broadcast & reply)
    @Test
    public void testFirstTimeArpResolution() {
        Project p = projectRepository.save(new Project(null, null, "S2 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device sw1 = deviceRepository.save(new Device(null, p.getId(), "Switch1", DeviceType.SWITCH, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:02:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface swPort1 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port1", "00:AA:02:01:01:02", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface swPort2 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port2", "00:AA:02:01:01:03", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:02:01:01:04", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), swPort1.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), swPort2.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        // Verify ARP cache empty before
        assertTrue(arpEngine.lookupMac(pc1.getId(), "192.168.1.20").isEmpty());

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertTrue(res.isSuccess());

        // Verify ARP Request & Reply events were generated
        boolean hasArpReq = res.getEvents().stream().anyMatch(e -> "ARP_REQUEST".equals(e.getType()));
        boolean hasArpRep = res.getEvents().stream().anyMatch(e -> "ARP_REPLY".equals(e.getType()));
        assertTrue(hasArpReq);
        assertTrue(hasArpRep);

        // Verify ARP cache now populated
        Optional<String> cachedMac = arpEngine.lookupMac(pc1.getId(), "192.168.1.20");
        assertTrue(cachedMac.isPresent());
        assertEquals("00:AA:02:01:01:04", cachedMac.get());
    }

    // Scenario 3: Subsequent communication using the ARP cache (Cache hit, no broadcast)
    @Test
    public void testSubsequentCommunicationUsingArpCache() {
        Project p = projectRepository.save(new Project(null, null, "S3 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:03:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:03:01:01:02", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        // First transmission populates cache
        simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");

        // Second transmission must hit ARP cache without broadcasting
        SimulationResult res2 = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertTrue(res2.isSuccess());

        boolean hasCacheHit = res2.getEvents().stream().anyMatch(e -> "ARP_CACHE_HIT".equals(e.getType()));
        boolean hasArpReq = res2.getEvents().stream().anyMatch(e -> "ARP_REQUEST".equals(e.getType()));
        assertTrue(hasCacheHit);
        assertFalse(hasArpReq); // No broadcast needed!
    }

    // Scenario 4: Communication between different subnets through a router
    @Test
    public void testDifferentSubnetsThroughRouter() {
        Project p = projectRepository.save(new Project(null, null, "S4 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:04:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface rEth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:04:01:01:02", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface rEth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "00:AA:04:01:01:03", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:04:01:01:04", "192.168.2.10", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), rEth1.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.2.10");
        assertTrue(res.isSuccess());

        // Check Router processed L3 forwarding, TTL decrement, and L2 re-encapsulation
        boolean hasRouteLookup = res.getEvents().stream().anyMatch(e -> "ROUTE_LOOKUP".equals(e.getType()));
        boolean hasTtlDec = res.getEvents().stream().anyMatch(e -> "TTL_DECREMENT".equals(e.getType()));
        boolean hasReencap = res.getEvents().stream().anyMatch(e -> "FRAME_REENCAPSULATION".equals(e.getType()));
        assertTrue(hasRouteLookup);
        assertTrue(hasTtlDec);
        assertTrue(hasReencap);
    }

    // Scenario 5: Communication across multiple routers (PC1 -> R1 -> R2 -> Server1)
    @Test
    public void testCommunicationAcrossMultipleRouters() {
        Project p = projectRepository.save(new Project(null, null, "S5 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device r2  = deviceRepository.save(new Device(null, p.getId(), "Router2", DeviceType.ROUTER, DeviceStatus.ONLINE, 200.0, 0.0, null, null));
        Device srv = deviceRepository.save(new Device(null, p.getId(), "Server1", DeviceType.SERVER, DeviceStatus.ONLINE, 300.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:05:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface r1Eth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:05:01:01:02", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface r1Eth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "00:AA:05:01:01:03", "10.0.0.1", "255.255.255.252", null, null, InterfaceStatus.UP));

        NetworkInterface r2Eth0 = interfaceRepository.save(new NetworkInterface(null, r2.getId(), "eth0", "00:AA:05:01:01:04", "10.0.0.2", "255.255.255.252", null, null, InterfaceStatus.UP));
        NetworkInterface r2Eth1 = interfaceRepository.save(new NetworkInterface(null, r2.getId(), "eth1", "00:AA:05:01:01:05", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, srv.getId(), "eth0", "00:AA:05:01:01:06", "192.168.2.10", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), r1Eth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), r1Eth1.getId(), r2Eth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), r2Eth1.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        // Add static route on R1 pointing to 192.168.2.0/24 via R2 (10.0.0.2)
        routeRepository.save(Route.builder()
                .projectId(p.getId())
                .deviceId(r1.getId())
                .destinationNetwork("192.168.2.0")
                .subnetMask("255.255.255.0")
                .nextHop("10.0.0.2")
                .outgoingInterface("eth1")
                .metric(1)
                .routeType(RouteType.STATIC)
                .build());

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.2.10");
        assertTrue(res.isSuccess());

        // Multi-router hop path verification
        assertTrue(res.getPath().contains("Router1"));
        assertTrue(res.getPath().contains("Router2"));
        assertTrue(res.getPath().contains("Server1"));

        // Verify two TTL decrements occur across the two routers
        long ttlDecrements = res.getEvents().stream().filter(e -> "TTL_DECREMENT".equals(e.getType())).count();
        assertEquals(2, ttlDecrements);
    }

    // Scenario 6: Unknown destination MAC flooding on switch
    @Test
    public void testUnknownDestinationMacFlooding() {
        SwitchingEngine engine = new SwitchingEngine();
        EthernetFrame frame = EthernetFrame.builder()
                .sourceMac("00:AA:06:01:01:01")
                .destinationMac("00:AA:06:01:01:99") // Unknown MAC
                .build();

        List<String> availablePorts = List.of("port1", "port2", "port3");
        List<String> egressPorts = engine.processFrame(1L, frame, "port1", availablePorts);

        // Must flood to all ports except incoming port1
        assertEquals(2, egressPorts.size());
        assertTrue(egressPorts.contains("port2"));
        assertTrue(egressPorts.contains("port3"));
        assertFalse(egressPorts.contains("port1"));
    }

    // Scenario 7: Correct MAC address learning on switch
    @Test
    public void testCorrectMacAddressLearning() {
        SwitchingEngine engine = new SwitchingEngine();
        EthernetFrame frame = EthernetFrame.builder()
                .sourceMac("00:AA:07:01:01:01")
                .destinationMac("FF:FF:FF:FF:FF:FF")
                .build();

        engine.processFrame(1L, frame, "port2", List.of("port1", "port2", "port3"));

        MacAddressTable table = engine.getMacTable(1L);
        Optional<MacTableEntry> entryOpt = table.lookup("00:AA:07:01:01:01");
        assertTrue(entryOpt.isPresent());
        assertEquals("port2", entryOpt.get().getPort());
    }

    // Scenario 8: Missing route (ICMP Destination Network Unreachable)
    @Test
    public void testMissingRoute() {
        Project p = projectRepository.save(new Project(null, null, "S8 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:08:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface rEth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:08:01:01:02", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));

        // Router has no route to 172.16.50.1
        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "172.16.50.1");
        assertFalse(res.isSuccess());

        boolean hasUnreachable = res.getEvents().stream().anyMatch(e -> "ICMP_DEST_UNREACHABLE".equals(e.getType()));
        assertTrue(hasUnreachable);
    }

    // Scenario 9: Disconnected cable
    @Test
    public void testDisconnectedCable() {
        Project p = projectRepository.save(new Project(null, null, "S9 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:09:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertFalse(res.isSuccess());
        assertEquals("LINK_DOWN", res.getErrorCode());
    }

    // Scenario 10: Interface administratively disabled
    @Test
    public void testInterfaceAdministrativelyDisabled() {
        Project p = projectRepository.save(new Project(null, null, "S10 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        // Interface adminStatus set to DOWN
        NetworkInterface intf1 = interfaceRepository.save(NetworkInterface.builder()
                .deviceId(pc1.getId())
                .name("eth0")
                .macAddress("00:AA:10:01:01:01")
                .ipAddress("192.168.1.10")
                .subnetMask("255.255.255.0")
                .status(InterfaceStatus.DOWN)
                .adminStatus(InterfaceStatus.DOWN)
                .build());

        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:10:01:01:02", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertFalse(res.isSuccess());
        assertEquals("LINK_DOWN", res.getErrorCode());
    }

    // Scenario 11: Interface operationally down because peer device is OFFLINE
    @Test
    public void testInterfaceOperationallyDownPeerOffline() {
        Project p = projectRepository.save(new Project(null, null, "S11 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.OFFLINE, 100.0, 0.0, null, null)); // Peer OFFLINE

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:11:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:11:01:01:02", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), intf1.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20");
        assertFalse(res.isSuccess());
    }

    // Scenario 12: Duplicate IP detection
    @Test
    public void testDuplicateIpDetection() {
        Project p = projectRepository.save(new Project(null, null, "S12 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:12:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:12:01:01:02", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP)); // Duplicate IP!

        Map<String, Object> validation = validationService.validateProject(p.getId());
        assertFalse((Boolean) validation.get("isValid"));
        List<String> errors = (List<String>) validation.get("errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate IP address")));
    }

    // Scenario 13: TTL expiration (TTL decremented to 0 -> ICMP Time Exceeded)
    @Test
    public void testTtlExpiration() {
        Project p = projectRepository.save(new Project(null, null, "S13 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:13:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface rEth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:13:01:01:02", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface rEth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "00:AA:13:01:01:03", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:13:01:01:04", "192.168.2.10", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), rEth1.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        // Initial TTL set to 1. At Router1, TTL will be decremented to 0!
        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.2.10", "TRACEROUTE", 1);
        assertFalse(res.isSuccess());

        boolean hasTimeExceeded = res.getEvents().stream().anyMatch(e -> "ICMP_TIME_EXCEEDED".equals(e.getType()));
        assertTrue(hasTimeExceeded);
    }

    // Scenario 14: DHCP message structure and format
    @Test
    public void testDhcpMessageFormat() {
        DhcpMessage dhcp = DhcpMessage.builder()
                .messageType(DhcpMessage.TYPE_DISCOVER)
                .clientMac("00:AA:14:01:01:01")
                .build();

        assertEquals(DhcpMessage.TYPE_DISCOVER, dhcp.getMessageType());
        assertEquals("00:AA:14:01:01:01", dhcp.getClientMac());
    }

    // Scenario 15: DNS resolution simulation
    @Test
    public void testDnsResolution() {
        Project p = projectRepository.save(new Project(null, null, "S15 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device srv = deviceRepository.save(new Device(null, p.getId(), "DnsServer", DeviceType.SERVER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:15:01:01:01", "192.168.1.10", "255.255.255.0", null, "192.168.1.53", InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, srv.getId(), "eth0", "00:AA:15:01:01:02", "192.168.1.53", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.53", "DNS", 64);
        assertTrue(res.isSuccess());
        assertEquals("PC1", res.getSource());
    }

    // Scenario 16: TCP 3-Way Handshake
    @Test
    public void testTcpHandshake() {
        Project p = projectRepository.save(new Project(null, null, "S16 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device srv = deviceRepository.save(new Device(null, p.getId(), "Server1", DeviceType.SERVER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:16:01:01:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, srv.getId(), "eth0", "00:AA:16:01:01:02", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));
        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.1.20", "TCP", 64);
        assertTrue(res.isSuccess());

        boolean hasSynAck = res.getEvents().stream().anyMatch(e -> "TCP_SYN_ACK".equals(e.getType()));
        assertTrue(hasSynAck);
    }

    // Scenario 17: HTTP request through multiple network devices
    @Test
    public void testHttpRequestThroughMultipleDevices() {
        Project p = projectRepository.save(new Project(null, null, "S17 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device sw1 = deviceRepository.save(new Device(null, p.getId(), "Switch1", DeviceType.SWITCH, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 200.0, 0.0, null, null));
        Device srv = deviceRepository.save(new Device(null, p.getId(), "WebServer", DeviceType.SERVER, DeviceStatus.ONLINE, 300.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:17:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface swPort1 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port1", "00:AA:17:01:01:02", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface swPort2 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port2", "00:AA:17:01:01:03", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface rEth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:17:01:01:04", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface rEth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "00:AA:17:01:01:05", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, srv.getId(), "eth0", "00:AA:17:01:01:06", "192.168.2.80", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), swPort1.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), swPort2.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), rEth1.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.2.80", "HTTP", 64);
        assertTrue(res.isSuccess());

        boolean hasHttpResp = res.getEvents().stream().anyMatch(e -> "HTTP_RESPONSE".equals(e.getType()));
        assertTrue(hasHttpResp);
    }

    // Scenario 18: VLAN isolation (VLAN 10 vs VLAN 20 frames cannot cross on Layer 2)
    @Test
    public void testVlanIsolation() {
        SwitchingEngine engine = new SwitchingEngine();
        EthernetFrame frameVlan10 = EthernetFrame.builder()
                .sourceMac("00:AA:18:01:01:01")
                .destinationMac("FF:FF:FF:FF:FF:FF")
                .vlanId(10)
                .build();

        // Available ports in VLAN 10 only
        List<String> vlan10Ports = List.of("port1", "port2");
        List<String> egress = engine.processFrame(1L, frameVlan10, "port1", vlan10Ports, 10);

        assertEquals(1, egress.size());
        assertEquals("port2", egress.get(0));
        assertFalse(egress.contains("port3")); // port3 is in VLAN 20, isolated!
    }

    // Scenario 19: Inter-VLAN routing
    @Test
    public void testInterVlanRouting() {
        Project p = projectRepository.save(new Project(null, null, "S19 Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1_VLAN10", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2_VLAN20", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(NetworkInterface.builder()
                .deviceId(pc1.getId()).name("eth0").macAddress("00:AA:19:01:01:01").ipAddress("192.168.10.10").subnetMask("255.255.255.0").gateway("192.168.10.1").vlanId(10).build());

        NetworkInterface rEth0 = interfaceRepository.save(NetworkInterface.builder()
                .deviceId(r1.getId()).name("eth0").macAddress("00:AA:19:01:01:02").ipAddress("192.168.10.1").subnetMask("255.255.255.0").vlanId(10).build());

        NetworkInterface rEth1 = interfaceRepository.save(NetworkInterface.builder()
                .deviceId(r1.getId()).name("eth1").macAddress("00:AA:19:01:01:03").ipAddress("192.168.20.1").subnetMask("255.255.255.0").vlanId(20).build());

        NetworkInterface pc2Intf = interfaceRepository.save(NetworkInterface.builder()
                .deviceId(pc2.getId()).name("eth0").macAddress("00:AA:19:01:01:04").ipAddress("192.168.20.20").subnetMask("255.255.255.0").gateway("192.168.20.1").vlanId(20).build());

        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), rEth1.getId(), pc2Intf.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(p.getId(), pc1.getId(), "192.168.20.20");
        assertTrue(res.isSuccess());
    }

    // Scenario 20: STP loop prevention state verification
    @Test
    public void testStpLoopPrevention() {
        Device sw1 = deviceRepository.save(new Device(null, 1L, "Switch1", DeviceType.SWITCH, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port1", "00:AA:20:01:01:01", null, null, null, null, InterfaceStatus.UP));

        var inspection = stateManager.getDeviceInspection(sw1.getId());
        assertNotNull(inspection.getStpInfo());
        assertEquals("IEEE 802.1D Spanning Tree", inspection.getStpInfo().get("protocol"));
        assertTrue((Boolean) inspection.getStpInfo().get("enabled"));
    }

    // Scenario 21: Hub broadcast to all ports
    @Test
    public void testHubBroadcastToAllPorts() {
        List<String> ports = List.of("port1", "port2", "port3", "port4");
        List<String> egress = hubEngine.processSignal(1L, "port1", ports);
        assertNotNull(egress);
        assertEquals(3, egress.size());
        assertFalse(egress.contains("port1")); // Does not echo back to ingress
        assertTrue(egress.contains("port2"));
        assertTrue(egress.contains("port3"));
        assertTrue(egress.contains("port4"));
    }

    // Scenario 22: Repeater signal collision & regeneration
    @Test
    public void testRepeaterSignalRegeneration() {
        boolean collision = hubEngine.detectCollision(1L, 2);
        assertTrue(collision);
        assertEquals(1, hubEngine.getCollisionCount(1L));
    }

    // Scenario 23: Bridge filtering on same segment
    @Test
    public void testBridgeFilteringSameSegment() {
        EthernetFrame frame = EthernetFrame.builder()
                .sourceMac("00:AA:01:01:01:01")
                .destinationMac("00:AA:01:01:01:02")
                .build();
        bridgeEngine.getBridgeTable(1L).learn("00:AA:01:01:01:02", "port1");
        List<String> egress = bridgeEngine.processFrame(1L, frame, "port1", List.of("port1", "port2"));
        assertTrue(egress.isEmpty()); // Filtered on same port/segment
    }

    // Scenario 24: Bridge forwarding across segments
    @Test
    public void testBridgeForwardingAcrossSegments() {
        EthernetFrame frame = EthernetFrame.builder()
                .sourceMac("00:AA:01:01:01:01")
                .destinationMac("00:AA:01:01:01:03")
                .build();
        bridgeEngine.getBridgeTable(1L).learn("00:AA:01:01:01:03", "port2");
        List<String> egress = bridgeEngine.processFrame(1L, frame, "port1", List.of("port1", "port2"));
        assertEquals(1, egress.size());
        assertEquals("port2", egress.get(0));
    }

    // Scenario 25: Wireless AP client association
    @Test
    public void testWirelessApClientAssociation() {
        wirelessEngine.associateClient(1L, "02:AA:BB:CC:DD:01", "192.168.1.55", "Laptop1");
        var clients = wirelessEngine.getAssociatedClients(1L);
        assertEquals(1, clients.size());
        assertEquals("192.168.1.55", clients.get(0).getClientIp());
        assertNotNull(wirelessEngine.getSupportedStandards());
    }

    // Scenario 26: CSMA/CD backoff after collision
    @Test
    public void testCsmaCdCollisionBackoff() {
        var csmaCd = macControlEngine.simulateCsmaCd(true, 3);
        assertTrue(csmaCd.isCollisionDetected());
        assertNotNull(csmaCd.getJamSignal());
        assertTrue(csmaCd.getBackoffSlots() >= 0);
    }

    // Scenario 27: CSMA/CA RTS/CTS exchange
    @Test
    public void testCsmaCaRtsCtsExchange() {
        var csmaCa = macControlEngine.simulateCsmaCa(false, true);
        assertTrue(csmaCa.isRtsCtsUsed());
        assertTrue(csmaCa.isAckReceived());
        assertEquals("TRANSMITTED", csmaCa.getStatus());
    }

    // Scenario 28: Bandwidth delay calculation
    @Test
    public void testBandwidthDelayCalculation() {
        double serDelay = macControlEngine.calculateTransmissionDelayMs(1500, "100Mbps");
        assertEquals(0.12, serDelay, 0.01);
    }

    // Scenario 29: Pathping multi-hop latency and loss
    @Test
    public void testPathpingMultiHop() {
        Project p = projectRepository.save(new Project(null, null, "Pathping Proj", "Desc", null, null));
        Device pc1 = deviceRepository.save(new Device(null, p.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, p.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, p.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface pcIntf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:AA:29:01:01:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface rEth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:29:01:01:02", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface rEth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "00:AA:29:01:01:03", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface pc2Intf = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:AA:29:01:01:04", "192.168.2.20", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, p.getId(), pcIntf.getId(), rEth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, p.getId(), rEth1.getId(), pc2Intf.getId(), ConnectionStatus.ACTIVE, null));

        var cmdRes = terminalService.executeCommand(pc1.getId(), "pathping 192.168.2.20");
        assertNotNull(cmdRes.getOutput());
        assertTrue(cmdRes.getOutput().contains("Tracing route to 192.168.2.20"));
        assertTrue(cmdRes.getOutput().contains("Computing statistics"));
    }

    // Scenario 30: Switching technique comparison (Circuit vs Packet vs Message)
    @Test
    public void testSwitchingTechniquesComparison() {
        var comp = switchingTechniquesEngine.compareTechniques(1000000, 4, 100_000_000L);
        assertNotNull(comp);
        assertNotNull(comp.getCircuitSwitching());
        assertNotNull(comp.getMessageSwitching());
        assertNotNull(comp.getPacketSwitching());
        assertTrue(comp.getPacketSwitching().getTotalLatencyMs() < comp.getMessageSwitching().getTotalLatencyMs());
    }

    // Scenario 31: Dijkstra shortest path computation
    @Test
    public void testDijkstraShortestPath() {
        List<GraphEdge> edges = List.of(
                GraphEdge.builder().sourceRouter("R1").targetRouter("R2").cost(1).build(),
                GraphEdge.builder().sourceRouter("R2").targetRouter("R3").cost(2).build(),
                GraphEdge.builder().sourceRouter("R1").targetRouter("R3").cost(5).build()
        );
        var result = dijkstraEngine.computeShortestPath("R1", edges);
        assertNotNull(result);
        assertEquals(0, result.getShortestDistances().get("R1"));
        assertEquals(1, result.getShortestDistances().get("R2"));
        assertEquals(3, result.getShortestDistances().get("R3")); // Via R2 (1 + 2 = 3 < 5)
    }

    // Scenario 32: OSPF link-state database sync & SPF
    @Test
    public void testOspfLsdbAndSpf() {
        Device r1 = deviceRepository.save(new Device(null, 1L, "R1", DeviceType.ROUTER, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        NetworkInterface intf = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "00:AA:32:01:01:01", "10.0.0.1", "255.255.255.0", null, null, InterfaceStatus.UP));

        var state = ospfEngine.computeOspf(r1.getId(), r1, List.of(intf), Collections.emptyList());
        assertNotNull(state);
        assertEquals("10.0.0.1", state.getOspfRouterId());
        assertFalse(state.getLsdb().isEmpty());
    }

    // Scenario 33: RIP count-to-infinity prevention (Split Horizon & Poison Reverse)
    @Test
    public void testRipCountToInfinityPrevention() {
        var state = ripEngine.initializeRip(1L, "Router1", List.of(
                RipEngine.RipRouteEntry.builder().destinationNetwork("192.168.1.0/24").subnetMask("255.255.255.0").nextHop("0.0.0.0").outgoingInterface("eth0").hopCount(1).timerSeconds(30).build()
        ));
        assertEquals(1, state.getRoutes().size());

        // Adverting route with 15 hops -> becomes 16 (Infinity)
        ripEngine.processRipUpdate(1L, "eth0", List.of(
                RipEngine.RipRouteEntry.builder().destinationNetwork("10.0.0.0/8").subnetMask("255.0.0.0").nextHop("192.168.1.2").outgoingInterface("eth0").hopCount(15).timerSeconds(30).build()
        ));
        var updated = ripEngine.getRipState(1L);
        var infRoute = updated.getRoutes().stream().filter(r -> "10.0.0.0/8".equals(r.getDestinationNetwork())).findFirst();
        assertTrue(infRoute.isPresent());
        assertEquals(16, infRoute.get().getHopCount()); // Capped at RIP_INFINITY
    }

    // Scenario 34: BGP AS-path routing
    @Test
    public void testBgpAsPathRouting() {
        var state = bgpEngine.configureBgp(1L, "BorderRouter1", 65001, "10.0.0.1");
        bgpEngine.addNeighbor(1L, "192.0.2.1", 65002);
        bgpEngine.advertisePrefix(1L, "203.0.113.0/24", "0.0.0.0", List.of(65001));

        var current = bgpEngine.getBgpState(1L);
        assertEquals(65001, current.getLocalAs());
        assertEquals(1, current.getNeighbors().size());
        assertEquals(1, current.getRoutingTable().size());
        assertEquals(List.of(65001), current.getRoutingTable().get(0).getAsPath());
    }

    // Scenario 35: AODV on-demand route discovery
    @Test
    public void testAodvRouteDiscovery() {
        var rreq = aodvEngine.createRreq("192.168.1.10", "192.168.2.20", 1, 0);
        assertNotNull(rreq);
        assertEquals("192.168.1.10", rreq.getOriginatorIp());
        assertEquals("192.168.2.20", rreq.getDestinationIp());

        var rrep = aodvEngine.createRrep(rreq, "192.168.2.20", 5);
        assertNotNull(rrep);
        assertEquals(0, rrep.getHopCount());

        aodvEngine.updateRoute("192.168.1.10", "192.168.2.20", "192.168.1.1", 2, 5);
        var opt = aodvEngine.lookupRoute("192.168.1.10", "192.168.2.20");
        assertTrue(opt.isPresent());
        assertEquals("192.168.1.1", opt.get().getNextHop());
        assertEquals(2, opt.get().getHopCount());
    }

    // Scenario 36: Transport & Application protocols
    @Test
    public void testTransportAndApplicationProtocols() {
        // DNS
        var dnsRes = applicationProtocolEngine.resolveDns("web.nethub.local");
        assertEquals("192.168.2.80", dnsRes.getResolvedIp());

        // TLS 1.3
        var tlsRes = applicationProtocolEngine.simulateTlsHandshake("Client", "Server");
        assertTrue(tlsRes.isHandshakeSuccessful());
        assertEquals("TLS 1.3", tlsRes.getTlsVersion());

        // FTP
        var ftpRes = applicationProtocolEngine.simulateFtp("192.168.2.10", "anonymous", "RETR", "test.bin");
        assertTrue(ftpRes.isSuccess());
        assertEquals(21, ftpRes.getControlPort());

        // TFTP
        var tftpRes = applicationProtocolEngine.simulateTftp("192.168.2.10", "RRQ", "firmware.bin", 1536);
        assertTrue(tftpRes.getTotalBlocks() > 0);
        assertEquals(3, tftpRes.getTotalBlocks());

        // Email
        var mailRes = applicationProtocolEngine.simulateEmail("mail.nethub.local", "sender@nethub.local", "rcpt@nethub.local", "Subject", "Body");
        assertFalse(mailRes.getSmtpTranscript().isEmpty());
        assertFalse(mailRes.getPop3Transcript().isEmpty());

        // Berkeley Sockets
        var calcRes = socketApiEngine.runUdpCalculator("192.168.1.10", "192.168.2.10", "ADD", 30.0, 12.0);
        assertNotNull(calcRes.getMetrics());

        var echoRes = socketApiEngine.runTcpMultiClientEcho("192.168.2.10", List.of("192.168.1.11", "192.168.1.12"));
        assertNotNull(echoRes.getExecutionLog());
    }

    // Scenario 37: Advanced security & translation (NAT/PAT, ACL, Stateful Firewall)
    @Test
    public void testSecurityAndTranslation() {
        // NAT/PAT
        var session = natEngine.translateOutbound(1L, "203.0.113.1", "192.168.1.10", 49152, "93.184.216.34", 80, "TCP");
        assertNotNull(session);
        assertEquals("203.0.113.1", session.getInsideGlobalIp());
        assertTrue(session.getInsideGlobalPort() >= 10000);

        // Standard ACL
        aclEngine.addRule(1L, AclEngine.AclRule.builder().sequence(10).action("PERMIT").sourceNetwork("192.168.1.0").sourceMask("255.255.255.0").protocol("ANY").build());
        var aclRes = aclEngine.evaluatePacket(1L, "192.168.1.15", "192.168.2.10", "TCP", 80);
        assertTrue(aclRes.isPermitted());

        // Stateful Firewall
        firewallEngine.registerOutboundConnection(1L, "TCP", "192.168.1.10", 54321, "1.1.1.1", 443);
        boolean allowed = firewallEngine.inspectInboundPacket(1L, "TCP", "1.1.1.1", 443, "192.168.1.10", 54321);
        assertTrue(allowed);

        // Unsolicited inbound SYN from external to internal
        boolean blocked = firewallEngine.inspectInboundPacket(1L, "TCP", "203.0.113.99", 60000, "192.168.1.10", 80);
        assertFalse(blocked);
    }

    // OSI & TCP/IP Layer Visualizer test
    @Test
    public void testOsiLayerVisualizer() {
        var trace = osiLayerVisualizer.buildEncapsulationProcess(
                "PC1", "WebServer", "00:AA:01:01:01:01", "00:AA:02:02:02:02",
                "192.168.1.10", "192.168.2.80", 50000, 80, "HTTP", "GET / HTTP/1.1");
        assertNotNull(trace);
        assertEquals(7, trace.getLayers().size());
    }
}

