package com.nethub.engine.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FirewallEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpiConnectionEntry {
        private String protocol;
        private String srcIp;
        private int srcPort;
        private String dstIp;
        private int dstPort;
        private String state; // NEW, ESTABLISHED, CLOSED
        @Builder.Default
        private LocalDateTime lastActivity = LocalDateTime.now();
    }

    // Device ID -> List<SpiConnectionEntry>
    private final Map<Long, List<SpiConnectionEntry>> spiTables = new ConcurrentHashMap<>();

    public void registerOutboundConnection(Long deviceId, String protocol, String srcIp, int srcPort, String dstIp, int dstPort) {
        List<SpiConnectionEntry> table = spiTables.computeIfAbsent(deviceId, k -> new ArrayList<>());
        table.removeIf(e -> e.getSrcIp().equalsIgnoreCase(srcIp) && e.getSrcPort() == srcPort && e.getDstIp().equalsIgnoreCase(dstIp) && e.getDstPort() == dstPort);
        table.add(SpiConnectionEntry.builder()
                .protocol(protocol)
                .srcIp(srcIp)
                .srcPort(srcPort)
                .dstIp(dstIp)
                .dstPort(dstPort)
                .state("ESTABLISHED")
                .lastActivity(LocalDateTime.now())
                .build());
    }

    public boolean inspectInboundPacket(Long deviceId, String protocol, String remoteSrcIp, int remoteSrcPort, String localDstIp, int localDstPort) {
        List<SpiConnectionEntry> table = spiTables.get(deviceId);
        if (table == null) return false;

        // Check if matching an established outbound connection (swapped src and dst)
        return table.stream().anyMatch(e ->
                e.getProtocol().equalsIgnoreCase(protocol) &&
                e.getDstIp().equalsIgnoreCase(remoteSrcIp) &&
                e.getDstPort() == remoteSrcPort &&
                e.getSrcIp().equalsIgnoreCase(localDstIp) &&
                e.getSrcPort() == localDstPort &&
                "ESTABLISHED".equalsIgnoreCase(e.getState())
        );
    }

    public List<SpiConnectionEntry> getActiveConnections(Long deviceId) {
        return spiTables.getOrDefault(deviceId, Collections.emptyList());
    }

    public void resetAll() {
        spiTables.clear();
    }
}

