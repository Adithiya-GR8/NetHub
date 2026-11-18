package com.nethub.engine;

import com.nethub.model.EthernetFrame;
import com.nethub.model.MacAddressTable;
import com.nethub.model.MacTableEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SwitchingEngine {

    // Per switch device MAC tables: switchDeviceId -> MacAddressTable
    private final Map<Long, MacAddressTable> switchMacTables = new ConcurrentHashMap<>();

    public MacAddressTable getMacTable(Long switchDeviceId) {
        return switchMacTables.computeIfAbsent(switchDeviceId, k -> new MacAddressTable());
    }

    public List<String> processFrame(Long switchDeviceId, EthernetFrame frame, String incomingPort, List<String> availablePorts) {
        return processFrame(switchDeviceId, frame, incomingPort, availablePorts, 1);
    }

    public List<String> processFrame(Long switchDeviceId, EthernetFrame frame, String incomingPort, List<String> availablePorts, Integer vlanId) {
        MacAddressTable macTable = getMacTable(switchDeviceId);
        int vlan = (vlanId != null && vlanId > 0) ? vlanId : 1;

        // Step 1: MAC Learning on ingress port and VLAN
        if (frame.getSourceMac() != null && incomingPort != null) {
            macTable.learn(frame.getSourceMac(), incomingPort, vlan);
        }

        List<String> targetPorts = new ArrayList<>();
        String destMac = frame.getDestinationMac();

        // Broadcast frame: flood to all ports except incoming port
        if (EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(destMac)) {
            for (String port : availablePorts) {
                if (!port.equalsIgnoreCase(incomingPort)) {
                    targetPorts.add(port);
                }
            }
            return targetPorts;
        }

        // Step 2: Unicast MAC Lookup within VLAN
        Optional<MacTableEntry> entryOpt = macTable.lookup(destMac, vlan);

        // Step 3: Forward or Flood
        if (entryOpt.isPresent()) {
            String targetPort = entryOpt.get().getPort();
            if (!targetPort.equalsIgnoreCase(incomingPort)) {
                targetPorts.add(targetPort);
            }
        } else {
            // Unknown destination MAC: Flood to all eligible ports except incoming
            for (String port : availablePorts) {
                if (!port.equalsIgnoreCase(incomingPort)) {
                    targetPorts.add(port);
                }
            }
        }

        return targetPorts;
    }

    public void clearMacTable(Long switchDeviceId) {
        MacAddressTable table = switchMacTables.get(switchDeviceId);
        if (table != null) {
            table.clear();
        }
    }

    public void resetAll() {
        switchMacTables.clear();
    }
}
