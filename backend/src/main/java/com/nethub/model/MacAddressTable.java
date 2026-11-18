package com.nethub.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class MacAddressTable {
    private final Map<String, MacTableEntry> entries = new ConcurrentHashMap<>();

    public void learn(String macAddress, String port) {
        if (macAddress == null || macAddress.isEmpty() || port == null) {
            return;
        }
        String normalizedMac = macAddress.toUpperCase().trim();
        MacTableEntry entry = entries.get(normalizedMac);
        if (entry == null) {
            entries.put(normalizedMac, MacTableEntry.builder()
                    .macAddress(normalizedMac)
                    .port(port)
                    .status("LEARNED")
                    .lastSeen(LocalDateTime.now())
                    .build());
        } else {
            entry.setPort(port);
            entry.setLastSeen(LocalDateTime.now());
        }
    }

    public Optional<MacTableEntry> lookup(String macAddress) {
        if (macAddress == null) return Optional.empty();
        return Optional.ofNullable(entries.get(macAddress.toUpperCase().trim()));
    }

    public List<MacTableEntry> getAllEntries() {
        return new ArrayList<>(entries.values());
    }

    public void clear() {
        entries.clear();
    }
}
