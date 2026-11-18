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

    private String sourceIp;
    private String destinationIp;
    
    @Builder.Default
    private int ttl = 64;
    
    @Builder.Default
    private int protocol = PROTOCOL_RAW;
    
    private Object payload;
}
