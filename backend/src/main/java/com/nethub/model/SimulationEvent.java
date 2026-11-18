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
    @Builder.Default
    private int stepIndex = 0;

    private String type;        // e.g. FRAME_CREATED, ARP_REQUEST, ARP_REPLY, SWITCH_FORWARD, SWITCH_FLOOD, ROUTE_LOOKUP, FRAME_REENCAPSULATION, DELIVERED, ERROR, PACKET_DROP
    private Long deviceId;
    private String device;      // Device name
    private String interfaceName;
    private String action;      // GENERATE, RECEIVE, FORWARD, FLOOD, REENCAPSULATE, DELIVER, DROP
    private String description; // Short description
    private String explanation; // In-depth educational explanation of why the device acted this way
    private String dropReason;  // TTL_EXPIRED, NO_ROUTE, INTERFACE_DOWN, etc.
    
    private EthernetFrame frameSnapshot; // Snapshot of the packet/frame at this exact step

    private Map<String, Object> details; // Key-value details (mac, ip, port, vlan, etc.)
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
