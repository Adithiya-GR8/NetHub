package com.nethub.utils;

import java.util.Random;

public class MACAddressGenerator {
    private static final Random random = new Random();

    public static String generateMAC() {
        byte[] mac = new byte[6];
        random.nextBytes(mac);
        // Set locally administered bit and clear multicast bit
        mac[0] = (byte) ((mac[0] | 2) & 254);
        
        StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
            if (i < mac.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
