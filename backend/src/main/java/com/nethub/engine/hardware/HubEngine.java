package com.nethub.engine.hardware;

import com.nethub.model.EthernetFrame;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class HubEngine {

    // Hub deviceId -> collision count
    private final Map<Long, AtomicLong> collisionCounters = new ConcurrentHashMap<>();
    
    // Hub deviceId -> total broadcast signals
    private final Map<Long, AtomicLong> signalCounters = new ConcurrentHashMap<>();

    public List<String> processSignal(Long hubDeviceId, String incomingPort, List<String> availablePorts) {
        signalCounters.computeIfAbsent(hubDeviceId, k -> new AtomicLong()).incrementAndGet();

        List<String> targetPorts = new ArrayList<>();
        // Layer 1 Hub: Blindly repeat incoming electrical signal to ALL ports except incoming port
        for (String port : availablePorts) {
            if (!port.equalsIgnoreCase(incomingPort)) {
                targetPorts.add(port);
            }
        }
        return targetPorts;
    }

    public boolean detectCollision(Long hubDeviceId, int concurrentTransmissions) {
        if (concurrentTransmissions > 1) {
            collisionCounters.computeIfAbsent(hubDeviceId, k -> new AtomicLong()).incrementAndGet();
            return true;
        }
        return false;
    }

    public long getCollisionCount(Long hubDeviceId) {
        AtomicLong c = collisionCounters.get(hubDeviceId);
        return c != null ? c.get() : 0L;
    }

    public long getSignalCount(Long hubDeviceId) {
        AtomicLong s = signalCounters.get(hubDeviceId);
        return s != null ? s.get() : 0L;
    }

    public void resetAll() {
        collisionCounters.clear();
        signalCounters.clear();
    }
}
