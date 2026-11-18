package com.nethub;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
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
}

