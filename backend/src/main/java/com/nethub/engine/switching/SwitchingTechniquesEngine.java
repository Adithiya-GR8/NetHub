package com.nethub.engine.switching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SwitchingTechniquesEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechniqueEvaluation {
        private String technique; // CIRCUIT_SWITCHING, MESSAGE_SWITCHING, PACKET_SWITCHING
        private double setupDelayMs;
        private double transmissionDelayMs;
        private double queuingDelayMs;
        private double propagationDelayMs;
        private double totalLatencyMs;
        private int totalPackets;
        private long totalBytesWithOverhead;
        private double lineEfficiencyPercent;
        private List<String> executionSteps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparativeSwitchingResult {
        private long originalMessageSizeBytes;
        private int hopCount;
        private long linkBandwidthBps;
        private TechniqueEvaluation circuitSwitching;
        private TechniqueEvaluation messageSwitching;
        private TechniqueEvaluation packetSwitching;
        private String recommendation;
    }

    public ComparativeSwitchingResult compareTechniques(long messageSizeBytes, int hopCount, long linkBandwidthBps) {
        if (hopCount < 1) hopCount = 1;
        if (linkBandwidthBps <= 0) linkBandwidthBps = 100_000_000L; // 100 Mbps default
        if (messageSizeBytes <= 0) messageSizeBytes = 64_000L; // 64 KB

        double propDelayPerHopMs = 2.0; // 2 ms per link propagation
        double totalPropMs = hopCount * propDelayPerHopMs;

        // --- 1. Circuit Switching ---
        double circuitSetupMs = 2.0 * (hopCount * propDelayPerHopMs) + (hopCount * 1.0); // 2-way handshake
        double circuitTxMs = ((messageSizeBytes * 8.0) / linkBandwidthBps) * 1000.0;
        double circuitTotalMs = circuitSetupMs + circuitTxMs + totalPropMs;
        List<String> circuitSteps = List.of(
                "Call Setup Request sent across " + hopCount + " hops to reserve dedicated bandwidth.",
                "Dedicated end-to-end circuit established and resource locked.",
                "Continuous bit-stream data transfer without intermediate store-and-forward queuing.",
                "Call Teardown message released reserved circuit."
        );
        TechniqueEvaluation circuitEval = TechniqueEvaluation.builder()
                .technique("CIRCUIT_SWITCHING")
                .setupDelayMs(circuitSetupMs)
                .transmissionDelayMs(circuitTxMs)
                .queuingDelayMs(0.0) // Zero queuing once circuit is open
                .propagationDelayMs(totalPropMs)
                .totalLatencyMs(circuitTotalMs)
                .totalPackets(1)
                .totalBytesWithOverhead(messageSizeBytes)
                .lineEfficiencyPercent(100.0)
                .executionSteps(circuitSteps)
                .build();

        // --- 2. Message Switching (Store-and-Forward of entire message) ---
        double msgTxPerHopMs = ((messageSizeBytes * 8.0) / linkBandwidthBps) * 1000.0;
        double msgTotalTxMs = msgTxPerHopMs * hopCount; // Must be stored completely at every intermediate node
        double msgQueuingMs = (hopCount - 1) * 3.0; // Buffer verification at intermediate nodes
        double msgTotalMs = msgTotalTxMs + msgQueuingMs + totalPropMs;
        List<String> msgSteps = List.of(
                "Sender transmits entire message (" + (messageSizeBytes / 1024) + " KB) to Node 1.",
                "Intermediate nodes store complete message on disk/RAM before forwarding to next node.",
                "Store-and-forward delay accumulates sequentially at all " + hopCount + " hops.",
                "Destination receives entire message and confirms delivery."
        );
        TechniqueEvaluation messageEval = TechniqueEvaluation.builder()
                .technique("MESSAGE_SWITCHING")
                .setupDelayMs(0.0)
                .transmissionDelayMs(msgTotalTxMs)
                .queuingDelayMs(msgQueuingMs)
                .propagationDelayMs(totalPropMs)
                .totalLatencyMs(msgTotalMs)
                .totalPackets(1)
                .totalBytesWithOverhead(messageSizeBytes + 128)
                .lineEfficiencyPercent(98.5)
                .executionSteps(msgSteps)
                .build();

        // --- 3. Packet Switching (Datagram pipelining with MTU) ---
        int packetPayloadSize = 1460; // Standard Ethernet MSS
        int packetHeaderSize = 40;   // 20B IP + 20B TCP
        int packetTotalSize = packetPayloadSize + packetHeaderSize;
        int packetCount = (int) Math.ceil((double) messageSizeBytes / packetPayloadSize);
        long totalPacketBytes = (long) packetCount * packetTotalSize;

        double packetTxTimeMs = ((packetTotalSize * 8.0) / linkBandwidthBps) * 1000.0;
        // Pipelining: Transmission time of all packets on first hop + transmission of last packet across remaining hops
        double pipelinedTxMs = (packetCount * packetTxTimeMs) + ((hopCount - 1) * packetTxTimeMs);
        double packetQueuingMs = (hopCount - 1) * 0.5;
        double packetTotalMs = pipelinedTxMs + packetQueuingMs + totalPropMs;
        List<String> packetSteps = List.of(
                "Segmented original message into " + packetCount + " individual packets (" + packetTotalSize + " bytes each).",
                "Pipelined transmission: Intermediate routers forward packet K while receiving packet K+1 simultaneously.",
                "Routers forward packets independently across optimal paths without circuit reservation.",
                "Destination receiver reassembles packets in correct sequence."
        );
        TechniqueEvaluation packetEval = TechniqueEvaluation.builder()
                .technique("PACKET_SWITCHING")
                .setupDelayMs(0.0)
                .transmissionDelayMs(pipelinedTxMs)
                .queuingDelayMs(packetQueuingMs)
                .propagationDelayMs(totalPropMs)
                .totalLatencyMs(packetTotalMs)
                .totalPackets(packetCount)
                .totalBytesWithOverhead(totalPacketBytes)
                .lineEfficiencyPercent(Math.round(((double) messageSizeBytes / totalPacketBytes) * 1000.0) / 10.0)
                .executionSteps(packetSteps)
                .build();

        String recommendation;
        if (circuitTotalMs < packetTotalMs && messageSizeBytes > 10_000_000L) {
            recommendation = "Circuit Switching is optimal for prolonged, massive data streams where initial call setup overhead is amortized.";
        } else {
            recommendation = "Packet Switching is most efficient due to link pipelining, multiplexing, and zero call setup latency.";
        }

        return ComparativeSwitchingResult.builder()
                .originalMessageSizeBytes(messageSizeBytes)
                .hopCount(hopCount)
                .linkBandwidthBps(linkBandwidthBps)
                .circuitSwitching(circuitEval)
                .messageSwitching(messageEval)
                .packetSwitching(packetEval)
                .recommendation(recommendation)
                .build();
    }
}

