package com.nethub.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ArpTable {
    private final Map<String, ArpEntry> cache = new ConcurrentHashMap<>();

    public void put(String ipAddress, String macAddress) {
        put(ipAddress, macAddress, ArpEntry.STATE_REACHABLE, "eth0");
    }

    public void put(String ipAddress, String macAddress, String state, String interfaceName) {
        if (ipAddress == null || macAddress == null) return;
        String normalizedIp = ipAddress.trim();
        String normalizedMac = macAddress.toUpperCase().trim();
        
        ArpEntry entry = cache.get(normalizedIp);
        if (entry == null) {
            cache.put(normalizedIp, ArpEntry.builder()
                    .ipAddress(normalizedIp)
                    .macAddress(normalizedMac)
                    .state(state != null ? state : ArpEntry.STATE_REACHABLE)
                    .interfaceName(interfaceName != null ? interfaceName : "eth0")
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            entry.setMacAddress(normalizedMac);
            entry.setState(state != null ? state : ArpEntry.STATE_REACHABLE);
            if (interfaceName != null) {
                entry.setInterfaceName(interfaceName);
            }
            entry.setTimestamp(LocalDateTime.now());
        }
    }

    public void markIncomplete(String ipAddress, String interfaceName) {
        if (ipAddress == null) return;
        String normalizedIp = ipAddress.trim();
        cache.put(normalizedIp, ArpEntry.builder()
                .ipAddress(normalizedIp)
                .macAddress("INCOMPLETE")
                .state(ArpEntry.STATE_INCOMPLETE)
                .interfaceName(interfaceName != null ? interfaceName : "eth0")
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void markFailed(String ipAddress) {
        if (ipAddress == null) return;
        String normalizedIp = ipAddress.trim();
        ArpEntry entry = cache.get(normalizedIp);
        if (entry != null) {
            entry.setState(ArpEntry.STATE_FAILED);
            entry.setTimestamp(LocalDateTime.now());
        }
    }

    public Optional<ArpEntry> lookup(String ipAddress) {
        if (ipAddress == null) return Optional.empty();
        ArpEntry entry = cache.get(ipAddress.trim());
        if (entry != null && !ArpEntry.STATE_FAILED.equalsIgnoreCase(entry.getState())) {
            return Optional.of(entry);
        }
        return Optional.empty();
    }

    public void removeExpired(long maxAgeSeconds) {
        cache.entrySet().removeIf(e -> e.getValue().isExpired(maxAgeSeconds));
    }

    public List<ArpEntry> getAllEntries() {
        return new ArrayList<>(cache.values());
    }

    public void clear() {
        cache.clear();
    }
}
