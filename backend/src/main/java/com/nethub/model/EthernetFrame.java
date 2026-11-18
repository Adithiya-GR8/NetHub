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
    public static final String ETHERTYPE_VLAN = "0x8100";
    public static final String BROADCAST_MAC = "FF:FF:FF:FF:FF:FF";

    private String sourceMac;
    private String destinationMac;
    
    @Builder.Default
    private String etherType = ETHERTYPE_IPV4;
    
    @Builder.Default
    private Integer vlanId = null; // 802.1Q VLAN Tag if tagged

    private Object payload;   // IpPacket or ArpPacket
    
    @Builder.Default
    private Integer frameSize = 64; // Frame size in bytes

    @Builder.Default
    private String fcs = "0x3A9F1C8B"; // Frame Check Sequence (CRC32 representation)

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public EthernetFrame copy() {
        return EthernetFrame.builder()
                .sourceMac(this.sourceMac)
                .destinationMac(this.destinationMac)
                .etherType(this.etherType)
                .vlanId(this.vlanId)
                .payload(this.payload)
                .frameSize(this.frameSize)
                .fcs(this.fcs)
                .timestamp(this.timestamp)
                .build();
    }
}
