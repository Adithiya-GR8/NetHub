package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpDatagram {
    @Builder.Default
    private int sourcePort = 5353;
    
    @Builder.Default
    private int destinationPort = 53;
    
    @Builder.Default
    private int length = 8;
    
    @Builder.Default
    private int checksum = 0x89A1;
    
    private Object payload;
}

