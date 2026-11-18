package com.nethub.engine.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SocketApiEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimSocket {
        private int socketId;
        private Long deviceId;
        private String domain; // AF_INET
        private String type; // SOCK_STREAM, SOCK_DGRAM
        private String protocol; // IPPROTO_TCP, IPPROTO_UDP
        private String localIp;
        private int localPort;
        private String remoteIp;
        private int remotePort;
        private String state; // CREATED, BOUND, LISTENING, CONNECTING, ESTABLISHED, CLOSED
        private int backlog;
        @Builder.Default
        private List<String> buffer = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocketAppResult {
        private String appName;
        private String transportProtocol;
        private List<String> executionLog;
        private Map<String, Object> metrics;
    }

    private final Map<Integer, SimSocket> openSockets = new ConcurrentHashMap<>();
    private final AtomicInteger socketIdSequence = new AtomicInteger(100);

    public SimSocket createSocket(Long deviceId, String domain, String type) {
        int id = socketIdSequence.incrementAndGet();
        SimSocket socket = SimSocket.builder()
                .socketId(id)
                .deviceId(deviceId)
                .domain(domain != null ? domain : "AF_INET")
                .type(type != null ? type : "SOCK_STREAM")
                .protocol("SOCK_STREAM".equalsIgnoreCase(type) ? "IPPROTO_TCP" : "IPPROTO_UDP")
                .state("CREATED")
                .build();
        openSockets.put(id, socket);
        return socket;
    }

    public boolean bind(int socketId, String ip, int port) {
        SimSocket socket = openSockets.get(socketId);
        if (socket == null) return false;
        socket.setLocalIp(ip);
        socket.setLocalPort(port);
        socket.setState("BOUND");
        return true;
    }

    public boolean listen(int socketId, int backlog) {
        SimSocket socket = openSockets.get(socketId);
        if (socket == null || !"BOUND".equals(socket.getState())) return false;
        socket.setBacklog(backlog);
        socket.setState("LISTENING");
        return true;
    }

    public SimSocket accept(int serverSocketId, String clientIp, int clientPort) {
        SimSocket server = openSockets.get(serverSocketId);
        if (server == null || !"LISTENING".equals(server.getState())) return null;

        int newId = socketIdSequence.incrementAndGet();
        SimSocket clientConn = SimSocket.builder()
                .socketId(newId)
                .deviceId(server.getDeviceId())
                .domain(server.getDomain())
                .type(server.getType())
                .protocol(server.getProtocol())
                .localIp(server.getLocalIp())
                .localPort(server.getLocalPort())
                .remoteIp(clientIp)
                .remotePort(clientPort)
                .state("ESTABLISHED")
                .build();
        openSockets.put(newId, clientConn);
        return clientConn;
    }

    public boolean connect(int clientSocketId, String remoteIp, int remotePort) {
        SimSocket socket = openSockets.get(clientSocketId);
        if (socket == null) return false;
        socket.setRemoteIp(remoteIp);
        socket.setRemotePort(remotePort);
        socket.setState("ESTABLISHED");
        return true;
    }

    public boolean close(int socketId) {
        SimSocket socket = openSockets.remove(socketId);
        if (socket != null) {
            socket.setState("CLOSED");
            return true;
        }
        return false;
    }

    // --- Educational Socket Applications ---

    public SocketAppResult runUdpCalculator(String clientIp, String serverIp, String operation, double a, double b) {
        List<String> logs = new ArrayList<>();
        logs.add("[Client " + clientIp + "] socket(AF_INET, SOCK_DGRAM, 0) returned sockfd=101");
        logs.add("[Server " + serverIp + "] socket(AF_INET, SOCK_DGRAM, 0) returned sockfd=102");
        logs.add("[Server " + serverIp + "] bind(sockfd=102, port=8888) successful. Waiting for UDP datagrams...");

        double res = switch (operation.toUpperCase()) {
            case "ADD" -> a + b;
            case "SUB" -> a - b;
            case "MUL" -> a * b;
            case "DIV" -> (b != 0) ? (a / b) : Double.NaN;
            default -> 0.0;
        };

        String requestStr = operation + " " + a + " " + b;
        logs.add("[Client " + clientIp + "] sendto(sockfd=101, \"" + requestStr + "\", dest=" + serverIp + ":8888)");
        logs.add("[Server " + serverIp + "] recvfrom(sockfd=102, bytes=16) -> received \"" + requestStr + "\" from " + clientIp + ":54321");
        logs.add("[Server " + serverIp + "] Computed: " + a + " " + operation + " " + b + " = " + res);
        logs.add("[Server " + serverIp + "] sendto(sockfd=102, \"" + res + "\", dest=" + clientIp + ":54321)");
        logs.add("[Client " + clientIp + "] recvfrom(sockfd=101) -> Result: " + res);
        logs.add("[Client " + clientIp + "] close(sockfd=101)");

        return SocketAppResult.builder()
                .appName("UDP Calculator Server")
                .transportProtocol("UDP (SOCK_DGRAM)")
                .executionLog(logs)
                .metrics(Map.of("operation", operation, "operandA", a, "operandB", b, "result", res, "packetsSent", 2))
                .build();
    }

    public SocketAppResult runTcpMultiClientEcho(String serverIp, List<String> clientIps) {
        List<String> logs = new ArrayList<>();
        logs.add("[Server " + serverIp + "] socket(AF_INET, SOCK_STREAM, 0) -> sockfd=100");
        logs.add("[Server " + serverIp + "] bind(sockfd=100, port=9000) -> OK");
        logs.add("[Server " + serverIp + "] listen(sockfd=100, backlog=5) -> Listening for incoming TCP connections...");

        int clientCount = 0;
        for (String client : clientIps) {
            clientCount++;
            logs.add("  --- Client #" + clientCount + " (" + client + ") ---");
            logs.add("  [Client " + client + "] connect(server=" + serverIp + ":9000) -> 3-Way Handshake [SYN -> SYN-ACK -> ACK]");
            logs.add("  [Server " + serverIp + "] accept() -> spawned new worker socket clientFd=" + (100 + clientCount) + " for " + client);
            logs.add("  [Client " + client + "] send(\"Hello from " + client + "\")");
            logs.add("  [Server " + serverIp + "] recv() -> Echoed: \"ECHO: Hello from " + client + "\"");
            logs.add("  [Client " + client + "] close() -> Connection gracefully terminated [FIN-ACK]");
        }

        return SocketAppResult.builder()
                .appName("Multi-Client Concurrent TCP Echo Server")
                .transportProtocol("TCP (SOCK_STREAM)")
                .executionLog(logs)
                .metrics(Map.of("totalClientsServed", clientCount, "serverPort", 9000, "concurrencyModel", "Thread-per-client simulated"))
                .build();
    }

    public void resetAll() {
        openSockets.clear();
    }
}
