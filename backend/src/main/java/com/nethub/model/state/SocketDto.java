package com.nethub.model.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocketDto {
    private String protocol; // TCP, UDP
    private String localAddress;
    private int localPort;
    private String remoteAddress;
    private int remotePort;
    private String state; // LISTEN, ESTABLISHED, SYN_SENT, TIME_WAIT, etc.
    private String serviceName; // HTTP, DNS, DHCP, etc.
}

