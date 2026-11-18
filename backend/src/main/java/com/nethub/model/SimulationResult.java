package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {
    private boolean success;
    private String source;
    private String destination;
    private String errorCode;
    private String errorMessage;
    private List<String> path;
    private List<SimulationEvent> events;
}
