package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathResult {
    private boolean reachable;
    private String reason;
    private String sourceDevice;
    private String destinationIp;
    private String destinationDevice;
    private List<String> path;
    private List<Long> nodeIds;
    private List<Map<String, Object>> hopDetails;
}
