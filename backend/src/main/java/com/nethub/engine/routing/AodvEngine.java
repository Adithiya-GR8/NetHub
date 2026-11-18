package com.nethub.engine.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AodvEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RreqMessage {
        private String rreqId;
        private String originatorIp;
        private int originatorSeqNum;
        private String destinationIp;
        private int destinationSeqNum;
        private int hopCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RrepMessage {
        private String originatorIp;
        private String destinationIp;
        private int destinationSeqNum;
        private int hopCount;
        private int lifetimeSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AodvRoute {
        private String destinationIp;
        private String nextHop;
        private int hopCount;
        private int destinationSeqNum;
        private String status; // ACTIVE, INVALID, EXPIRED
        private LocalDateTime expiresAt;
    }

    // Node IP -> List<AodvRoute>
    private final Map<String, List<AodvRoute>> aodvRoutingTables = new ConcurrentHashMap<>();

    public RreqMessage createRreq(String originatorIp, String destinationIp, int origSeq, int lastKnownDestSeq) {
        return RreqMessage.builder()
                .rreqId(UUID.randomUUID().toString().substring(0, 8))
                .originatorIp(originatorIp)
                .originatorSeqNum(origSeq)
                .destinationIp(destinationIp)
                .destinationSeqNum(lastKnownDestSeq)
                .hopCount(0)
                .build();
    }

    public RrepMessage createRrep(RreqMessage rreq, String respondingNodeIp, int currentDestSeq) {
        return RrepMessage.builder()
                .originatorIp(rreq.getOriginatorIp())
                .destinationIp(rreq.getDestinationIp())
                .destinationSeqNum(Math.max(rreq.getDestinationSeqNum() + 1, currentDestSeq))
                .hopCount(rreq.getHopCount())
                .lifetimeSeconds(300)
                .build();
    }

    public void updateRoute(String nodeIp, String destinationIp, String nextHop, int hops, int seqNum) {
        List<AodvRoute> routes = aodvRoutingTables.computeIfAbsent(nodeIp, k -> new ArrayList<>());
        routes.removeIf(r -> r.getDestinationIp().equalsIgnoreCase(destinationIp));
        routes.add(AodvRoute.builder()
                .destinationIp(destinationIp)
                .nextHop(nextHop)
                .hopCount(hops)
                .destinationSeqNum(seqNum)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusSeconds(300))
                .build());
    }

    public Optional<AodvRoute> lookupRoute(String nodeIp, String destinationIp) {
        List<AodvRoute> routes = aodvRoutingTables.get(nodeIp);
        if (routes != null) {
            return routes.stream()
                    .filter(r -> r.getDestinationIp().equalsIgnoreCase(destinationIp) && "ACTIVE".equals(r.getStatus()))
                    .findFirst();
        }
        return Optional.empty();
    }

    public List<AodvRoute> getRoutes(String nodeIp) {
        return aodvRoutingTables.getOrDefault(nodeIp, Collections.emptyList());
    }

    public void resetAll() {
        aodvRoutingTables.clear();
    }
}

