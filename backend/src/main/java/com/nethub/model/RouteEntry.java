package com.nethub.model;

import com.nethub.entity.enums.RouteType;
import com.nethub.utils.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEntry {
    private Long id;
    private String destinationNetwork;
    private String subnetMask;
    private String nextHop;
    private String outgoingInterface;
    private int metric;
    private RouteType routeType;

    public int getPrefixLength() {
        if (subnetMask == null || subnetMask.isEmpty()) return 0;
        return IpUtils.maskToCidr(subnetMask);
    }
}
