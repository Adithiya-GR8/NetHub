package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationEvent {
    private String type;        // e.g. VALIDATION, ARP_REQUEST, ARP_REPLY, FRAME_FORWARD, ROUTE_LOOKUP, FRAME_REENCAPSULATION, DELIVERED, ERROR
    private String device;      // Device name
    private String description; // Human-readable description
    private Map<String, Object> details; // Key-value details (e.g. mac, ip, port, interface)
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
