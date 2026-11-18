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
public class EthernetFrame {
    public static final String ETHERTYPE_IPV4 = "0x0800";
    public static final String ETHERTYPE_ARP = "0x0806";
    public static final String BROADCAST_MAC = "FF:FF:FF:FF:FF:FF";

    private String sourceMac;
    private String destinationMac;
    private String etherType; // 0x0800 for IPv4, 0x0806 for ARP
    private Object payload;   // IpPacket or ArpPacket
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
