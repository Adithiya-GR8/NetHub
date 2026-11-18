package com.nethub;

import com.nethub.engine.ArpEngine;
import com.nethub.model.ArpPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ArpEngineTest {

    private ArpEngine arpEngine;

    @BeforeEach
    public void setUp() {
        arpEngine = new ArpEngine();
        arpEngine.resetAll();
    }

    @Test
    public void testArpRequestAndReply() {
        Long dev1 = 10L;
        Long dev2 = 20L;

        // Dev1 creates ARP Request for 192.168.1.20
        ArpPacket req = arpEngine.createArpRequest("AA:01", "192.168.1.10", "192.168.1.20");
        assertEquals(ArpPacket.OP_REQUEST, req.getOperation());
        assertEquals("192.168.1.20", req.getTargetIp());

        // Dev2 sends ARP Reply
        ArpPacket reply = arpEngine.createArpReply("BB:02", "192.168.1.20", "AA:01", "192.168.1.10");
        assertEquals(ArpPacket.OP_REPLY, reply.getOperation());
        assertEquals("BB:02", reply.getSenderMac());

        // Update Dev1 ARP Cache
        arpEngine.updateArpCache(dev1, "192.168.1.20", "BB:02");

        // Lookup Cache
        Optional<String> macOpt = arpEngine.lookupMac(dev1, "192.168.1.20");
        assertTrue(macOpt.isPresent());
        assertEquals("BB:02", macOpt.get());
    }
}
