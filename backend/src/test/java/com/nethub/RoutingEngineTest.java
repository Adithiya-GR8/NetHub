package com.nethub;

import com.nethub.engine.RoutingEngine;
import com.nethub.entity.enums.RouteType;
import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingEngineTest {

    private RoutingEngine routingEngine;

    @BeforeEach
    public void setUp() {
        routingEngine = new RoutingEngine();
    }

    @Test
    public void testLongestPrefixMatching() {
        RoutingTable table = new RoutingTable();

        // Route 1: 192.168.1.0 /24
        table.addRoute(RouteEntry.builder()
                .destinationNetwork("192.168.1.0")
                .subnetMask("255.255.255.0")
                .outgoingInterface("eth0")
                .metric(1)
                .routeType(RouteType.STATIC)
                .build());

        // Route 2: 192.168.1.48 /28 (More specific match for 192.168.1.50)
        table.addRoute(RouteEntry.builder()
                .destinationNetwork("192.168.1.48")
                .subnetMask("255.255.255.240")
                .outgoingInterface("eth1")
                .metric(1)
                .routeType(RouteType.STATIC)
                .build());

        Optional<RouteEntry> match = routingEngine.lookupBestRoute(table, "192.168.1.50");
        assertTrue(match.isPresent());
        assertEquals("eth1", match.get().getOutgoingInterface());
        assertEquals(28, match.get().getPrefixLength());
    }

    @Test
    public void testNoMatchingRoute() {
        RoutingTable table = new RoutingTable();

        table.addRoute(RouteEntry.builder()
                .destinationNetwork("10.0.0.0")
                .subnetMask("255.0.0.0")
                .outgoingInterface("eth0")
                .metric(1)
                .routeType(RouteType.CONNECTED)
                .build());

        Optional<RouteEntry> match = routingEngine.lookupBestRoute(table, "192.168.1.10");
        assertTrue(match.isEmpty());
    }
}
