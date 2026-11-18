package com.nethub.engine;

import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import com.nethub.utils.IpUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
public class RoutingEngine {

    public Optional<RouteEntry> lookupBestRoute(RoutingTable routingTable, String destinationIp) {
        if (routingTable == null || destinationIp == null || !IpUtils.isValidIPv4(destinationIp)) {
            return Optional.empty();
        }

        return routingTable.getAllRoutes().stream()
                .filter(route -> IpUtils.isIpInSubnet(destinationIp, route.getDestinationNetwork(), route.getSubnetMask()))
                .max(Comparator.comparingInt(RouteEntry::getPrefixLength)
                        .thenComparing(Comparator.comparingInt(RouteEntry::getMetric).reversed()));
    }
}
