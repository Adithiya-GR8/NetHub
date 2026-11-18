package com.nethub.engine.security;

import com.nethub.utils.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AclEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AclRule {
        private int sequence;
        private String action; // PERMIT or DENY
        @Builder.Default
        private String protocol = "IP"; // IP, TCP, UDP, ICMP
        private String sourceNetwork; // e.g. 192.168.1.0 or any
        private String sourceMask;    // e.g. 255.255.255.0
        private String destinationNetwork;
        private String destinationMask;
        private Integer destinationPort; // e.g. 80, or null for any
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AclEvaluationResult {
        private boolean permitted;
        private AclRule matchedRule;
        private String explanation;
    }

    // Router ID -> List<AclRule>
    private final Map<Long, List<AclRule>> routerAcls = new ConcurrentHashMap<>();

    public void addRule(Long routerId, AclRule rule) {
        List<AclRule> rules = routerAcls.computeIfAbsent(routerId, k -> new ArrayList<>());
        rules.removeIf(r -> r.getSequence() == rule.getSequence());
        rules.add(rule);
        rules.sort(Comparator.comparingInt(AclRule::getSequence));
    }

    public AclEvaluationResult evaluatePacket(Long routerId, String srcIp, String dstIp, String protocol, Integer dstPort) {
        List<AclRule> rules = routerAcls.get(routerId);
        if (rules == null || rules.isEmpty()) {
            return AclEvaluationResult.builder()
                    .permitted(true)
                    .matchedRule(null)
                    .explanation("No active Access Control Lists configured on router. Default PERMIT all traffic.")
                    .build();
        }

        for (AclRule rule : rules) {
            // Protocol match
            if (rule.getProtocol() != null 
                    && !"ANY".equalsIgnoreCase(rule.getProtocol()) 
                    && !"IP".equalsIgnoreCase(rule.getProtocol()) 
                    && !rule.getProtocol().equalsIgnoreCase(protocol)) {
                continue;
            }

            // Source IP match
            if (!"any".equalsIgnoreCase(rule.getSourceNetwork()) && rule.getSourceMask() != null) {
                if (!IpUtils.isIpInSubnet(srcIp, rule.getSourceNetwork(), rule.getSourceMask())) {
                    continue;
                }
            }

            // Destination IP match
            if (!"any".equalsIgnoreCase(rule.getDestinationNetwork()) && rule.getDestinationMask() != null) {
                if (!IpUtils.isIpInSubnet(dstIp, rule.getDestinationNetwork(), rule.getDestinationMask())) {
                    continue;
                }
            }

            // Destination port match
            if (rule.getDestinationPort() != null && dstPort != null && !rule.getDestinationPort().equals(dstPort)) {
                continue;
            }

            // Rule matched!
            boolean permit = "PERMIT".equalsIgnoreCase(rule.getAction());
            return AclEvaluationResult.builder()
                    .permitted(permit)
                    .matchedRule(rule)
                    .explanation("Matched ACL Rule #" + rule.getSequence() + " [" + rule.getAction() + " " + rule.getProtocol() + " " + rule.getSourceNetwork() + " -> " + rule.getDestinationNetwork() + (rule.getDestinationPort() != null ? " port " + rule.getDestinationPort() : "") + "]")
                    .build();
        }

        // Implicit Deny at the end of standard Cisco/Juniper ACL
        return AclEvaluationResult.builder()
                .permitted(false)
                .matchedRule(null)
                .explanation("Packet dropped by implicit deny at the end of Access Control List.")
                .build();
    }

    public List<AclRule> getRules(Long routerId) {
        return routerAcls.getOrDefault(routerId, Collections.emptyList());
    }

    public void clearRules(Long routerId) {
        List<AclRule> rules = routerAcls.get(routerId);
        if (rules != null) {
            rules.clear();
        }
    }

    public void resetAll() {
        routerAcls.clear();
    }
}
