package com.nethub.controller;

import com.nethub.dto.ApiResponse;
import com.nethub.engine.hardware.HubEngine;
import com.nethub.engine.hardware.BridgeEngine;
import com.nethub.engine.hardware.MacControlEngine;
import com.nethub.engine.hardware.WirelessEngine;
import com.nethub.engine.osi.OsiLayerVisualizer;
import com.nethub.engine.protocols.ApplicationProtocolEngine;
import com.nethub.engine.routing.*;
import com.nethub.engine.security.AclEngine;
import com.nethub.engine.security.FirewallEngine;
import com.nethub.engine.security.NatEngine;
import com.nethub.engine.switching.SwitchingTechniquesEngine;
import com.nethub.engine.transport.SocketApiEngine;
import com.nethub.entity.Device;
import com.nethub.entity.enums.DeviceType;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdvancedNetworkingController {

    private final SwitchingTechniquesEngine switchingTechniquesEngine;
    private final OsiLayerVisualizer osiLayerVisualizer;
    private final ApplicationProtocolEngine protocolEngine;
    private final SocketApiEngine socketApiEngine;
    private final DijkstraEngine dijkstraEngine;
    private final OspfEngine ospfEngine;
    private final RipEngine ripEngine;
    private final BgpEngine bgpEngine;
    private final AodvEngine aodvEngine;
    private final MacControlEngine macControlEngine;
    private final WirelessEngine wirelessEngine;
    private final AclEngine aclEngine;
    private final NatEngine natEngine;
    private final FirewallEngine firewallEngine;
    private final DeviceRepository deviceRepository;
    private final ConnectionRepository connectionRepository;
    private final NetworkInterfaceRepository interfaceRepository;

    // --- 1. Switching Techniques Comparison ---
    @GetMapping("/switching/compare")
    public ResponseEntity<ApiResponse<SwitchingTechniquesEngine.ComparativeSwitchingResult>> compareSwitching(
            @RequestParam(defaultValue = "1000000") long messageSizeBytes,
            @RequestParam(defaultValue = "4") int hops,
            @RequestParam(defaultValue = "100") double bandwidthMbps) {

        long linkBps = (long) (bandwidthMbps * 1_000_000L);
        var result = switchingTechniquesEngine.compareTechniques(messageSizeBytes, hops, linkBps);
        return ResponseEntity.ok(ApiResponse.success(result, "Switching techniques evaluated successfully"));
    }

    // --- 2. OSI & TCP/IP Layer Visualizer ---
    @GetMapping("/simulation/osi-trace")
    public ResponseEntity<ApiResponse<OsiLayerVisualizer.OsiProcessResult>> getOsiTrace(
            @RequestParam(defaultValue = "PC1") String sourceHost,
            @RequestParam(defaultValue = "02:00:00:11:22:33") String sourceMac,
            @RequestParam(defaultValue = "192.168.1.10") String sourceIp,
            @RequestParam(defaultValue = "49152") int sourcePort,
            @RequestParam(defaultValue = "WebServer") String destHost,
            @RequestParam(defaultValue = "02:00:00:AA:BB:CC") String destMac,
            @RequestParam(defaultValue = "192.168.2.80") String destIp,
            @RequestParam(defaultValue = "80") int destPort,
            @RequestParam(defaultValue = "HTTP") String protocol,
            @RequestParam(defaultValue = "GET /index.html HTTP/1.1") String payload) {

        var trace = osiLayerVisualizer.buildEncapsulationProcess(
                sourceHost, destHost, sourceMac, destMac, sourceIp, destIp,
                sourcePort, destPort, protocol, payload);
        return ResponseEntity.ok(ApiResponse.success(trace, "OSI trace generated successfully"));
    }

    // --- 3. DNS Resolution ---
    @Data
    public static class DnsRequest {
        private String domain;
    }

    @PostMapping("/protocols/dns")
    public ResponseEntity<ApiResponse<ApplicationProtocolEngine.DnsResolutionResult>> resolveDns(
            @RequestBody DnsRequest request) {
        var result = protocolEngine.resolveDns(request.getDomain() != null ? request.getDomain() : "web.nethub.local");
        return ResponseEntity.ok(ApiResponse.success(result, "DNS query processed"));
    }

    // --- 4. TLS 1.3 Handshake ---
    @Data
    public static class TlsRequest {
        private String clientHost;
        private String serverHost;
    }

    @PostMapping("/protocols/tls")
    public ResponseEntity<ApiResponse<ApplicationProtocolEngine.TlsHandshakeResult>> simulateTls(
            @RequestBody(required = false) TlsRequest request) {
        String client = request != null && request.getClientHost() != null ? request.getClientHost() : "Client-PC";
        String server = request != null && request.getServerHost() != null ? request.getServerHost() : "Secure-WebServer";

        var result = protocolEngine.simulateTlsHandshake(client, server);
        return ResponseEntity.ok(ApiResponse.success(result, "TLS 1.3 Handshake completed"));
    }

    // --- 5. FTP Session ---
    @Data
    public static class FtpRequest {
        private String serverIp;
        private String username;
        private String command;
        private String filename;
    }

    @PostMapping("/protocols/ftp")
    public ResponseEntity<ApiResponse<ApplicationProtocolEngine.FtpTransactionResult>> simulateFtp(
            @RequestBody(required = false) FtpRequest request) {
        String server = request != null && request.getServerIp() != null ? request.getServerIp() : "192.168.2.10";
        String user = request != null && request.getUsername() != null ? request.getUsername() : "anonymous";
        String cmd = request != null && request.getCommand() != null ? request.getCommand() : "RETR";
        String file = request != null && request.getFilename() != null ? request.getFilename() : "report.pdf";

        var result = protocolEngine.simulateFtp(server, user, cmd, file);
        return ResponseEntity.ok(ApiResponse.success(result, "FTP session executed"));
    }

    // --- 6. Email (SMTP / POP3) ---
    @Data
    public static class EmailRequest {
        private String mailServer;
        private String sender;
        private String recipient;
        private String subject;
        private String body;
    }

    @PostMapping("/protocols/email")
    public ResponseEntity<ApiResponse<ApplicationProtocolEngine.EmailTransactionResult>> simulateEmail(
            @RequestBody(required = false) EmailRequest request) {
        String server = request != null && request.getMailServer() != null ? request.getMailServer() : "mail.nethub.local";
        String sender = request != null && request.getSender() != null ? request.getSender() : "alice@nethub.local";
        String recipient = request != null && request.getRecipient() != null ? request.getRecipient() : "bob@nethub.local";
        String subject = request != null && request.getSubject() != null ? request.getSubject() : "Network Project Update";
        String body = request != null && request.getBody() != null ? request.getBody() : "Hello Bob, all protocols are operational!";

        var result = protocolEngine.simulateEmail(server, sender, recipient, subject, body);
        return ResponseEntity.ok(ApiResponse.success(result, "Email exchange processed"));
    }

    // --- 7. Berkeley Socket Apps ---
    @Data
    public static class SocketAppRequest {
        private String appType; // "UDP_CALC" or "TCP_ECHO"
        private String clientIp;
        private String serverIp;
        private String operation; // For calc: "ADD", "SUB", "MUL", "DIV"
        private Double operand1;
        private Double operand2;
        private List<String> clientIps;
    }

    @PostMapping("/transport/socket-app")
    public ResponseEntity<ApiResponse<Object>> runSocketApp(
            @RequestBody(required = false) SocketAppRequest request) {
        String appType = request != null && request.getAppType() != null ? request.getAppType() : "TCP_ECHO";
        String client = request != null && request.getClientIp() != null ? request.getClientIp() : "192.168.1.10";
        String server = request != null && request.getServerIp() != null ? request.getServerIp() : "192.168.2.10";

        if ("UDP_CALC".equalsIgnoreCase(appType)) {
            String op = request != null && request.getOperation() != null ? request.getOperation() : "ADD";
            double op1 = request != null && request.getOperand1() != null ? request.getOperand1() : 25.0;
            double op2 = request != null && request.getOperand2() != null ? request.getOperand2() : 17.0;
            var calcResult = socketApiEngine.runUdpCalculator(client, server, op, op1, op2);
            return ResponseEntity.ok(ApiResponse.success(calcResult, "UDP Calculator Socket executed"));
        } else {
            List<String> clients = (request != null && request.getClientIps() != null && !request.getClientIps().isEmpty())
                    ? request.getClientIps()
                    : List.of("192.168.1.11", "192.168.1.12", "192.168.1.13");
            var echoResult = socketApiEngine.runTcpMultiClientEcho(server, clients);
            return ResponseEntity.ok(ApiResponse.success(echoResult, "TCP Multi-client Echo executed"));
        }
    }

    // --- 8. Dijkstra Shortest Path Tree for Project ---
    @GetMapping("/projects/{projectId}/routing/dijkstra")
    public ResponseEntity<ApiResponse<DijkstraEngine.DijkstraResult>> computeDijkstra(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long sourceDeviceId) {

        var devices = deviceRepository.findByProjectId(projectId);
        var connections = connectionRepository.findByProjectId(projectId);

        List<DijkstraEngine.GraphEdge> edges = new ArrayList<>();
        for (var c : connections) {
            String d1 = c.getInterface1().getDevice().getName();
            String d2 = c.getInterface2().getDevice().getName();
            int cost = c.getInterface1().getSpeed() != null && c.getInterface1().getSpeed().contains("100M") ? 10 : 1;
            edges.add(new DijkstraEngine.GraphEdge(d1, d2, cost));
        }

        String srcName = devices.isEmpty() ? "R1" : devices.get(0).getName();
        if (sourceDeviceId != null) {
            srcName = deviceRepository.findById(sourceDeviceId).map(Device::getName).orElse(srcName);
        }

        var result = dijkstraEngine.computeShortestPath(srcName, edges);
        return ResponseEntity.ok(ApiResponse.success(result, "Dijkstra SPF computed successfully"));
    }

    // --- 9. Dynamic Routing Protocols (OSPF, RIP, BGP, AODV) ---
    @GetMapping("/devices/{deviceId}/routing/ospf")
    public ResponseEntity<ApiResponse<OspfEngine.OspfStateDto>> getOspfState(@PathVariable Long deviceId) {
        Device dev = deviceRepository.findById(deviceId).orElseThrow();
        var intfs = interfaceRepository.findByDeviceId(deviceId);
        var state = ospfEngine.computeOspf(deviceId, dev, intfs, Collections.emptyList());
        return ResponseEntity.ok(ApiResponse.success(state, "OSPF State retrieved"));
    }

    @GetMapping("/devices/{deviceId}/routing/rip")
    public ResponseEntity<ApiResponse<RipEngine.RipStateDto>> getRipState(@PathVariable Long deviceId) {
        Device dev = deviceRepository.findById(deviceId).orElseThrow();
        var state = ripEngine.getRipState(deviceId);
        if (state == null) {
            state = ripEngine.initializeRip(deviceId, dev.getName(), List.of(
                    RipEngine.RipRouteEntry.builder().destinationNetwork("192.168.1.0/24").subnetMask("255.255.255.0").nextHop("0.0.0.0").outgoingInterface("eth0").hopCount(1).timerSeconds(30).build(),
                    RipEngine.RipRouteEntry.builder().destinationNetwork("192.168.2.0/24").subnetMask("255.255.255.0").nextHop("192.168.1.1").outgoingInterface("eth0").hopCount(2).timerSeconds(30).build()
            ));
        }
        return ResponseEntity.ok(ApiResponse.success(state, "RIP State retrieved"));
    }

    @GetMapping("/devices/{deviceId}/routing/bgp")
    public ResponseEntity<ApiResponse<BgpEngine.BgpStateDto>> getBgpState(@PathVariable Long deviceId) {
        Device dev = deviceRepository.findById(deviceId).orElseThrow();
        int asn = dev.getAsNumber() != null && dev.getAsNumber() > 0 ? dev.getAsNumber() : 65001;
        String rId = "10.0.0." + dev.getId();
        var state = bgpEngine.getBgpState(deviceId);
        if (state == null) {
            state = bgpEngine.configureBgp(deviceId, dev.getName(), asn, rId);
            bgpEngine.advertisePrefix(deviceId, "192.168." + dev.getId() + ".0/24", "0.0.0.0", List.of(asn));
        }
        return ResponseEntity.ok(ApiResponse.success(state, "BGP Instance retrieved"));
    }

    @GetMapping("/devices/{deviceId}/routing/aodv")
    public ResponseEntity<ApiResponse<List<AodvEngine.AodvRoute>>> getAodvRoutes(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "192.168.2.50") String targetIp) {
        Device dev = deviceRepository.findById(deviceId).orElseThrow();
        String nodeIp = "192.168.1." + dev.getId();
        aodvEngine.updateRoute(nodeIp, targetIp, "192.168.1.1", 2, 101);
        var routes = aodvEngine.getRoutes(nodeIp);
        return ResponseEntity.ok(ApiResponse.success(routes, "AODV Route Discovery simulated"));
    }

    // --- 10. Hardware CSMA and Wireless Standards ---
    @GetMapping("/devices/{deviceId}/hardware/csma")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCsmaInfo(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "1") int collisionCount) {
        var csmaCd = macControlEngine.simulateCsmaCd(collisionCount > 0, collisionCount);
        var csmaCa = macControlEngine.simulateCsmaCa(false, true);
        double delayMs = macControlEngine.calculateTransmissionDelayMs(1500, "100Mbps");

        Map<String, Object> map = new HashMap<>();
        map.put("collisionCount", collisionCount);
        map.put("csmaCd", csmaCd);
        map.put("csmaCa", csmaCa);
        map.put("txDelayMs", delayMs);
        return ResponseEntity.ok(ApiResponse.success(map, "CSMA/CD and CSMA/CA parameters evaluated"));
    }

    @GetMapping("/devices/{deviceId}/wireless/standards")
    public ResponseEntity<ApiResponse<Map<String, WirelessEngine.WirelessStandardInfo>>> getWirelessStandards(
            @PathVariable Long deviceId) {
        var standards = wirelessEngine.getSupportedStandards();
        return ResponseEntity.ok(ApiResponse.success(standards, "Wireless standards list retrieved"));
    }
}
