package com.nethub.utils;

import java.util.regex.Pattern;

public class IpUtils {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    public static boolean isValidSubnetMask(String mask) {
        if (!isValidIPv4(mask)) {
            return false;
        }
        long maskLong = ipToLong(mask);
        // Subnet mask must be contiguous 1s followed by contiguous 0s
        long inverted = ~maskLong & 0xFFFFFFFFL;
        return (inverted & (inverted + 1)) == 0;
    }

    public static long ipToLong(String ip) {
        if (!isValidIPv4(ip)) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }
        String[] parts = ip.trim().split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(parts[i]);
        }
        return result & 0xFFFFFFFFL;
    }

    public static String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }

    public static int maskToCidr(String mask) {
        if (!isValidSubnetMask(mask)) {
            throw new IllegalArgumentException("Invalid subnet mask: " + mask);
        }
        long maskLong = ipToLong(mask);
        return Long.bitCount(maskLong);
    }

    public static String cidrToMask(int cidr) {
        if (cidr < 0 || cidr > 32) {
            throw new IllegalArgumentException("Invalid CIDR prefix: " + cidr);
        }
        long maskLong = cidr == 0 ? 0L : (0xFFFFFFFFL << (32 - cidr)) & 0xFFFFFFFFL;
        return longToIp(maskLong);
    }

    public static String getNetworkIp(String ip, String mask) {
        long ipLong = ipToLong(ip);
        long maskLong = ipToLong(mask);
        long networkLong = ipLong & maskLong;
        return longToIp(networkLong);
    }

    public static String getBroadcastIp(String ip, String mask) {
        long ipLong = ipToLong(ip);
        long maskLong = ipToLong(mask);
        long broadcastLong = ipLong | (~maskLong & 0xFFFFFFFFL);
        return longToIp(broadcastLong);
    }

    public static boolean isIpInSubnet(String ip, String networkIp, String mask) {
        if (!isValidIPv4(ip) || !isValidIPv4(networkIp) || !isValidSubnetMask(mask)) {
            return false;
        }
        long targetNet = ipToLong(getNetworkIp(ip, mask));
        long baseNet = ipToLong(getNetworkIp(networkIp, mask));
        return targetNet == baseNet;
    }

    public static boolean inSameSubnet(String ip1, String mask1, String ip2, String mask2) {
        if (!isValidIPv4(ip1) || !isValidIPv4(ip2) || !isValidSubnetMask(mask1) || !isValidSubnetMask(mask2)) {
            return false;
        }
        // If masks match, compare calculated network addresses
        if (mask1.trim().equals(mask2.trim())) {
            return getNetworkIp(ip1, mask1).equals(getNetworkIp(ip2, mask2));
        }
        // Otherwise check if ip2 is in ip1's subnet and ip1 is in ip2's subnet
        return isIpInSubnet(ip2, ip1, mask1) && isIpInSubnet(ip1, ip2, mask2);
    }

    public static boolean isLocal(String sourceIp, String sourceMask, String destIp) {
        if (!isValidIPv4(sourceIp) || !isValidSubnetMask(sourceMask) || !isValidIPv4(destIp)) {
            return false;
        }
        return isIpInSubnet(destIp, sourceIp, sourceMask);
    }
}
