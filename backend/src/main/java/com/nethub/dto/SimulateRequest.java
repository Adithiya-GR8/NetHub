package com.nethub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulateRequest {
    @NotNull(message = "Source device ID is required")
    private Long sourceDeviceId;

    @NotBlank(message = "Destination IP is required")
    private String destinationIp;
}
