package com.nethub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    private String command;
    private String output;
    
    @Builder.Default
    private int exitCode = 0;
    
    private String deviceName;
    private String deviceType;
}

