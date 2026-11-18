package com.nethub;

import com.nethub.utils.IpUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IpUtilsTest {

    @Test
    public void testValidIPv4() {
        assertTrue(IpUtils.isValidIPv4("192.168.1.10"));
        assertTrue(IpUtils.isValidIPv4("10.0.0.1"));
        assertTrue(IpUtils.isValidIPv4("255.255.255.255"));
        assertFalse(IpUtils.isValidIPv4("256.1.1.1"));
        assertFalse(IpUtils.isValidIPv4("192.168.1"));
        assertFalse(IpUtils.isValidIPv4("invalid"));
    }

    @Test
    public void testSubnetMaskValidation() {
        assertTrue(IpUtils.isValidSubnetMask("255.255.255.0"));
        assertTrue(IpUtils.isValidSubnetMask("255.255.0.0"));
        assertTrue(IpUtils.isValidSubnetMask("255.255.255.240"));
        assertFalse(IpUtils.isValidSubnetMask("255.255.255.1"));
        assertFalse(IpUtils.isValidSubnetMask("255.0.255.0"));
    }

    @Test
    public void testCidrConversion() {
        assertEquals(24, IpUtils.maskToCidr("255.255.255.0"));
        assertEquals(16, IpUtils.maskToCidr("255.255.0.0"));
        assertEquals(8, IpUtils.maskToCidr("255.0.0.0"));
        assertEquals("255.255.255.0", IpUtils.cidrToMask(24));
        assertEquals("255.255.0.0", IpUtils.cidrToMask(16));
    }

    @Test
    public void testNetworkAndBroadcastAddress() {
        assertEquals("192.168.1.0", IpUtils.getNetworkIp("192.168.1.10", "255.255.255.0"));
        assertEquals("192.168.1.255", IpUtils.getBroadcastIp("192.168.1.10", "255.255.255.0"));
    }

    @Test
    public void testSameSubnet() {
        assertTrue(IpUtils.inSameSubnet("192.168.1.10", "255.255.255.0", "192.168.1.20", "255.255.255.0"));
        assertFalse(IpUtils.inSameSubnet("192.168.1.10", "255.255.255.0", "192.168.2.20", "255.255.255.0"));
    }

    @Test
    public void testIsLocal() {
        assertTrue(IpUtils.isLocal("192.168.1.10", "255.255.255.0", "192.168.1.20"));
        assertFalse(IpUtils.isLocal("192.168.1.10", "255.255.255.0", "192.168.2.10"));
    }
}
