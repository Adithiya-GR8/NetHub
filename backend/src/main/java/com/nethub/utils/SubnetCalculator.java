package com.nethub.utils;

import com.nethub.model.Subnet;

public class SubnetCalculator {

    public static Subnet calculate(String ip, String mask) {
        if (!IpUtils.isValidIPv4(ip)) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }
        if (!IpUtils.isValidSubnetMask(mask)) {
            throw new IllegalArgumentException("Invalid subnet mask: " + mask);
        }

        int cidr = IpUtils.maskToCidr(mask);
        String networkAddress = IpUtils.getNetworkIp(ip, mask);
        String broadcastAddress = IpUtils.getBroadcastIp(ip, mask);

        long netLong = IpUtils.ipToLong(networkAddress);
        long bcastLong = IpUtils.ipToLong(broadcastAddress);

        long usableHostCount;
        String firstUsable;
        String lastUsable;

        if (cidr >= 31) {
            // Special cases: /31 (point-to-point RFC 3021) or /32 (single host)
            if (cidr == 32) {
                usableHostCount = 1;
                firstUsable = networkAddress;
                lastUsable = networkAddress;
            } else { // /31
                usableHostCount = 2;
                firstUsable = networkAddress;
                lastUsable = broadcastAddress;
            }
        } else {
            usableHostCount = (bcastLong - netLong - 1);
            firstUsable = IpUtils.longToIp(netLong + 1);
            lastUsable = IpUtils.longToIp(bcastLong - 1);
        }

        return Subnet.builder()
                .ipAddress(ip)
                .subnetMask(mask)
                .cidr(cidr)
                .networkAddress(networkAddress)
                .broadcastAddress(broadcastAddress)
                .firstUsableAddress(firstUsable)
                .lastUsableAddress(lastUsable)
                .usableHostCount(usableHostCount)
                .build();
    }

    public static String getNetworkAddress(String ip, String mask) {
        return IpUtils.getNetworkIp(ip, mask);
    }

    public static String getBroadcastAddress(String ip, String mask) {
        return IpUtils.getBroadcastIp(ip, mask);
    }

    public static String getFirstUsableAddress(String ip, String mask) {
        return calculate(ip, mask).getFirstUsableAddress();
    }

    public static String getLastUsableAddress(String ip, String mask) {
        return calculate(ip, mask).getLastUsableAddress();
    }

    public static long getUsableHostCount(String mask) {
        int cidr = IpUtils.maskToCidr(mask);
        if (cidr == 32) return 1;
        if (cidr == 31) return 2;
        return (1L << (32 - cidr)) - 2;
    }
}
