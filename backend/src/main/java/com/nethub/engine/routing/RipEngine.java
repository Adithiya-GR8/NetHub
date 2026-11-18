package com.nethub.engine.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RipEngine {

    public static final int RIP_INFINITY = 16;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RipRouteEntry {
        private String destinationNetwork;
        private String subnetMask;
        private String nextHop;
        private String outgoingInterface;
        private int hopCount; // 1 to 15 (16 is unreachable/infinity)
        private int timerSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RipStateDto {
        private Long routerId;
        private String routerName;
        private int updateTimer;
        private boolean splitHorizonEnabled;
        private boolean poisonReverseEnabled;
        private List<RipRouteEntry> routes;
    }

    private final Map<Long, RipStateDto> ripStates = new ConcurrentHashMap<>();

    public RipStateDto initializeRip(Long routerId, String routerName, List<RipRouteEntry> initialRoutes) {
        RipStateDto state = RipStateDto.builder()
                .routerId(routerId)
                .routerName(routerName)
                .updateTimer(30)
                .splitHorizonEnabled(true)
                .poisonReverseEnabled(true)
                .routes(new ArrayList<>(initialRoutes))
                .build();

        ripStates.put(routerId, state);
        return state;
    }

    /**
     * Applies Bellman-Ford Distance Vector update from neighbor advertisement
     */
    public boolean processRipUpdate(Long routerId, String neighborInterface, List<RipRouteEntry> advertisement) {
        RipStateDto state = ripStates.get(routerId);
        if (state == null) return false;

        boolean updated = false;
        for (RipRouteEntry advertised : advertisement) {
            int newHopCount = Math.min(advertised.getHopCount() + 1, RIP_INFINITY);

            RipRouteEntry existing = state.getRoutes().stream()
                    .filter(r -> r.getDestinationNetwork().equalsIgnoreCase(advertised.getDestinationNetwork()))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                state.getRoutes().add(RipRouteEntry.builder()
                        .destinationNetwork(advertised.getDestinationNetwork())
                        .subnetMask(advertised.getSubnetMask())
                        .nextHop(advertised.getNextHop())
                        .outgoingInterface(neighborInterface)
                        .hopCount(newHopCount)
                        .timerSeconds(180)
                        .build());
                updated = true;
            } else if (newHopCount < existing.getHopCount() || (existing.getNextHop() != null && existing.getNextHop().equalsIgnoreCase(advertised.getNextHop()))) {
                existing.setHopCount(newHopCount);
                existing.setNextHop(advertised.getNextHop());
                existing.setOutgoingInterface(neighborInterface);
                existing.setTimerSeconds(180);
                updated = true;
            }
        }
        return updated;
    }

    public RipStateDto getRipState(Long routerId) {
        return ripStates.get(routerId);
    }

    public void resetAll() {
        ripStates.clear();
    }
}
