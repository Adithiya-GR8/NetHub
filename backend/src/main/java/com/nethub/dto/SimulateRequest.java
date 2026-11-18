package com.nethub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulateRequest {
    @NotNull(message = "Source device ID is required")
    private Long sourceDeviceId;

    @NotBlank(message = "Destination IP is required")
    private String destinationIp;

    @Builder.Default
    private String protocol = "ICMP"; // ICMP, TCP, HTTP, UDP, DNS, DHCP, TRACEROUTE

    @Builder.Default
    private int ttl = 64;
}
