package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpSegment {
    @Builder.Default
    private int sourcePort = 49152;
    
    @Builder.Default
    private int destinationPort = 80;
    
    @Builder.Default
    private long sequenceNumber = 1000L;
    
    @Builder.Default
    private long acknowledgmentNumber = 0L;
    
    @Builder.Default
    private int headerLength = 20;
    
    @Builder.Default
    private boolean syn = false;
    
    @Builder.Default
    private boolean ack = false;
    
    @Builder.Default
    private boolean fin = false;
    
    @Builder.Default
    private boolean rst = false;
    
    @Builder.Default
    private boolean psh = false;
    
    @Builder.Default
    private boolean urg = false;
    
    @Builder.Default
    private int windowSize = 65535;
    
    @Builder.Default
    private int checksum = 0x4B21;
    
    private Object payload;

    public String getFlagsString() {
        StringBuilder sb = new StringBuilder("[");
        if (syn) sb.append("SYN ");
        if (ack) sb.append("ACK ");
        if (fin) sb.append("FIN ");
        if (rst) sb.append("RST ");
        if (psh) sb.append("PSH ");
        if (urg) sb.append("URG ");
        return sb.toString().trim() + "]";
    }
}

