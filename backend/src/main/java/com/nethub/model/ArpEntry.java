package com.nethub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArpEntry {
    public static final String STATE_REACHABLE = "REACHABLE";
    public static final String STATE_INCOMPLETE = "INCOMPLETE";
    public static final String STATE_STALE = "STALE";
    public static final String STATE_FAILED = "FAILED";
    public static final String STATE_STATIC = "STATIC";

    private String ipAddress;
    private String macAddress;
    
    @Builder.Default
    private String state = STATE_REACHABLE;

    @Builder.Default
    private String interfaceName = "eth0";

    @Builder.Default
    private int ttlSeconds = 300;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public boolean isExpired(long maxAgeSeconds) {
        if (STATE_STATIC.equalsIgnoreCase(state)) {
            return false;
        }
        return timestamp.plusSeconds(maxAgeSeconds).isBefore(LocalDateTime.now());
    }
}
