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
    private String status; // LEARNED or STATIC
    
    @Builder.Default
    private LocalDateTime lastSeen = LocalDateTime.now();
}
