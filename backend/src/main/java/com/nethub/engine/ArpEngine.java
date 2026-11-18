package com.nethub.engine;

import com.nethub.model.ArpEntry;
import com.nethub.model.ArpPacket;
import com.nethub.model.ArpTable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ArpEngine {

    // Per device ARP caches: deviceId -> ArpTable
    private final Map<Long, ArpTable> deviceArpTables = new ConcurrentHashMap<>();

    public ArpTable getArpTable(Long deviceId) {
        return deviceArpTables.computeIfAbsent(deviceId, k -> new ArpTable());
    }

    public Optional<String> lookupMac(Long deviceId, String targetIp) {
        ArpTable arpTable = getArpTable(deviceId);
        return arpTable.lookup(targetIp).map(ArpEntry::getMacAddress);
    }

    public ArpPacket createArpRequest(String senderMac, String senderIp, String targetIp) {
        return ArpPacket.builder()
                .operation(ArpPacket.OP_REQUEST)
                .senderMac(senderMac)
                .senderIp(senderIp)
                .targetMac("00:00:00:00:00:00") // Unknown target MAC in request
                .targetIp(targetIp)
                .build();
    }

    public ArpPacket createArpReply(String targetDeviceMac, String targetIp, String requesterMac, String requesterIp) {
        return ArpPacket.builder()
                .operation(ArpPacket.OP_REPLY)
                .senderMac(targetDeviceMac)
                .senderIp(targetIp)
                .targetMac(requesterMac)
                .targetIp(requesterIp)
                .build();
    }

    public void updateArpCache(Long deviceId, String ipAddress, String macAddress) {
        ArpTable table = getArpTable(deviceId);
        table.put(ipAddress, macAddress);
    }

    public void clearArpCache(Long deviceId) {
        ArpTable table = deviceArpTables.get(deviceId);
        if (table != null) {
            table.clear();
        }
    }

    public void resetAll() {
        deviceArpTables.clear();
    }
}
