package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DhcpMessage {
    public static final String TYPE_DISCOVER = "DHCPDISCOVER";
    public static final String TYPE_OFFER = "DHCPOFFER";
    public static final String TYPE_REQUEST = "DHCPREQUEST";
    public static final String TYPE_ACK = "DHCPACK";

    @Builder.Default
    private String messageType = TYPE_DISCOVER;
    
    @Builder.Default
    private String xid = "0x3903F326";
    
    private String clientMac;
    private String offeredIp;
    private String subnetMask;
    private String routerGateway;
    private String dnsServer;
    
    @Builder.Default
    private int leaseSeconds = 86400;
}

