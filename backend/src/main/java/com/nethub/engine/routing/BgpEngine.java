package com.nethub.engine.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BgpEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BgpNeighbor {
        private String peerIp;
        private int remoteAs;
        private String peerType; // eBGP or iBGP
        private String state; // IDLE, CONNECT, ACTIVE, OPENSENT, OPENCONFIRM, ESTABLISHED
        private int prefixesReceived;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BgpRoute {
        private String prefix;
        private String nextHop;
        private List<Integer> asPath; // List of AS numbers traversed
        private int localPreference;
        private int med; // Multi-Exit Discriminator
        private String origin; // IGP, EGP, INCOMPLETE
        private boolean bestPath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BgpStateDto {
        private Long routerId;
        private String routerName;
        private int localAs;
        private String bgpRouterId;
        private List<BgpNeighbor> neighbors;
        private List<BgpRoute> routingTable;
    }

    private final Map<Long, BgpStateDto> bgpStates = new ConcurrentHashMap<>();

    public BgpStateDto configureBgp(Long routerId, String routerName, int localAs, String bgpId) {
        BgpStateDto state = BgpStateDto.builder()
                .routerId(routerId)
                .routerName(routerName)
                .localAs(localAs)
                .bgpRouterId(bgpId)
                .neighbors(new ArrayList<>())
                .routingTable(new ArrayList<>())
                .build();

        bgpStates.put(routerId, state);
        return state;
    }

    public void addNeighbor(Long routerId, String peerIp, int remoteAs) {
        BgpStateDto state = bgpStates.get(routerId);
        if (state != null) {
            String type = (remoteAs == state.getLocalAs()) ? "iBGP" : "eBGP";
            state.getNeighbors().add(BgpNeighbor.builder()
                    .peerIp(peerIp)
                    .remoteAs(remoteAs)
                    .peerType(type)
                    .state("ESTABLISHED")
                    .prefixesReceived(1)
                    .build());
        }
    }

    public void advertisePrefix(Long routerId, String prefix, String nextHop, List<Integer> asPath) {
        BgpStateDto state = bgpStates.get(routerId);
        if (state != null) {
            List<Integer> fullPath = new ArrayList<>(asPath);
            if (!fullPath.contains(state.getLocalAs())) {
                fullPath.add(0, state.getLocalAs());
            }

            state.getRoutingTable().add(BgpRoute.builder()
                    .prefix(prefix)
                    .nextHop(nextHop)
                    .asPath(fullPath)
                    .localPreference(100)
                    .med(0)
                    .origin("IGP")
                    .bestPath(true)
                    .build());
        }
    }

    public BgpStateDto getBgpState(Long routerId) {
        return bgpStates.get(routerId);
    }

    public void resetAll() {
        bgpStates.clear();
    }
}

