package com.nethub;

import com.nethub.entity.*;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.repository.*;
import com.nethub.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DemonstrationScenarioTest {

    @Autowired
    private DemonstrationScenarioRepository scenarioRepository;

    @Autowired
    private DemonstrationScenarioSeeder scenarioSeeder;

    @Autowired
    private DemonstrationScenarioService scenarioService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NetworkInterfaceRepository interfaceRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private InterfaceService interfaceService;

    @BeforeEach
    void setUp() {
        // Run seeder to ensure all scenarios are present
        scenarioSeeder.seedAllScenarios();
    }

    @Test
    void testSeederSeedsAll32Scenarios() {
        List<DemonstrationScenario> allScenarios = scenarioRepository.findAll();
        assertEquals(32, allScenarios.size(), "Should seed exactly 32 scenarios (14 valid + 18 invalid)");

        List<DemonstrationScenario> validScenarios = scenarioRepository.findByCategoryOrderByCodeAsc("VALID");
        assertEquals(14, validScenarios.size(), "Should have exactly 14 valid scenarios");

        List<DemonstrationScenario> invalidScenarios = scenarioRepository.findByCategoryOrderByCodeAsc("INVALID");
        assertEquals(18, invalidScenarios.size(), "Should have exactly 18 invalid scenarios");

        // Verify key codes exist
        assertTrue(scenarioRepository.existsByCode("V1"), "V1 Hub scenario must exist");
        assertTrue(scenarioRepository.existsByCode("V5"), "V5 Router scenario must exist");
        assertTrue(scenarioRepository.existsByCode("V14"), "V14 Comparison scenario must exist");
        assertTrue(scenarioRepository.existsByCode("I1"), "I1 Duplicate IP scenario must exist");
        assertTrue(scenarioRepository.existsByCode("I3"), "I3 Invalid Gateway scenario must exist");
        assertTrue(scenarioRepository.existsByCode("I10"), "I10 Routing loop scenario must exist");
        assertTrue(scenarioRepository.existsByCode("I18"), "I18 OSPF misconfiguration scenario must exist");
    }

    @Test
    void testSeedingIsIdempotent() {
        long initialCount = scenarioRepository.count();
        assertEquals(32, initialCount);

        // Run seeder a second time
        scenarioSeeder.seedAllScenarios();

        long afterCount = scenarioRepository.count();
        assertEquals(32, afterCount, "Seeding again must not create duplicate scenarios");
    }

    @Test
    void testLoadScenarioIntoNewProject() {
        DemonstrationScenario v5 = scenarioRepository.findByCode("V5")
                .orElseThrow(() -> new AssertionError("V5 scenario not found"));

        Project clonedProject = scenarioService.loadScenarioIntoProject(v5.getId(), null);
        assertNotNull(clonedProject);
        assertNotNull(clonedProject.getId());
        assertNotEquals(v5.getTemplateProjectId(), clonedProject.getId());

        List<Device> devices = deviceRepository.findByProjectId(clonedProject.getId());
        assertEquals(7, devices.size(), "V5 should have 7 devices (1 Router, 2 Switches, 4 PCs)");
    }

    @Test
    void testLoadScenarioIntoExistingProject() {
        Project targetProject = new Project();
        targetProject.setName("User Workspace");
        targetProject.setDescription("Target project for loading");
        targetProject = projectRepository.save(targetProject);

        DemonstrationScenario v1 = scenarioRepository.findByCode("V1")
                .orElseThrow(() -> new AssertionError("V1 scenario not found"));

        Project result = scenarioService.loadScenarioIntoProject(v1.getId(), targetProject.getId());
        assertEquals(targetProject.getId(), result.getId());

        List<Device> devices = deviceRepository.findByProjectId(targetProject.getId());
        assertEquals(4, devices.size(), "V1 should have 4 devices (1 Hub, 3 PCs)");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidScenarioPassesValidation() {
        DemonstrationScenario v5 = scenarioRepository.findByCode("V5")
                .orElseThrow(() -> new AssertionError("V5 scenario not found"));

        Project project = scenarioService.loadScenarioIntoProject(v5.getId(), null);
        Map<String, Object> valResult = validationService.validateProject(project.getId());

        assertTrue((Boolean) valResult.get("isValid"), "Valid scenario V5 must pass validation with 0 errors");
        List<String> errors = (List<String>) valResult.get("errors");
        assertTrue(errors.isEmpty(), "Error count must be 0 for valid scenario");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInvalidScenarioDuplicateIpFailsValidation() {
        DemonstrationScenario i1 = scenarioRepository.findByCode("I1")
                .orElseThrow(() -> new AssertionError("I1 scenario not found"));

        Project project = scenarioService.loadScenarioIntoProject(i1.getId(), null);
        Map<String, Object> valResult = validationService.validateProject(project.getId());

        assertFalse((Boolean) valResult.get("isValid"), "Invalid scenario I1 must fail validation");
        List<String> errors = (List<String>) valResult.get("errors");
        assertTrue(errors.stream()
                .anyMatch(err -> err.toLowerCase().contains("duplicate ip")),
                "Should report duplicate IP address issue");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInvalidScenarioInvalidGatewayFailsValidation() {
        DemonstrationScenario i3 = scenarioRepository.findByCode("I3")
                .orElseThrow(() -> new AssertionError("I3 scenario not found"));

        Project project = scenarioService.loadScenarioIntoProject(i3.getId(), null);
        Map<String, Object> valResult = validationService.validateProject(project.getId());

        assertFalse((Boolean) valResult.get("isValid"), "Invalid scenario I3 must fail validation");
        List<String> errors = (List<String>) valResult.get("errors");
        assertTrue(errors.stream()
                .anyMatch(err -> err.toLowerCase().contains("gateway")),
                "Should report invalid gateway issue");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInvalidScenarioDuplicateMacFailsValidation() {
        DemonstrationScenario i16 = scenarioRepository.findByCode("I16")
                .orElseThrow(() -> new AssertionError("I16 scenario not found"));

        Project project = scenarioService.loadScenarioIntoProject(i16.getId(), null);
        Map<String, Object> valResult = validationService.validateProject(project.getId());

        assertFalse((Boolean) valResult.get("isValid"), "Invalid scenario I16 must fail validation");
        List<String> errors = (List<String>) valResult.get("errors");
        assertTrue(errors.stream()
                .anyMatch(err -> err.toLowerCase().contains("duplicate mac")),
                "Should report duplicate MAC address issue");
    }

    @Test
    void testAutomaticFieldDefaultsOnDeviceAndInterface() {
        Project project = new Project();
        project.setName("Test Defaults Project");
        project = projectRepository.save(project);

        Device dev = new Device();
        dev.setName("Workstation-1");
        dev.setType(DeviceType.PC);
        Device savedDev = deviceService.createDevice(project.getId(), dev);

        // Hostname and status populated automatically
        assertEquals("workstation-1.local", savedDev.getHostname());
        assertNotNull(savedDev.getStatus());

        // Interface creation defaults
        NetworkInterface iface = new NetworkInterface();
        NetworkInterface savedIface = interfaceService.createInterface(savedDev.getId(), iface);

        assertEquals(1500, savedIface.getMtu());
        assertEquals("1Gbps", savedIface.getSpeed());
        assertEquals("FULL", savedIface.getDuplex());
        assertEquals("255.255.255.0", savedIface.getSubnetMask());
        assertEquals(InterfaceStatus.UP, savedIface.getStatus());
        assertNotNull(savedIface.getMacAddress(), "MAC address should be auto-generated");
        assertNull(savedIface.getIpAddress(), "IP address should remain unassigned for user input");
    }
}
