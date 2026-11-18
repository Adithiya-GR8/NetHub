package com.nethub.engine.hardware;

import com.nethub.model.EthernetFrame;
import com.nethub.model.MacAddressTable;
import com.nethub.model.MacTableEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BridgeEngine {

    // Bridge deviceId -> MacAddressTable (Segment Table)
    private final Map<Long, MacAddressTable> bridgeTables = new ConcurrentHashMap<>();

    public MacAddressTable getBridgeTable(Long bridgeDeviceId) {
        return bridgeTables.computeIfAbsent(bridgeDeviceId, k -> new MacAddressTable());
    }

    public List<String> processFrame(Long bridgeDeviceId, EthernetFrame frame, String incomingPort, List<String> availablePorts) {
        MacAddressTable table = getBridgeTable(bridgeDeviceId);

        // Learn source MAC on incoming segment/port
        if (frame.getSourceMac() != null && incomingPort != null) {
            table.learn(frame.getSourceMac(), incomingPort);
        }

        String destMac = frame.getDestinationMac();

        // Broadcast: forward to all other segments
        if (EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(destMac)) {
            List<String> broadcastPorts = new ArrayList<>();
            for (String p : availablePorts) {
                if (!p.equalsIgnoreCase(incomingPort)) {
                    broadcastPorts.add(p);
                }
            }
            return broadcastPorts;
        }

        // Unicast lookup
        Optional<MacTableEntry> match = table.lookup(destMac);
        if (match.isPresent()) {
            String targetPort = match.get().getPort();
            // Same segment filter: if destination MAC is located on the SAME incoming segment, FILTER/DROP!
            if (targetPort.equalsIgnoreCase(incomingPort)) {
                return Collections.emptyList(); // Filtered (keeps local traffic local)
            }
            // Different segment: forward across bridge
            return Collections.singletonList(targetPort);
        }

        // Unknown destination: flood to all other segments
        List<String> floodPorts = new ArrayList<>();
        for (String p : availablePorts) {
            if (!p.equalsIgnoreCase(incomingPort)) {
                floodPorts.add(p);
            }
        }
        return floodPorts;
    }

    public void resetAll() {
        bridgeTables.clear();
    }
}
