package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArpPacket {
    public static final String OP_REQUEST = "REQUEST";
    public static final String OP_REPLY = "REPLY";

    private String operation; // REQUEST or REPLY
    private String senderMac;
    private String senderIp;
    private String targetMac;
    private String targetIp;
}
