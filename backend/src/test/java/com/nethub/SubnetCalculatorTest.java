package com.nethub;

import com.nethub.model.Subnet;
import com.nethub.utils.SubnetCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubnetCalculatorTest {

    @Test
    public void testCalculateSubnet24() {
        Subnet subnet = SubnetCalculator.calculate("192.168.1.10", "255.255.255.0");
        assertEquals(24, subnet.getCidr());
        assertEquals("192.168.1.0", subnet.getNetworkAddress());
        assertEquals("192.168.1.255", subnet.getBroadcastAddress());
        assertEquals("192.168.1.1", subnet.getFirstUsableAddress());
        assertEquals("192.168.1.254", subnet.getLastUsableAddress());
        assertEquals(254, subnet.getUsableHostCount());
    }

    @Test
    public void testCalculateSubnet30() {
        Subnet subnet = SubnetCalculator.calculate("10.0.0.1", "255.255.255.252");
        assertEquals(30, subnet.getCidr());
        assertEquals("10.0.0.0", subnet.getNetworkAddress());
        assertEquals("10.0.0.3", subnet.getBroadcastAddress());
        assertEquals("10.0.0.1", subnet.getFirstUsableAddress());
        assertEquals("10.0.0.2", subnet.getLastUsableAddress());
        assertEquals(2, subnet.getUsableHostCount());
    }
}
