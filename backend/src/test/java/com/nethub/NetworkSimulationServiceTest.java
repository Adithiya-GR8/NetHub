package com.nethub;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.NetworkPathFinder;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.entity.Connection;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.Project;
import com.nethub.entity.enums.ConnectionStatus;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.model.SimulationResult;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.ProjectRepository;
import com.nethub.service.NetworkSimulationService;
import com.nethub.service.RoutingTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NetworkSimulationServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NetworkInterfaceRepository interfaceRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

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

    private NetworkSimulationService simulationService;

    @BeforeEach
    public void setUp() {
        switchingEngine.resetAll();
        arpEngine.resetAll();
        simulationService = new NetworkSimulationService(
                deviceRepository, interfaceRepository, connectionRepository,
                routingTableService, routingEngine, switchingEngine, arpEngine, pathFinder);
    }

    @Test
    public void testLayer2LocalSubnetTransmission() {
        // Topology: PC1 -- Switch1 -- PC2
        Project proj = projectRepository.save(new Project(null, null, "L2 Test", "Desc", null, null));

        Device pc1 = deviceRepository.save(new Device(null, proj.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device sw1 = deviceRepository.save(new Device(null, proj.getId(), "Switch1", DeviceType.SWITCH, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device pc2 = deviceRepository.save(new Device(null, proj.getId(), "PC2", DeviceType.PC, DeviceStatus.ONLINE, 200.0, 0.0, null, null));

        NetworkInterface intf1 = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "00:11:22:33:44:01", "192.168.1.10", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface swPort1 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port1", "00:11:22:33:44:02", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface swPort2 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port2", "00:11:22:33:44:03", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface intf2 = interfaceRepository.save(new NetworkInterface(null, pc2.getId(), "eth0", "00:11:22:33:44:04", "192.168.1.20", "255.255.255.0", null, null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, proj.getId(), intf1.getId(), swPort1.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, proj.getId(), swPort2.getId(), intf2.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(proj.getId(), pc1.getId(), "192.168.1.20");

        assertTrue(res.isSuccess());
        assertEquals("PC1", res.getSource());
        assertEquals("192.168.1.20", res.getDestination());
        assertNotNull(res.getEvents());
        assertFalse(res.getEvents().isEmpty());
    }

    @Test
    public void testLayer3RoutedSubnetTransmission() {
        // Topology: PC1 -- Switch1 -- Router1 -- Switch2 -- Server1
        Project proj = projectRepository.save(new Project(null, null, "L3 Test", "Desc", null, null));

        Device pc1 = deviceRepository.save(new Device(null, proj.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        Device sw1 = deviceRepository.save(new Device(null, proj.getId(), "Switch1", DeviceType.SWITCH, DeviceStatus.ONLINE, 100.0, 0.0, null, null));
        Device r1  = deviceRepository.save(new Device(null, proj.getId(), "Router1", DeviceType.ROUTER, DeviceStatus.ONLINE, 200.0, 0.0, null, null));
        Device sw2 = deviceRepository.save(new Device(null, proj.getId(), "Switch2", DeviceType.SWITCH, DeviceStatus.ONLINE, 300.0, 0.0, null, null));
        Device srv = deviceRepository.save(new Device(null, proj.getId(), "Server1", DeviceType.SERVER, DeviceStatus.ONLINE, 400.0, 0.0, null, null));

        NetworkInterface pc1Intf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "AA:BB:CC:DD:EE:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));
        NetworkInterface sw1Port1 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port1", "AA:BB:CC:DD:EE:02", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface sw1Port2 = interfaceRepository.save(new NetworkInterface(null, sw1.getId(), "port2", "AA:BB:CC:DD:EE:03", null, null, null, null, InterfaceStatus.UP));

        NetworkInterface r1Eth0 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth0", "AA:BB:CC:DD:EE:04", "192.168.1.1", "255.255.255.0", null, null, InterfaceStatus.UP));
        NetworkInterface r1Eth1 = interfaceRepository.save(new NetworkInterface(null, r1.getId(), "eth1", "AA:BB:CC:DD:EE:05", "192.168.2.1", "255.255.255.0", null, null, InterfaceStatus.UP));

        NetworkInterface sw2Port1 = interfaceRepository.save(new NetworkInterface(null, sw2.getId(), "port1", "AA:BB:CC:DD:EE:06", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface sw2Port2 = interfaceRepository.save(new NetworkInterface(null, sw2.getId(), "port2", "AA:BB:CC:DD:EE:07", null, null, null, null, InterfaceStatus.UP));
        NetworkInterface srvIntf = interfaceRepository.save(new NetworkInterface(null, srv.getId(), "eth0", "AA:BB:CC:DD:EE:08", "192.168.2.10", "255.255.255.0", "192.168.2.1", null, InterfaceStatus.UP));

        connectionRepository.save(new Connection(null, proj.getId(), pc1Intf.getId(), sw1Port1.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, proj.getId(), sw1Port2.getId(), r1Eth0.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, proj.getId(), r1Eth1.getId(), sw2Port1.getId(), ConnectionStatus.ACTIVE, null));
        connectionRepository.save(new Connection(null, proj.getId(), sw2Port2.getId(), srvIntf.getId(), ConnectionStatus.ACTIVE, null));

        SimulationResult res = simulationService.simulateTransmission(proj.getId(), pc1.getId(), "192.168.2.10");

        assertTrue(res.isSuccess());
        assertEquals("PC1", res.getSource());
        assertEquals("192.168.2.10", res.getDestination());
    }

    @Test
    public void testFailureNoRoute() {
        Project proj = projectRepository.save(new Project(null, null, "Failure Test", "Desc", null, null));

        Device pc1 = deviceRepository.save(new Device(null, proj.getId(), "PC1", DeviceType.PC, DeviceStatus.ONLINE, 0.0, 0.0, null, null));
        NetworkInterface pc1Intf = interfaceRepository.save(new NetworkInterface(null, pc1.getId(), "eth0", "AA:BB:CC:DD:EE:01", "192.168.1.10", "255.255.255.0", "192.168.1.1", null, InterfaceStatus.UP));

        SimulationResult res = simulationService.simulateTransmission(proj.getId(), pc1.getId(), "10.10.10.10");

        assertFalse(res.isSuccess());
        assertNotNull(res.getErrorCode());
    }
}
