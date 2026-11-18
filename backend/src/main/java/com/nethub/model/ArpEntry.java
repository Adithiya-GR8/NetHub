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
public class ArpEntry {
    private String ipAddress;
    private String macAddress;
    
    @Builder.Default
    private String state = "REACHABLE";
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
