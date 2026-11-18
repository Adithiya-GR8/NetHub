package com.nethub.engine.hardware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WirelessEngine {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WirelessClient {
        private String clientMac;
        private String clientIp;
        private String deviceName;
        @Builder.Default
        private int rssi = -45; // dBm signal strength
        @Builder.Default
        private String status = "ASSOCIATED"; // AUTHENTICATING, ASSOCIATED, DISASSOCIATED
        @Builder.Default
        private LocalDateTime connectedAt = LocalDateTime.now();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WirelessStandardInfo {
        private String standard;
        private String frequency;
        private String maxDataRate;
        private String modulation;
        private String typicalRange;
        private String description;
    }

    // WAP deviceId -> List<WirelessClient>
    private final Map<Long, List<WirelessClient>> accessPointClients = new ConcurrentHashMap<>();

    public void associateClient(Long apDeviceId, String clientMac, String clientIp, String deviceName) {
        List<WirelessClient> clients = accessPointClients.computeIfAbsent(apDeviceId, k -> new ArrayList<>());
        clients.removeIf(c -> c.getClientMac().equalsIgnoreCase(clientMac));
        clients.add(WirelessClient.builder()
                .clientMac(clientMac)
                .clientIp(clientIp)
                .deviceName(deviceName)
                .rssi(-42 - (new Random().nextInt(20)))
                .status("ASSOCIATED")
                .connectedAt(LocalDateTime.now())
                .build());
    }

    public void disassociateClient(Long apDeviceId, String clientMac) {
        List<WirelessClient> clients = accessPointClients.get(apDeviceId);
        if (clients != null) {
            clients.removeIf(c -> c.getClientMac().equalsIgnoreCase(clientMac));
        }
    }

    public List<WirelessClient> getAssociatedClients(Long apDeviceId) {
        return accessPointClients.getOrDefault(apDeviceId, Collections.emptyList());
    }

    public Map<String, WirelessStandardInfo> getSupportedStandards() {
        Map<String, WirelessStandardInfo> map = new LinkedHashMap<>();
        map.put("802.11a", new WirelessStandardInfo("IEEE 802.11a", "5 GHz", "54 Mbps", "OFDM", "35 m", "High frequency, immune to 2.4GHz interference, shorter range."));
        map.put("802.11b", new WirelessStandardInfo("IEEE 802.11b", "2.4 GHz", "11 Mbps", "DSSS / CCK", "38 m", "Legacy widely deployed 2.4GHz Wi-Fi standard."));
        map.put("802.11g", new WirelessStandardInfo("IEEE 802.11g", "2.4 GHz", "54 Mbps", "OFDM", "38 m", "Backward-compatible with 802.11b at higher 54Mbps rates."));
        map.put("802.11n", new WirelessStandardInfo("IEEE 802.11n (Wi-Fi 4)", "2.4 & 5 GHz", "600 Mbps", "MIMO-OFDM", "70 m", "Introduced MIMO multi-antenna spatial multiplexing."));
        map.put("802.11ac", new WirelessStandardInfo("IEEE 802.11ac (Wi-Fi 5)", "5 GHz", "1.3 Gbps", "256-QAM MIMO", "35 m", "Gigabit wireless speeds using wide 80MHz/160MHz channels."));
        map.put("802.15.1", new WirelessStandardInfo("IEEE 802.15.1 (Bluetooth)", "2.4 GHz", "3 Mbps", "GFSK / FHSS", "10 m", "Short-range wireless personal area network (WPAN)."));
        map.put("802.15.4", new WirelessStandardInfo("IEEE 802.15.4 (Zigbee)", "2.4 GHz / 868 MHz", "250 kbps", "DSSS / BPSK", "100 m", "Low-power, low-data-rate IoT mesh sensor networks."));
        map.put("802.16", new WirelessStandardInfo("IEEE 802.16 (WiMAX)", "2 - 11 GHz", "75 Mbps", "SOFDMA", "50 km", "Broadband Wireless Metropolitan Area Access (WMAN)."));
        return map;
    }

    public void resetAll() {
        accessPointClients.clear();
    }
}
