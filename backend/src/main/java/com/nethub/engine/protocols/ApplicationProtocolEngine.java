package com.nethub.engine.protocols;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ApplicationProtocolEngine {

    // --- DNS Models & Service ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DnsResolutionResult {
        private String domain;
        private String resolvedIp;
        private String responseCode; // NOERROR, NXDOMAIN
        private int ttlSeconds;
        private boolean fromCache;
        private List<String> queryTrace;
    }

    private final Map<String, String> authoritativeDnsRecords = new HashMap<>() {{
        put("web.nethub.local", "192.168.2.80");
        put("server1.local", "192.168.2.10");
        put("dns.nethub.local", "192.168.1.53");
        put("router1.local", "192.168.1.1");
        put("gateway.local", "192.168.1.254");
    }};

    public DnsResolutionResult resolveDns(String domain) {
        String normalized = domain.toLowerCase().trim();
        List<String> trace = new ArrayList<>();
        trace.add("Client queried local DNS resolver for '" + normalized + "' (Type A Record)");

        if (authoritativeDnsRecords.containsKey(normalized)) {
            String ip = authoritativeDnsRecords.get(normalized);
            trace.add("Authoritative DNS server matched: " + normalized + " -> " + ip + " (TTL=300)");
            return DnsResolutionResult.builder()
                    .domain(normalized)
                    .resolvedIp(ip)
                    .responseCode("NOERROR")
                    .ttlSeconds(300)
                    .fromCache(false)
                    .queryTrace(trace)
                    .build();
        } else {
            trace.add("Authoritative DNS server queried: No matching A record for domain '" + normalized + "'");
            return DnsResolutionResult.builder()
                    .domain(normalized)
                    .resolvedIp(null)
                    .responseCode("NXDOMAIN")
                    .ttlSeconds(60)
                    .fromCache(false)
                    .queryTrace(trace)
                    .build();
        }
    }

    // --- TLS / HTTPS Handshake Simulation ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TlsHandshakeStep {
        private int stepNumber;
        private String direction; // CLIENT_TO_SERVER or SERVER_TO_CLIENT
        private String messageName; // ClientHello, ServerHello, etc.
        private Map<String, Object> parameters;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TlsHandshakeResult {
        private String clientHost;
        private String serverHost;
        private String tlsVersion; // TLS 1.3
        private String negotiatedCipher; // TLS_AES_256_GCM_SHA384
        private boolean handshakeSuccessful;
        private List<TlsHandshakeStep> steps;
    }

    public TlsHandshakeResult simulateTlsHandshake(String client, String server) {
        List<TlsHandshakeStep> steps = new ArrayList<>();

        // Step 1: ClientHello
        steps.add(TlsHandshakeStep.builder()
                .stepNumber(1)
                .direction("CLIENT_TO_SERVER")
                .messageName("ClientHello")
                .parameters(Map.of("tlsVersion", "TLS 1.3", "cipherSuites", List.of("TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256"), "keyShare", "ECDH x25519 Public Key"))
                .explanation("Client initiates TLS session, advertises supported ciphers, and sends ECDH ephemeral key share.")
                .build());

        // Step 2: ServerHello + EncryptedExtensions + Certificate + Finished
        steps.add(TlsHandshakeStep.builder()
                .stepNumber(2)
                .direction("SERVER_TO_CLIENT")
                .messageName("ServerHello, Certificate, Finished")
                .parameters(Map.of("selectedCipher", "TLS_AES_256_GCM_SHA384", "serverCertificate", "CN=" + server + ", Issuer=NetHub Root CA", "signature", "ECDSA-SHA384"))
                .explanation("Server agrees on TLS 1.3 cipher, transmits X.509 digital certificate for authentication, and completes key exchange.")
                .build());

        // Step 3: Client Finished + Encrypted Application Data
        steps.add(TlsHandshakeStep.builder()
                .stepNumber(3)
                .direction("CLIENT_TO_SERVER")
                .messageName("ClientFinished & Encrypted HTTP Request")
                .parameters(Map.of("sessionStatus", "SECURE_SESSION_ESTABLISHED", "symmetricKeyLength", "256 bits AES-GCM"))
                .explanation("Client authenticates server certificate, computes shared symmetric session key, and transmits encrypted application data (HTTPS).")
                .build());

        return TlsHandshakeResult.builder()
                .clientHost(client)
                .serverHost(server)
                .tlsVersion("TLS 1.3")
                .negotiatedCipher("TLS_AES_256_GCM_SHA384")
                .handshakeSuccessful(true)
                .steps(steps)
                .build();
    }

    // --- FTP Simulation ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FtpTransactionResult {
        private String serverHost;
        private String command; // USER, PASS, LIST, RETR, STOR
        private int controlPort; // 21
        private int dataPort; // 20
        private List<String> transcript;
        private boolean success;
    }

    public FtpTransactionResult simulateFtp(String server, String username, String command, String filename) {
        List<String> logs = new ArrayList<>();
        logs.add("Connected to " + server + " on Control Port 21.");
        logs.add("< 220 NetHub FTP Service Ready.");
        logs.add("> USER " + username);
        logs.add("< 331 User name okay, need password.");
        logs.add("> PASS ********");
        logs.add("< 230 User logged in, proceed.");

        if ("LIST".equalsIgnoreCase(command)) {
            logs.add("> PASV");
            logs.add("< 227 Entering Passive Mode (192,168,2,10,195,84).");
            logs.add("> LIST");
            logs.add("< 150 Here comes the directory listing.");
            logs.add("  -rw-r--r--   1 root  admin     1024 Jan 15 10:00 index.html");
            logs.add("  -rw-r--r--   1 root  admin    65536 Jan 15 10:05 network_backup.cfg");
            logs.add("< 226 Directory send OK.");
        } else if ("RETR".equalsIgnoreCase(command)) {
            logs.add("> RETR " + filename);
            logs.add("< 150 Opening BINARY mode data connection for " + filename + ".");
            logs.add("< 226 Transfer complete (65536 bytes transferred in 0.05 secs).");
        }
        logs.add("> QUIT");
        logs.add("< 221 Goodbye.");

        return FtpTransactionResult.builder()
                .serverHost(server)
                .command(command)
                .controlPort(21)
                .dataPort(20)
                .transcript(logs)
                .success(true)
                .build();
    }

    // --- TFTP Simulation ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TftpTransactionResult {
        private String serverHost;
        private String opcode; // RRQ (Read Request), WRQ (Write Request)
        private String filename;
        private int totalBlocks;
        private int blockSizeBytes; // 512 bytes standard
        private List<String> blockEvents;
    }

    public TftpTransactionResult simulateTftp(String server, String opcode, String filename, int totalBytes) {
        int blockSize = 512;
        int blocks = (int) Math.ceil((double) totalBytes / blockSize);
        if (blocks < 1) blocks = 1;

        List<String> events = new ArrayList<>();
        events.add("Sent TFTP " + opcode + " for '" + filename + "' (mode: octet) to " + server + ":69");
        for (int b = 1; b <= blocks; b++) {
            events.add("Received DATA block #" + b + " (512 bytes) -> Transmitted ACK #" + b);
        }
        events.add("TFTP File Transfer Complete (" + totalBytes + " bytes transferred via UDP).");

        return TftpTransactionResult.builder()
                .serverHost(server)
                .opcode(opcode)
                .filename(filename)
                .totalBlocks(blocks)
                .blockSizeBytes(blockSize)
                .blockEvents(events)
                .build();
    }

    // --- Email Simulation (SMTP & POP3) ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailTransactionResult {
        private String mailServer;
        private String sender;
        private String recipient;
        private String subject;
        private String mimeType;
        private List<String> smtpTranscript;
        private List<String> pop3Transcript;
    }

    public EmailTransactionResult simulateEmail(String mailServer, String sender, String recipient, String subject, String body) {
        List<String> smtp = new ArrayList<>();
        smtp.add("< 220 " + mailServer + " ESMTP NetHub Mail Service ready");
        smtp.add("> HELO client.local");
        smtp.add("< 250 Hello client.local, pleased to meet you");
        smtp.add("> MAIL FROM:<" + sender + ">");
        smtp.add("< 250 2.1.0 Sender <" + sender + "> OK");
        smtp.add("> RCPT TO:<" + recipient + ">");
        smtp.add("< 250 2.1.5 Recipient <" + recipient + "> OK");
        smtp.add("> DATA");
        smtp.add("< 354 Start mail input; end with <CRLF>.<CRLF>");
        smtp.add("> From: " + sender);
        smtp.add("> To: " + recipient);
        smtp.add("> Subject: " + subject);
        smtp.add("> MIME-Version: 1.0");
        smtp.add("> Content-Type: multipart/mixed; boundary=\"_nethub_sep_\"");
        smtp.add("> " + body);
        smtp.add("> .");
        smtp.add("< 250 2.0.0 Message queued for delivery as 4F82910");
        smtp.add("> QUIT");
        smtp.add("< 221 2.0.0 " + mailServer + " closing transmission channel");

        List<String> pop3 = new ArrayList<>();
        pop3.add("< +OK POP3 server ready <1896.697082@mail.nethub.local>");
        pop3.add("> USER " + recipient);
        pop3.add("< +OK User accepted");
        pop3.add("> PASS ********");
        pop3.add("< +OK Mailbox open, 1 message (1024 octets)");
        pop3.add("> STAT");
        pop3.add("< +OK 1 1024");
        pop3.add("> RETR 1");
        pop3.add("< +OK 1024 octets (Subject: " + subject + ")");
        pop3.add("> QUIT");
        pop3.add("< +OK POP3 server closing connection");

        return EmailTransactionResult.builder()
                .mailServer(mailServer)
                .sender(sender)
                .recipient(recipient)
                .subject(subject)
                .mimeType("multipart/mixed; boundary=\"_nethub_sep_\"")
                .smtpTranscript(smtp)
                .pop3Transcript(pop3)
                .build();
    }
}

