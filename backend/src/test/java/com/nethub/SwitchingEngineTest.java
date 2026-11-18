package com.nethub;

import com.nethub.engine.SwitchingEngine;
import com.nethub.model.EthernetFrame;
import com.nethub.model.MacAddressTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SwitchingEngineTest {

    private SwitchingEngine switchingEngine;

    @BeforeEach
    public void setUp() {
        switchingEngine = new SwitchingEngine();
        switchingEngine.resetAll();
    }

    @Test
    public void testMacLearningAndKnownForwarding() {
        Long switchId = 1L;
        List<String> ports = List.of("port1", "port2", "port3");

        // Frame 1: Src AA:01 on port1 -> Dst BB:02 (Unknown)
        EthernetFrame frame1 = EthernetFrame.builder()
                .sourceMac("AA:BB:CC:DD:EE:01")
                .destinationMac("AA:BB:CC:DD:EE:02")
                .etherType(EthernetFrame.ETHERTYPE_IPV4)
                .build();

        List<String> targets1 = switchingEngine.processFrame(switchId, frame1, "port1", ports);
        // Floods to port2 and port3
        assertEquals(2, targets1.size());
        assertTrue(targets1.contains("port2"));
        assertTrue(targets1.contains("port3"));

        // Verify AA:01 is learned on port1
        MacAddressTable macTable = switchingEngine.getMacTable(switchId);
        assertTrue(macTable.lookup("AA:BB:CC:DD:EE:01").isPresent());
        assertEquals("port1", macTable.lookup("AA:BB:CC:DD:EE:01").get().getPort());

        // Frame 2: Src BB:02 on port2 -> Dst AA:01 (Known on port1)
        EthernetFrame frame2 = EthernetFrame.builder()
                .sourceMac("AA:BB:CC:DD:EE:02")
                .destinationMac("AA:BB:CC:DD:EE:01")
                .etherType(EthernetFrame.ETHERTYPE_IPV4)
                .build();

        List<String> targets2 = switchingEngine.processFrame(switchId, frame2, "port2", ports);
        // Forwards ONLY to port1
        assertEquals(1, targets2.size());
        assertEquals("port1", targets2.get(0));
    }

    @Test
    public void testBroadcastFlooding() {
        Long switchId = 1L;
        List<String> ports = List.of("port1", "port2", "port3");

        EthernetFrame bcastFrame = EthernetFrame.builder()
                .sourceMac("AA:BB:CC:DD:EE:01")
                .destinationMac(EthernetFrame.BROADCAST_MAC)
                .etherType(EthernetFrame.ETHERTYPE_ARP)
                .build();

        List<String> targets = switchingEngine.processFrame(switchId, bcastFrame, "port1", ports);
        assertEquals(2, targets.size());
        assertTrue(targets.contains("port2"));
        assertTrue(targets.contains("port3"));
        assertFalse(targets.contains("port1"));
    }
}
