package com.nethub.engine.hardware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MacControlEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsmaCdResult {
        private boolean collisionDetected;
        private int collisionAttempt;
        private int backoffSlots;
        private double backoffMicroseconds;
        private String jamSignal;
        private String status; // TRANSMITTED, COLLISION_BACKOFF, RETRANSMISSION_EXCEEDED
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsmaCaResult {
        private boolean channelBusy;
        private int difsMicroseconds;
        private int sifsMicroseconds;
        private int backoffSlots;
        private boolean rtsCtsUsed;
        private boolean ackReceived;
        private String status; // TRANSMITTED, CHANNEL_BUSY_WAIT, ACK_TIMEOUT
        private String explanation;
    }

    /**
     * Simulates IEEE 802.3 CSMA/CD Truncated Binary Exponential Backoff
     */
    public CsmaCdResult simulateCsmaCd(boolean channelContention, int attemptNumber) {
        if (!channelContention) {
            return CsmaCdResult.builder()
                    .collisionDetected(false)
                    .collisionAttempt(attemptNumber)
                    .backoffSlots(0)
                    .backoffMicroseconds(0)
                    .status("TRANSMITTED")
                    .explanation("Carrier sense detected idle channel (half-duplex). Frame successfully transmitted with no collisions.")
                    .build();
        }

        if (attemptNumber >= 16) {
            return CsmaCdResult.builder()
                    .collisionDetected(true)
                    .collisionAttempt(attemptNumber)
                    .backoffSlots(0)
                    .backoffMicroseconds(0)
                    .jamSignal("0xAAAAAAAA")
                    .status("RETRANSMISSION_EXCEEDED")
                    .explanation("Collision detected on attempt 16. Exceeded maximum retry count (16). Frame dropped with excessive collision error.")
                    .build();
        }

        // k = min(attemptNumber, 10)
        int k = Math.min(attemptNumber, 10);
        int maxSlots = (1 << k) - 1; // 2^k - 1
        int chosenSlot = new Random().nextInt(maxSlots + 1);
        double slotTimeUs = 51.2; // 51.2 microseconds for 10/100 Mbps standard slot time (512 bit times)
        double backoffUs = chosenSlot * slotTimeUs;

        return CsmaCdResult.builder()
                .collisionDetected(true)
                .collisionAttempt(attemptNumber)
                .backoffSlots(chosenSlot)
                .backoffMicroseconds(backoffUs)
                .jamSignal("0xAAAAAAAA")
                .status("COLLISION_BACKOFF")
                .explanation("Collision detected! Sent 32-bit JAM signal. Truncated Binary Exponential Backoff: Chosen slot r=" + chosenSlot + " from [0, " + maxSlots + "]. Backing off for " + String.format("%.2f", backoffUs) + " \u00b5s before retransmission.")
                .build();
    }

    /**
     * Simulates IEEE 802.11 CSMA/CA with DIFS, Contention Window, RTS/CTS, and ACK
     */
    public CsmaCaResult simulateCsmaCa(boolean channelBusy, boolean enableRtsCts) {
        int difs = 50; // 50 us
        int sifs = 10; // 10 us
        int backoffSlots = new Random().nextInt(16);

        if (channelBusy) {
            return CsmaCaResult.builder()
                    .channelBusy(true)
                    .difsMicroseconds(difs)
                    .sifsMicroseconds(sifs)
                    .backoffSlots(backoffSlots)
                    .rtsCtsUsed(enableRtsCts)
                    .ackReceived(false)
                    .status("CHANNEL_BUSY_WAIT")
                    .explanation("Physical carrier sensing detected RF energy. Channel is BUSY. Station enters deferral and decrements backoff timer only when channel is idle for DIFS (" + difs + " \u00b5s).")
                    .build();
        }

        return CsmaCaResult.builder()
                .channelBusy(false)
                .difsMicroseconds(difs)
                .sifsMicroseconds(sifs)
                .backoffSlots(backoffSlots)
                .rtsCtsUsed(enableRtsCts)
                .ackReceived(true)
                .status("TRANSMITTED")
                .explanation(enableRtsCts
                        ? "Channel sensed idle for DIFS (" + difs + " \u00b5s). Sent RTS -> Received CTS after SIFS (" + sifs + " \u00b5s) -> Transmitted 802.11 Data frame -> Received ACK."
                        : "Channel sensed idle for DIFS (" + difs + " \u00b5s). Completed random backoff (" + backoffSlots + " slots) -> Transmitted 802.11 Data frame -> Received MAC-level ACK.")
                .build();
    }

    /**
     * Computes transmission serialization delay based on interface speed
     */
    public double calculateTransmissionDelayMs(int frameBytes, String speed) {
        long bps;
        if ("10Gbps".equalsIgnoreCase(speed)) {
            bps = 10_000_000_000L;
        } else if ("1Gbps".equalsIgnoreCase(speed)) {
            bps = 1_000_000_000L;
        } else if ("100Mbps".equalsIgnoreCase(speed)) {
            bps = 100_000_000L;
        } else {
            bps = 10_000_000L; // 10 Mbps
        }
        long bits = frameBytes * 8L;
        return (bits * 1000.0) / bps;
    }
}

