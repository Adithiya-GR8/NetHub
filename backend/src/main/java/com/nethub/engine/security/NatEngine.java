package com.nethub.engine.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NatEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NatTranslationEntry {
        private String protocol; // TCP, UDP, ICMP
        private String insideLocalIp;
        private int insideLocalPort;
        private String insideGlobalIp;
        private int insideGlobalPort;
        private String outsideLocalIp;
        private int outsideLocalPort;
        private String outsideGlobalIp;
        private int outsideGlobalPort;
        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();
    }

    // Router ID -> List<NatTranslationEntry>
    private final Map<Long, List<NatTranslationEntry>> natTables = new ConcurrentHashMap<>();
    private final AtomicInteger nextAllocatedPort = new AtomicInteger(10000);

    public NatTranslationEntry translateOutbound(Long routerId, String insideGlobalWanIp, String insideLocalIp, int insideLocalPort, String destIp, int destPort, String protocol) {
        List<NatTranslationEntry> table = natTables.computeIfAbsent(routerId, k -> new ArrayList<>());

        // Check existing translation
        for (NatTranslationEntry entry : table) {
            if (entry.getInsideLocalIp().equalsIgnoreCase(insideLocalIp) &&
                entry.getInsideLocalPort() == insideLocalPort &&
                entry.getProtocol().equalsIgnoreCase(protocol)) {
                return entry;
            }
        }

        // Create new PAT translation entry
        int wanPort = nextAllocatedPort.incrementAndGet();
        NatTranslationEntry newEntry = NatTranslationEntry.builder()
                .protocol(protocol)
                .insideLocalIp(insideLocalIp)
                .insideLocalPort(insideLocalPort)
                .insideGlobalIp(insideGlobalWanIp)
                .insideGlobalPort(wanPort)
                .outsideLocalIp(destIp)
                .outsideLocalPort(destPort)
                .outsideGlobalIp(destIp)
                .outsideGlobalPort(destPort)
                .createdAt(LocalDateTime.now())
                .build();

        table.add(newEntry);
        return newEntry;
    }

    public Optional<NatTranslationEntry> lookupInbound(Long routerId, int insideGlobalPort, String protocol) {
        List<NatTranslationEntry> table = natTables.get(routerId);
        if (table != null) {
            return table.stream()
                    .filter(e -> e.getInsideGlobalPort() == insideGlobalPort && e.getProtocol().equalsIgnoreCase(protocol))
                    .findFirst();
        }
        return Optional.empty();
    }

    public List<NatTranslationEntry> getTranslations(Long routerId) {
        return natTables.getOrDefault(routerId, Collections.emptyList());
    }

    public void clearTranslations(Long routerId) {
        List<NatTranslationEntry> table = natTables.get(routerId);
        if (table != null) {
            table.clear();
        }
    }

    public void resetAll() {
        natTables.clear();
    }
}

