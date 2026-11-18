package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MacTableEntry {
    private String macAddress;
    private String port; // interface name or ID
    
    @Builder.Default
    private Integer vlanId = 1;

    @Builder.Default
    private String status = "LEARNED"; // LEARNED, DYNAMIC, or STATIC
    
    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();

    public boolean isExpired(long maxAgeSeconds) {
        if ("STATIC".equalsIgnoreCase(status)) {
            return false;
        }
        return lastSeen.plusSeconds(maxAgeSeconds).isBefore(LocalDateTime.now());
    }
}
