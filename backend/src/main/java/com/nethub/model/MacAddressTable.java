package com.nethub.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class MacAddressTable {
    // Key: VLAN:MAC, e.g. "1:00:11:22:33:44:55"
    private final Map<String, MacTableEntry> entries = new ConcurrentHashMap<>();

    private String buildKey(String macAddress, Integer vlanId) {
        int vlan = (vlanId != null && vlanId > 0) ? vlanId : 1;
        return vlan + ":" + macAddress.toUpperCase().trim();
    }

    public void learn(String macAddress, String port) {
        learn(macAddress, port, 1);
    }

    public void learn(String macAddress, String port, Integer vlanId) {
        if (macAddress == null || macAddress.isEmpty() || port == null) {
            return;
        }
        int vlan = (vlanId != null && vlanId > 0) ? vlanId : 1;
        String key = buildKey(macAddress, vlan);
        String normalizedMac = macAddress.toUpperCase().trim();
        
        MacTableEntry entry = entries.get(key);
        if (entry == null) {
            entries.put(key, MacTableEntry.builder()
                    .macAddress(normalizedMac)
                    .port(port)
                    .vlanId(vlan)
                    .status("LEARNED")
                    .lastSeen(LocalDateTime.now())
                    .build());
        } else {
            entry.setPort(port);
            entry.setVlanId(vlan);
            entry.setLastSeen(LocalDateTime.now());
        }
    }

    public Optional<MacTableEntry> lookup(String macAddress) {
        return lookup(macAddress, 1);
    }

    public Optional<MacTableEntry> lookup(String macAddress, Integer vlanId) {
        if (macAddress == null) return Optional.empty();
        int vlan = (vlanId != null && vlanId > 0) ? vlanId : 1;
        MacTableEntry entry = entries.get(buildKey(macAddress, vlan));
        if (entry != null) {
            return Optional.of(entry);
        }
        // Fallback to any VLAN match if not found in specific VLAN
        for (MacTableEntry e : entries.values()) {
            if (e.getMacAddress().equalsIgnoreCase(macAddress.trim())) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public void removeExpired(long maxAgeSeconds) {
        entries.entrySet().removeIf(e -> e.getValue().isExpired(maxAgeSeconds));
    }

    public List<MacTableEntry> getAllEntries() {
        return new ArrayList<>(entries.values());
    }

    public void clear() {
        entries.clear();
    }
}
