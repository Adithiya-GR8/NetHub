package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcmpPacket {
    public static final int TYPE_ECHO_REPLY = 0;
    public static final int TYPE_DESTINATION_UNREACHABLE = 3;
    public static final int TYPE_ECHO_REQUEST = 8;
    public static final int TYPE_TIME_EXCEEDED = 11;

    public static final int CODE_NET_UNREACHABLE = 0;
    public static final int CODE_HOST_UNREACHABLE = 1;
    public static final int CODE_PORT_UNREACHABLE = 3;
    public static final int CODE_ADMIN_PROHIBITED = 13;
    public static final int CODE_TTL_EXPIRED = 0;

    @Builder.Default
    private int type = TYPE_ECHO_REQUEST;
    
    @Builder.Default
    private int code = 0;
    
    @Builder.Default
    private int checksum = 0xF54A;
    
    @Builder.Default
    private int identifier = 1;
    
    @Builder.Default
    private int sequenceNumber = 1;
    
    @Builder.Default
    private String data = "NetHub Network Probe Data 32 bytes";

    public String getTypeName() {
        switch (type) {
            case TYPE_ECHO_REQUEST: return "Echo Request";
            case TYPE_ECHO_REPLY: return "Echo Reply";
            case TYPE_DESTINATION_UNREACHABLE: return "Destination Unreachable (Code " + code + ")";
            case TYPE_TIME_EXCEEDED: return "Time Exceeded (TTL Expired in Transit)";
            default: return "ICMP Type " + type;
        }
    }
}

