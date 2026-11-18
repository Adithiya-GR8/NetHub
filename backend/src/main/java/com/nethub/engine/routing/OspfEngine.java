package com.nethub.engine.routing;

import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.DeviceType;
import com.nethub.model.RouteEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OspfEngine {

    private final DijkstraEngine dijkstraEngine = new DijkstraEngine();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OspfNeighbor {
        private String neighborRouterId;
        private String neighborIp;
        private String interfaceName;
        private int deadTimeSeconds;
        private String state; // DOWN, INIT, 2-WAY, EXSTART, EXCHANGE, LOADING, FULL
        private String drStatus; // DR, BDR, DROTHER
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LsaEntry {
        private String linkStateId;
        private String advertisingRouter;
        private int sequenceNumber;
        private int ageSeconds;
        private int metric;
        private String networkPrefix;
        private String subnetMask;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OspfStateDto {
        private Long routerId;
        private String routerName;
        private String ospfRouterId;
        private int areaId;
        private List<OspfNeighbor> neighbors;
        private List<LsaEntry> lsdb;
        private DijkstraEngine.DijkstraResult spfCalculation;
    }

    // Router ID -> OSPF state
    private final Map<Long, OspfStateDto> ospfStates = new ConcurrentHashMap<>();

    public OspfStateDto computeOspf(Long deviceId, Device router, List<NetworkInterface> interfaces, List<DijkstraEngine.GraphEdge> edges) {
        String routerIdIp = interfaces.isEmpty() || interfaces.get(0).getIpAddress() == null
                ? "1.1.1.1"
                : interfaces.get(0).getIpAddress();

        // Build Neighbors
        List<OspfNeighbor> neighbors = new ArrayList<>();
        List<LsaEntry> lsdb = new ArrayList<>();

        // Generate Type 1 Router-LSA for directly connected interfaces
        for (NetworkInterface intf : interfaces) {
            if (intf.getIpAddress() != null && !intf.getIpAddress().trim().isEmpty()) {
                lsdb.add(LsaEntry.builder()
                        .linkStateId(intf.getIpAddress())
                        .advertisingRouter(routerIdIp)
                        .sequenceNumber(0x80000001)
                        .ageSeconds(30)
                        .metric(10)
                        .networkPrefix(intf.getIpAddress())
                        .subnetMask(intf.getSubnetMask() != null ? intf.getSubnetMask() : "255.255.255.0")
                        .build());
            }
        }

        for (DijkstraEngine.GraphEdge edge : edges) {
            if (edge.getSourceRouter().equalsIgnoreCase(router.getName())) {
                neighbors.add(OspfNeighbor.builder()
                        .neighborRouterId(edge.getTargetRouter())
                        .neighborIp("10.0.0.2")
                        .interfaceName(edge.getOutgoingInterface())
                        .deadTimeSeconds(38)
                        .state("FULL")
                        .drStatus("DR")
                        .build());

                lsdb.add(LsaEntry.builder()
                        .linkStateId(edge.getTargetNetwork() != null ? edge.getTargetNetwork() : edge.getTargetRouter())
                        .advertisingRouter(edge.getSourceRouter())
                        .sequenceNumber(0x80000001)
                        .ageSeconds(120)
                        .metric(edge.getCost())
                        .networkPrefix(edge.getTargetNetwork())
                        .subnetMask(edge.getSubnetMask())
                        .build());
            }
        }

        DijkstraEngine.DijkstraResult spf = dijkstraEngine.computeShortestPath(router.getName(), edges);

        OspfStateDto state = OspfStateDto.builder()
                .routerId(deviceId)
                .routerName(router.getName())
                .ospfRouterId(routerIdIp)
                .areaId(0)
                .neighbors(neighbors)
                .lsdb(lsdb)
                .spfCalculation(spf)
                .build();

        ospfStates.put(deviceId, state);
        return state;
    }

    public OspfStateDto getOspfState(Long deviceId) {
        return ospfStates.get(deviceId);
    }

    public void resetAll() {
        ospfStates.clear();
    }
}
