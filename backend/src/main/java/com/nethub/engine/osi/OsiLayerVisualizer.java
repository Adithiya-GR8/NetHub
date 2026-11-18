package com.nethub.engine.osi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OsiLayerVisualizer {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayerDetail {
        private int layerNumber; // 1 to 7
        private String osiLayerName; // Physical, Data Link, Network, Transport, Session, Presentation, Application
        private String tcpIpLayerName; // Network Access, Internet, Transport, Application
        private String pduName; // Bits, Frame, Packet, Segment/Datagram, Data
        private String protocol; // Ethernet, IPv4, TCP, HTTP, etc.
        private String action; // ENCAPSULATE or DECAPSULATE
        private Map<String, Object> headerFields;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OsiProcessResult {
        private String direction; // SENDER_ENCAPSULATION or RECEIVER_DECAPSULATION
        private String sourceDevice;
        private String destinationDevice;
        private String applicationType;
        private List<LayerDetail> layers;
    }

    public OsiProcessResult buildEncapsulationProcess(String srcDevice, String dstDevice, String srcMac, String dstMac, String srcIp, String dstIp, int srcPort, int dstPort, String appProtocol, String payloadText) {
        List<LayerDetail> layers = new ArrayList<>();

        // Layer 7: Application
        layers.add(LayerDetail.builder()
                .layerNumber(7)
                .osiLayerName("Application")
                .tcpIpLayerName("Application")
                .pduName("Data")
                .protocol(appProtocol)
                .action("ENCAPSULATE")
                .headerFields(Map.of("request", payloadText, "protocol", appProtocol))
                .explanation("User application constructs the original request payload data.")
                .build());

        // Layer 6: Presentation
        layers.add(LayerDetail.builder()
                .layerNumber(6)
                .osiLayerName("Presentation")
                .tcpIpLayerName("Application")
                .pduName("Formatted Data")
                .protocol("MIME / ASCII")
                .action("ENCAPSULATE")
                .headerFields(Map.of("charset", "UTF-8", "encoding", "identity", "compression", "none"))
                .explanation("Formats, encodes, or encrypts application data into a standardized transfer syntax.")
                .build());

        // Layer 5: Session
        layers.add(LayerDetail.builder()
                .layerNumber(5)
                .osiLayerName("Session")
                .tcpIpLayerName("Application")
                .pduName("Session Data")
                .protocol("Sockets API")
                .action("ENCAPSULATE")
                .headerFields(Map.of("sessionId", "0x" + UUID.randomUUID().toString().substring(0, 8), "duplex", "Full-Duplex"))
                .explanation("Manages dialogue control, establishing and coordinating the active connection session.")
                .build());

        // Layer 4: Transport
        layers.add(LayerDetail.builder()
                .layerNumber(4)
                .osiLayerName("Transport")
                .tcpIpLayerName("Transport")
                .pduName("Segment")
                .protocol("TCP")
                .action("ENCAPSULATE")
                .headerFields(Map.of("srcPort", srcPort, "dstPort", dstPort, "seq", 1000, "ack", 0, "flags", "[SYN, ACK]", "window", 65535))
                .explanation("Appends TCP/UDP transport header with port numbers, sequence/acknowledgement numbers, and checksum for reliable end-to-end communication.")
                .build());

        // Layer 3: Network
        layers.add(LayerDetail.builder()
                .layerNumber(3)
                .osiLayerName("Network")
                .tcpIpLayerName("Internet")
                .pduName("Packet")
                .protocol("IPv4")
                .action("ENCAPSULATE")
                .headerFields(Map.of("srcIp", srcIp, "dstIp", dstIp, "ttl", 64, "protocol", "TCP (6)", "totalLength", 84))
                .explanation("Appends IPv4 header containing logical source and destination IP addresses and initial Time-To-Live (TTL) for multi-hop internetwork routing.")
                .build());

        // Layer 2: Data Link
        layers.add(LayerDetail.builder()
                .layerNumber(2)
                .osiLayerName("Data Link")
                .tcpIpLayerName("Network Access")
                .pduName("Frame")
                .protocol("Ethernet II (IEEE 802.3)")
                .action("ENCAPSULATE")
                .headerFields(Map.of("srcMac", srcMac, "dstMac", dstMac, "etherType", "0x0800 (IPv4)", "fcs", "0x5E8C21B4 (CRC32)"))
                .explanation("Encapsulates packet into Ethernet frame, adding physical source and destination MAC addresses and Frame Check Sequence (FCS) error detection.")
                .build());

        // Layer 1: Physical
        layers.add(LayerDetail.builder()
                .layerNumber(1)
                .osiLayerName("Physical")
                .tcpIpLayerName("Network Access")
                .pduName("Bits")
                .protocol("1000BASE-T / 802.3ab")
                .action("ENCAPSULATE")
                .headerFields(Map.of("encoding", "Manchester / PAM-5", "medium", "Twisted Pair Copper", "clockRate", "125 MHz", "bandwidth", "1 Gbps"))
                .explanation("Converts frame bits into electrical or optical signals transmitted across the physical transmission medium.")
                .build());

        return OsiProcessResult.builder()
                .direction("SENDER_ENCAPSULATION")
                .sourceDevice(srcDevice)
                .destinationDevice(dstDevice)
                .applicationType(appProtocol)
                .layers(layers)
                .build();
    }

    public OsiProcessResult buildDecapsulationProcess(String srcDevice, String dstDevice, String srcMac, String dstMac, String srcIp, String dstIp, int srcPort, int dstPort, String appProtocol, String payloadText) {
        // Reverse process (Layers 1 up to 7)
        OsiProcessResult encap = buildEncapsulationProcess(srcDevice, dstDevice, srcMac, dstMac, srcIp, dstIp, srcPort, dstPort, appProtocol, payloadText);
        List<LayerDetail> decaps = new ArrayList<>(encap.getLayers());
        Collections.reverse(decaps);
        for (LayerDetail l : decaps) {
            l.setAction("DECAPSULATE");
        }

        return OsiProcessResult.builder()
                .direction("RECEIVER_DECAPSULATION")
                .sourceDevice(srcDevice)
                .destinationDevice(dstDevice)
                .applicationType(appProtocol)
                .layers(decaps)
                .build();
    }
}

