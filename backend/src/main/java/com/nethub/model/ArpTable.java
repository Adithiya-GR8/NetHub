package com.nethub.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ArpTable {
    private final Map<String, ArpEntry> cache = new ConcurrentHashMap<>();

    public void put(String ipAddress, String macAddress) {
        if (ipAddress == null || macAddress == null) return;
        String normalizedIp = ipAddress.trim();
        String normalizedMac = macAddress.toUpperCase().trim();
        
        ArpEntry entry = cache.get(normalizedIp);
        if (entry == null) {
            cache.put(normalizedIp, ArpEntry.builder()
                    .ipAddress(normalizedIp)
                    .macAddress(normalizedMac)
                    .state("REACHABLE")
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            entry.setMacAddress(normalizedMac);
            entry.setState("REACHABLE");
            entry.setTimestamp(LocalDateTime.now());
        }
    }

    public Optional<ArpEntry> lookup(String ipAddress) {
        if (ipAddress == null) return Optional.empty();
        return Optional.ofNullable(cache.get(ipAddress.trim()));
    }

    public List<ArpEntry> getAllEntries() {
        return new ArrayList<>(cache.values());
    }

    public void clear() {
        cache.clear();
    }
}
