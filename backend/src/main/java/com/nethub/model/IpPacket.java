package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpPacket {
    public static final int PROTOCOL_ICMP = 1;
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;
    public static final int PROTOCOL_RAW = 254;

    @Builder.Default
    private int version = 4;

    @Builder.Default
    private int ihl = 5; // 20 bytes standard header

    @Builder.Default
    private int typeOfService = 0; // DSCP / ECN

    @Builder.Default
    private int totalLength = 84;

    @Builder.Default
    private int identification = 0x5432;

    @Builder.Default
    private boolean dontFragment = true;

    @Builder.Default
    private boolean moreFragments = false;

    @Builder.Default
    private int fragmentOffset = 0;

    private String sourceIp;
    private String destinationIp;
    
    @Builder.Default
    private int ttl = 64;
    
    @Builder.Default
    private int protocol = PROTOCOL_RAW;

    @Builder.Default
    private int headerChecksum = 0xAE42;
    
    private Object payload;

    public void decrementTtl() {
        this.ttl = Math.max(0, this.ttl - 1);
    }

    public String getProtocolName() {
        switch (protocol) {
            case PROTOCOL_ICMP: return "ICMP (1)";
            case PROTOCOL_TCP: return "TCP (6)";
            case PROTOCOL_UDP: return "UDP (17)";
            default: return "RAW (" + protocol + ")";
        }
    }

    public IpPacket copy() {
        return IpPacket.builder()
                .version(this.version)
                .ihl(this.ihl)
                .typeOfService(this.typeOfService)
                .totalLength(this.totalLength)
                .identification(this.identification)
                .dontFragment(this.dontFragment)
                .moreFragments(this.moreFragments)
                .fragmentOffset(this.fragmentOffset)
                .sourceIp(this.sourceIp)
                .destinationIp(this.destinationIp)
                .ttl(this.ttl)
                .protocol(this.protocol)
                .headerChecksum(this.headerChecksum)
                .payload(this.payload)
                .build();
    }
}
