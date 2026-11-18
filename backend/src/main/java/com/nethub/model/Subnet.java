package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subnet {
    private String ipAddress;
    private String subnetMask;
    private int cidr;
    private String networkAddress;
    private String broadcastAddress;
    private String firstUsableAddress;
    private String lastUsableAddress;
    private long usableHostCount;
}
