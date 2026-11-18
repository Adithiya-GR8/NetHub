package com.nethub.engine.simulation;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.engine.hardware.BridgeEngine;
import com.nethub.engine.hardware.HubEngine;
import com.nethub.engine.hardware.MacControlEngine;
import com.nethub.engine.hardware.WirelessEngine;
import com.nethub.engine.security.AclEngine;
import com.nethub.engine.security.NatEngine;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.model.*;
import com.nethub.service.DeviceStateManager;
import com.nethub.service.RoutingTableService;
import com.nethub.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DiscreteNetworkSimulator {

    private final ArpEngine arpEngine;
    private final SwitchingEngine switchingEngine;
    private final RoutingEngine routingEngine;
    private final RoutingTableService routingTableService;
    private final DeviceStateManager stateManager;
    private final HubEngine hubEngine;
    private final BridgeEngine bridgeEngine;
    private final WirelessEngine wirelessEngine;
    private final MacControlEngine macControlEngine;
    private final AclEngine aclEngine;
    private final NatEngine natEngine;

    @Autowired
    public DiscreteNetworkSimulator(ArpEngine arpEngine,
                                    SwitchingEngine switchingEngine,
                                    RoutingEngine routingEngine,
                                    RoutingTableService routingTableService,
                                    DeviceStateManager stateManager,
                                    HubEngine hubEngine,
                                    BridgeEngine bridgeEngine,
                                    WirelessEngine wirelessEngine,
                                    MacControlEngine macControlEngine,
                                    AclEngine aclEngine,
                                    NatEngine natEngine) {
        this.arpEngine = arpEngine;
        this.switchingEngine = switchingEngine;
        this.routingEngine = routingEngine;
        this.routingTableService = routingTableService;
        this.stateManager = stateManager;
        this.hubEngine = hubEngine;
        this.bridgeEngine = bridgeEngine;
        this.wirelessEngine = wirelessEngine;
        this.macControlEngine = macControlEngine;
        this.aclEngine = aclEngine;
        this.natEngine = natEngine;
    }

    // Backward-compatible constructor for existing tests
    public DiscreteNetworkSimulator(ArpEngine arpEngine,
                                    SwitchingEngine switchingEngine,
                                    RoutingEngine routingEngine,
                                    RoutingTableService routingTableService,
                                    DeviceStateManager stateManager) {
        this.arpEngine = arpEngine;
        this.switchingEngine = switchingEngine;
        this.routingEngine = routingEngine;
        this.routingTableService = routingTableService;
        this.stateManager = stateManager;
        this.hubEngine = new HubEngine();
        this.bridgeEngine = new BridgeEngine();
        this.wirelessEngine = new WirelessEngine();
        this.macControlEngine = new MacControlEngine();
        this.aclEngine = new AclEngine();
        this.natEngine = new NatEngine();
    }

    public SimulationResult runSimulation(SimContext ctx, Long sourceDeviceId, String destinationIp, String protocolType, int initialTtl) {
        Device sourceDevice = ctx.getDevice(sourceDeviceId);
        if (sourceDevice == null) {
            return SimulationResult.builder()
                    .success(false)
                    .errorCode("DEVICE_NOT_FOUND")
                    .errorMessage("Source device not found with ID: " + sourceDeviceId)
                    .events(Collections.emptyList())
                    .build();
        }

        if (sourceDevice.getStatus() != DeviceStatus.ONLINE) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .action("DROP")
                    .dropReason("DEVICE_OFFLINE")
                    .description("Source device is OFFLINE")
                    .explanation("An offline device cannot generate, process, or transmit network frames.")
                    .build());

            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("SOURCE_DEVICE_OFFLINE")
                    .errorMessage("Source device " + sourceDevice.getName() + " is offline.")
                    .events(ctx.getEvents())
                    .build();
        }

        if (!IpUtils.isValidIPv4(destinationIp)) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .action("DROP")
                    .dropReason("INVALID_IP")
                    .description("Invalid destination IPv4 format: " + destinationIp)
                    .explanation("The destination address must conform to standard dotted-decimal IPv4 notation (e.g. 192.168.1.10).")
                    .build());

            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("INVALID_DESTINATION_IP")
                    .errorMessage("Invalid destination IP format.")
                    .events(ctx.getEvents())
                    .build();
        }

        ctx.addEvent(SimulationEvent.builder()
                .type("SOURCE_VALIDATED")
                .deviceId(sourceDevice.getId())
                .device(sourceDevice.getName())
                .action("VALIDATE")
                .description("Validated source device " + sourceDevice.getName() + " (ONLINE)")
                .explanation("Device power state is ONLINE, hardware interfaces initialized.")
                .build());

        // Select suitable source interface
        List<NetworkInterface> interfaces = ctx.getDeviceInterfaces(sourceDeviceId);
        NetworkInterface sourceIntf = null;

        // Prefer an interface on the same local subnet as destination
        for (NetworkInterface ni : interfaces) {
            if (ni.getIpAddress() != null && !ni.getIpAddress().trim().isEmpty() && ni.getSubnetMask() != null) {
                if (IpUtils.isLocal(ni.getIpAddress(), ni.getSubnetMask(), destinationIp)) {
                    sourceIntf = ni;
                    break;
                }
            }
        }

        // If not on local subnet, pick first interface with IP and gateway configured
        if (sourceIntf == null) {
            for (NetworkInterface ni : interfaces) {
                if (ni.getIpAddress() != null && !ni.getIpAddress().trim().isEmpty()) {
                    sourceIntf = ni;
                    break;
                }
            }
        }

        if (sourceIntf == null) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .action("DROP")
                    .dropReason("INTERFACE_DOWN")
                    .description("No network interface with a configured IP address found on " + sourceDevice.getName())
                    .explanation("A host requires at least one active network interface with an IP address and subnet mask to communicate.")
                    .build());

            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("INTERFACE_DOWN")
                    .errorMessage("Source device has no configured network interface.")
                    .events(ctx.getEvents())
                    .build();
        }

        // Check if interface is operationally UP
        if (!ctx.isInterfaceOperationallyUp(sourceIntf)) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .interfaceName(sourceIntf.getName())
                    .action("DROP")
                    .dropReason("LINK_DOWN")
                    .description("Interface " + sourceIntf.getName() + " is operationally DOWN (disconnected cable or remote peer offline)")
                    .explanation("Layer 1/2 physical link check failed: Interface cannot transmit data without an active physical connection to an online peer.")
                    .build());

            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .errorCode("LINK_DOWN")
                    .errorMessage("Interface " + sourceIntf.getName() + " link is operationally down.")
                    .events(ctx.getEvents())
                    .build();
        }

        String sourceIp = sourceIntf.getIpAddress();
        String sourceMask = sourceIntf.getSubnetMask();
        boolean isLocal = IpUtils.isLocal(sourceIp, sourceMask, destinationIp);

        ctx.addEvent(SimulationEvent.builder()
                .type("SUBNET_CHECK")
                .deviceId(sourceDevice.getId())
                .device(sourceDevice.getName())
                .interfaceName(sourceIntf.getName())
                .action("INSPECT")
                .description("Subnet evaluation: Destination " + destinationIp + " is " + (isLocal ? "LOCAL" : "REMOTE") + " relative to " + sourceIp + "/" + IpUtils.maskToCidr(sourceMask))
                .explanation(isLocal
                        ? "Bitwise AND comparison between destination IP and subnet mask matches the local network address. Direct Layer 2 delivery via ARP."
                        : "Destination network does not match local subnet. Traffic must be forwarded to the Default Gateway router for Layer 3 inter-network routing.")
                .build());

        String targetResolutionIp = isLocal ? destinationIp : sourceIntf.getGateway();

        if (!isLocal && (targetResolutionIp == null || targetResolutionIp.trim().isEmpty())) {
            // Check device-level default gateway fallback
            if (sourceDevice.getDefaultGateway() != null && !sourceDevice.getDefaultGateway().trim().isEmpty()) {
                targetResolutionIp = sourceDevice.getDefaultGateway().trim();
            } else {
                ctx.addEvent(SimulationEvent.builder()
                        .type("ERROR")
                        .deviceId(sourceDevice.getId())
                        .device(sourceDevice.getName())
                        .interfaceName(sourceIntf.getName())
                        .action("DROP")
                        .dropReason("NO_DEFAULT_GATEWAY")
                        .description("Cannot route to remote destination " + destinationIp + ": No default gateway configured.")
                        .explanation("When communicating with an external or remote IP, a host must send packets to a gateway router.")
                        .build());

                return SimulationResult.builder()
                        .success(false)
                        .source(sourceDevice.getName())
                        .destination(destinationIp)
                        .errorCode("NO_DEFAULT_GATEWAY")
                        .errorMessage("No default gateway configured for remote transmission.")
                        .events(ctx.getEvents())
                        .build();
            }
        }

        // Resolve Target MAC using ARP (Local target or Default Gateway)
        Optional<String> cachedMacOpt = arpEngine.lookupMac(sourceDeviceId, targetResolutionIp);
        String targetMac;

        if (cachedMacOpt.isPresent()) {
            targetMac = cachedMacOpt.get();
            ctx.addEvent(SimulationEvent.builder()
                    .type("ARP_CACHE_HIT")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .interfaceName(sourceIntf.getName())
                    .action("LOOKUP")
                    .description("ARP Cache Hit: " + targetResolutionIp + " is mapped to " + targetMac)
                    .explanation("The device found a valid mapping for " + targetResolutionIp + " in its local ARP table, avoiding an ARP broadcast query.")
                    .details(Map.of("ip", targetResolutionIp, "mac", targetMac))
                    .build());
        } else {
            // ARP Request Broadcasting
            targetMac = performArpResolution(ctx, sourceDevice, sourceIntf, targetResolutionIp);
            if (targetMac == null) {
                ctx.addEvent(SimulationEvent.builder()
                        .type("ERROR")
                        .deviceId(sourceDevice.getId())
                        .device(sourceDevice.getName())
                        .interfaceName(sourceIntf.getName())
                        .action("DROP")
                        .dropReason("ARP_TIMEOUT")
                        .description("ARP Resolution Failed: No reply received for " + targetResolutionIp)
                        .explanation("The sender broadcasted an ARP Request asking 'Who has " + targetResolutionIp + "?', but received no reply within the timeout period.")
                        .build());

                return SimulationResult.builder()
                        .success(false)
                        .source(sourceDevice.getName())
                        .destination(destinationIp)
                        .errorCode("ARP_RESOLUTION_FAILED")
                        .errorMessage("ARP resolution failed for " + targetResolutionIp)
                        .events(ctx.getEvents())
                        .build();
            }
        }

        // Build Higher-Layer Payload (ICMP, TCP, HTTP, UDP, etc.)
        Object upperPayload = createUpperPayload(protocolType, sourceIp, destinationIp);
        int ipProtocol = determineIpProtocol(protocolType);

        IpPacket ipPacket = IpPacket.builder()
                .version(4)
                .ihl(5)
                .sourceIp(sourceIp)
                .destinationIp(destinationIp)
                .ttl(initialTtl > 0 ? initialTtl : 64)
                .protocol(ipProtocol)
                .totalLength(84)
                .payload(upperPayload)
                .build();

        EthernetFrame frame = EthernetFrame.builder()
                .sourceMac(sourceIntf.getMacAddress())
                .destinationMac(targetMac)
                .etherType(EthernetFrame.ETHERTYPE_IPV4)
                .vlanId(sourceIntf.getVlanId() != null ? sourceIntf.getVlanId() : 1)
                .frameSize(ipPacket.getTotalLength() + 18)
                .payload(ipPacket)
                .build();

        ctx.addEvent(SimulationEvent.builder()
                .type("FRAME_CREATED")
                .deviceId(sourceDevice.getId())
                .device(sourceDevice.getName())
                .interfaceName(sourceIntf.getName())
                .action("GENERATE")
                .description("Constructed Ethernet II Frame [" + sourceIntf.getMacAddress() + " -> " + targetMac + "] carrying IPv4 (" + ipPacket.getProtocolName() + ")")
                .explanation("Encapsulated Layer 3 IP Packet into Layer 2 Ethernet Frame with destination MAC matching the next hop.")
                .frameSnapshot(frame.copy())
                .details(Map.of("srcMac", sourceIntf.getMacAddress(), "dstMac", targetMac, "srcIp", sourceIp, "dstIp", destinationIp, "ttl", ipPacket.getTtl()))
                .build());

        // Transmit frame across physical link into network
        stateManager.recordPacketSent(sourceIntf.getId(), frame.getFrameSize());
        List<String> traversedPath = new ArrayList<>();
        traversedPath.add(sourceDevice.getName());

        NetworkInterface peerIntf = ctx.findPeerInterface(sourceIntf.getId());
        if (peerIntf == null) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(sourceDevice.getId())
                    .device(sourceDevice.getName())
                    .action("DROP")
                    .dropReason("DISCONNECTED_CABLE")
                    .description("Frame dropped at physical layer: Cable is unplugged from " + sourceIntf.getName())
                    .explanation("Signals cannot propagate across an unattached cable port.")
                    .build());

            return SimulationResult.builder()
                    .success(false)
                    .source(sourceDevice.getName())
                    .destination(destinationIp)
                    .path(traversedPath)
                    .errorCode("DISCONNECTED_CABLE")
                    .errorMessage("Cable is disconnected from interface " + sourceIntf.getName())
                    .events(ctx.getEvents())
                    .build();
        }

        // Process frame hop-by-hop through connected devices
        boolean delivered = processHop(ctx, peerIntf, frame, traversedPath, 0);

        return SimulationResult.builder()
                .success(delivered)
                .source(sourceDevice.getName())
                .destination(destinationIp)
                .path(traversedPath)
                .events(ctx.getEvents())
                .errorCode(delivered ? null : "TRANSMISSION_FAILED")
                .errorMessage(delivered ? null : "Frame delivery was not completed.")
                .build();
    }

    /**
     * Executes realistic ARP Request broadcast and unicast ARP Reply across topology
     */
    private String performArpResolution(SimContext ctx, Device requesterDevice, NetworkInterface requesterIntf, String targetIp) {
        ctx.addEvent(SimulationEvent.builder()
                .type("ARP_REQUEST")
                .deviceId(requesterDevice.getId())
                .device(requesterDevice.getName())
                .interfaceName(requesterIntf.getName())
                .action("BROADCAST")
                .description("ARP Request Broadcast: Who has " + targetIp + "? Tell " + requesterIntf.getIpAddress())
                .explanation("The host broadcasts an ARP query (Destination MAC FF:FF:FF:FF:FF:FF) on the local broadcast domain to discover the target's Layer 2 hardware address.")
                .details(Map.of("targetIp", targetIp, "senderIp", requesterIntf.getIpAddress(), "senderMac", requesterIntf.getMacAddress()))
                .build());

        // Find which device owns the target IP in the topology
        NetworkInterface targetIntf = null;
        Device targetDevice = null;
        for (NetworkInterface ni : ctx.getInterfaces()) {
            if (targetIp.equalsIgnoreCase(ni.getIpAddress())) {
                targetIntf = ni;
                targetDevice = ctx.getDevice(ni.getDeviceId());
                break;
            }
        }

        if (targetDevice == null || targetIntf == null) {
            return null;
        }

        // Check if target device is online and interface is up
        if (targetDevice.getStatus() != DeviceStatus.ONLINE || !ctx.isInterfaceOperationallyUp(targetIntf)) {
            return null;
        }

        // Record ARP cache updates on target device
        arpEngine.updateArpCache(targetDevice.getId(), requesterIntf.getIpAddress(), requesterIntf.getMacAddress(), ArpEntry.STATE_REACHABLE, targetIntf.getName());

        // Generate ARP Reply from target device
        ctx.addEvent(SimulationEvent.builder()
                .type("ARP_REPLY")
                .deviceId(targetDevice.getId())
                .device(targetDevice.getName())
                .interfaceName(targetIntf.getName())
                .action("UNICAST")
                .description("ARP Reply from " + targetDevice.getName() + ": " + targetIp + " is at " + targetIntf.getMacAddress())
                .explanation("The target host recognizes its own IP, updates its ARP cache with the requester's mapping, and sends a unicast ARP Reply with its MAC address.")
                .details(Map.of("ip", targetIp, "mac", targetIntf.getMacAddress(), "replyTo", requesterIntf.getMacAddress()))
                .build());

        // Update requester ARP cache
        arpEngine.updateArpCache(requesterDevice.getId(), targetIp, targetIntf.getMacAddress(), ArpEntry.STATE_REACHABLE, requesterIntf.getName());
        return targetIntf.getMacAddress();
    }

    /**
     * Recursively processes an incoming frame on an ingress interface of a device
     */
    private boolean processHop(SimContext ctx, NetworkInterface ingressIntf, EthernetFrame frame, List<String> traversedPath, int depth) {
        if (depth > 25) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .action("DROP")
                    .dropReason("ROUTING_LOOP")
                    .description("Frame dropped: Maximum hop limit (25) exceeded. Network loop detected.")
                    .explanation("The packet recirculated excessively across devices without reaching its destination.")
                    .build());
            return false;
        }

        Device currentDevice = ctx.getDevice(ingressIntf.getDeviceId());
        if (currentDevice == null || currentDevice.getStatus() != DeviceStatus.ONLINE) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .action("DROP")
                    .dropReason("DEVICE_OFFLINE")
                    .description("Frame dropped: Receiving device is OFFLINE")
                    .build());
            return false;
        }

        if (!traversedPath.contains(currentDevice.getName())) {
            traversedPath.add(currentDevice.getName());
        }

        stateManager.recordPacketReceived(ingressIntf.getId(), frame.getFrameSize());

        // Route by Device Type across all supported hardware types
        switch (currentDevice.getType()) {
            case SWITCH:
                return processSwitchHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case ROUTER:
                return processRouterHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case HUB:
                return processHubHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case REPEATER:
                return processRepeaterHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case BRIDGE:
                return processBridgeHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case ACCESS_POINT:
                return processAccessPointHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case MODEM:
                return processModemHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case GATEWAY:
                return processGatewayHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case PC:
            case SERVER:
                return processHostHop(ctx, currentDevice, ingressIntf, frame);

            default:
                return false;
        }
    }

    /**
     * Layer 1 Hub Processing: Blindly repeat incoming electrical signal to all other ports
     */
    private boolean processHubHop(SimContext ctx, Device hub, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        List<NetworkInterface> allPorts = ctx.getDeviceInterfaces(hub.getId());
        List<String> availablePortNames = allPorts.stream().map(NetworkInterface::getName).toList();
        List<String> targetPorts = hubEngine.processSignal(hub.getId(), ingressPort.getName(), availablePortNames);

        ctx.addEvent(SimulationEvent.builder()
                .type("HUB_BROADCAST")
                .deviceId(hub.getId())
                .device(hub.getName())
                .interfaceName(ingressPort.getName())
                .action("BROADCAST")
                .description("Hub " + hub.getName() + " (Layer 1) repeated electrical signal from " + ingressPort.getName() + " to all connected ports: " + targetPorts)
                .explanation("A multiport repeater (Hub) operates purely at Physical Layer 1. It does not inspect MAC or IP headers; it regenerates bits and repeats them unconditionally to all ports within its shared collision domain.")
                .frameSnapshot(frame.copy())
                .details(Map.of("targetPorts", targetPorts, "halfDuplex", true, "sharedBandwidth", true))
                .build());

        boolean anyDelivered = false;
        for (String outPortName : targetPorts) {
            NetworkInterface outPort = allPorts.stream().filter(p -> p.getName().equalsIgnoreCase(outPortName)).findFirst().orElse(null);
            if (outPort != null && ctx.isInterfaceOperationallyUp(outPort)) {
                stateManager.recordPacketSent(outPort.getId(), frame.getFrameSize());
                NetworkInterface peer = ctx.findPeerInterface(outPort.getId());
                if (peer != null) {
                    if (processHop(ctx, peer, frame, traversedPath, depth + 1)) anyDelivered = true;
                }
            }
        }
        return anyDelivered;
    }

    /**
     * Layer 1 Repeater Processing: Regenerate and amplify signal across two segments
     */
    private boolean processRepeaterHop(SimContext ctx, Device rep, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        List<NetworkInterface> allPorts = ctx.getDeviceInterfaces(rep.getId());
        NetworkInterface outPort = allPorts.stream().filter(p -> !p.getId().equals(ingressPort.getId())).findFirst().orElse(null);

        if (outPort == null || !ctx.isInterfaceOperationallyUp(outPort)) {
            return false;
        }

        ctx.addEvent(SimulationEvent.builder()
                .type("SIGNAL_REGENERATED")
                .deviceId(rep.getId())
                .device(rep.getName())
                .interfaceName(ingressPort.getName())
                .action("FORWARD")
                .description("Repeater " + rep.getName() + " (Layer 1) regenerated and amplified signal from " + ingressPort.getName() + " out " + outPort.getName())
                .explanation("A Layer 1 repeater overcomes attenuation across long cabling runs by restoring the waveform, voltage, and timing without modifying any frame headers.")
                .frameSnapshot(frame.copy())
                .build());

        stateManager.recordPacketSent(outPort.getId(), frame.getFrameSize());
        NetworkInterface peer = ctx.findPeerInterface(outPort.getId());
        return peer != null && processHop(ctx, peer, frame, traversedPath, depth + 1);
    }

    /**
     * Layer 2 Bridge Processing: Segment MAC learning and filtering
     */
    private boolean processBridgeHop(SimContext ctx, Device bridge, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        List<NetworkInterface> allPorts = ctx.getDeviceInterfaces(bridge.getId());
        List<String> portNames = allPorts.stream().map(NetworkInterface::getName).toList();
        List<String> targetPorts = bridgeEngine.processFrame(bridge.getId(), frame, ingressPort.getName(), portNames);

        if (targetPorts.isEmpty()) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("BRIDGE_FILTERED")
                    .deviceId(bridge.getId())
                    .device(bridge.getName())
                    .interfaceName(ingressPort.getName())
                    .action("DROP")
                    .dropReason("SAME_SEGMENT_FILTER")
                    .description("Bridge " + bridge.getName() + " (Layer 2) filtered frame: Destination MAC " + frame.getDestinationMac() + " resides on the same segment.")
                    .explanation("The bridge learned that both source and destination MACs reside on the incoming segment. It drops the frame to isolate traffic and prevent congestion on other segments.")
                    .frameSnapshot(frame.copy())
                    .build());
            return false;
        }

        ctx.addEvent(SimulationEvent.builder()
                .type("BRIDGE_FORWARD")
                .deviceId(bridge.getId())
                .device(bridge.getName())
                .interfaceName(ingressPort.getName())
                .action("FORWARD")
                .description("Bridge " + bridge.getName() + " forwarded frame across segments to " + targetPorts)
                .explanation("The bridge learned the source MAC on segment " + ingressPort.getName() + " and forwarded the frame to segment " + targetPorts + " based on its forwarding table.")
                .frameSnapshot(frame.copy())
                .build());

        boolean anyDelivered = false;
        for (String portName : targetPorts) {
            NetworkInterface outPort = allPorts.stream().filter(p -> p.getName().equalsIgnoreCase(portName)).findFirst().orElse(null);
            if (outPort != null && ctx.isInterfaceOperationallyUp(outPort)) {
                stateManager.recordPacketSent(outPort.getId(), frame.getFrameSize());
                NetworkInterface peer = ctx.findPeerInterface(outPort.getId());
                if (peer != null) {
                    if (processHop(ctx, peer, frame, traversedPath, depth + 1)) anyDelivered = true;
                }
            }
        }
        return anyDelivered;
    }

    /**
     * Wireless Access Point (WAP) Processing: 802.11 to 802.3 bridge & CSMA/CA
     */
    private boolean processAccessPointHop(SimContext ctx, Device ap, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        MacControlEngine.CsmaCaResult csmaCa = macControlEngine.simulateCsmaCa(false, true);

        ctx.addEvent(SimulationEvent.builder()
                .type("WIRELESS_BRIDGE")
                .deviceId(ap.getId())
                .device(ap.getName())
                .interfaceName(ingressPort.getName())
                .action("FORWARD")
                .description("Wireless AP " + ap.getName() + " (SSID: " + (ap.getSsid() != null ? ap.getSsid() : "NetHub-WiFi") + ", Ch: " + ap.getWirelessChannel() + ") bridged 802.11 RF signal to 802.3 Ethernet")
                .explanation(csmaCa.getExplanation())
                .frameSnapshot(frame.copy())
                .details(Map.of("ssid", ap.getSsid() != null ? ap.getSsid() : "NetHub-WiFi", "channel", ap.getWirelessChannel(), "security", ap.getWirelessSecurity()))
                .build());

        List<NetworkInterface> allPorts = ctx.getDeviceInterfaces(ap.getId());
        boolean any = false;
        for (NetworkInterface p : allPorts) {
            if (!p.getId().equals(ingressPort.getId()) && ctx.isInterfaceOperationallyUp(p)) {
                stateManager.recordPacketSent(p.getId(), frame.getFrameSize());
                NetworkInterface peer = ctx.findPeerInterface(p.getId());
                if (peer != null && processHop(ctx, peer, frame, traversedPath, depth + 1)) any = true;
            }
        }
        return any;
    }

    /**
     * Modem Processing: Modulation / Demodulation across LAN/WAN boundary
     */
    private boolean processModemHop(SimContext ctx, Device modem, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        ctx.addEvent(SimulationEvent.builder()
                .type("MODEM_MODULATION")
                .deviceId(modem.getId())
                .device(modem.getName())
                .interfaceName(ingressPort.getName())
                .action("FORWARD")
                .description("Modem " + modem.getName() + " performed signal modulation/demodulation across LAN/WAN boundary")
                .explanation("A modem modulates outgoing digital computer bits into carrier frequency signals for WAN transmission and demodulates incoming carrier signals back into digital frames.")
                .frameSnapshot(frame.copy())
                .build());

        List<NetworkInterface> allPorts = ctx.getDeviceInterfaces(modem.getId());
        for (NetworkInterface p : allPorts) {
            if (!p.getId().equals(ingressPort.getId()) && ctx.isInterfaceOperationallyUp(p)) {
                stateManager.recordPacketSent(p.getId(), frame.getFrameSize());
                NetworkInterface peer = ctx.findPeerInterface(p.getId());
                if (peer != null) return processHop(ctx, peer, frame, traversedPath, depth + 1);
            }
        }
        return false;
    }

    /**
     * Gateway Processing: Network & Protocol Translation
     */
    private boolean processGatewayHop(SimContext ctx, Device gw, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        ctx.addEvent(SimulationEvent.builder()
                .type("GATEWAY_TRANSLATION")
                .deviceId(gw.getId())
                .device(gw.getName())
                .interfaceName(ingressPort.getName())
                .action("FORWARD")
                .description("Network Gateway " + gw.getName() + " bridged and translated traffic between heterogeneous networks")
                .explanation("A gateway operates across multiple protocol layers to connect dissimilar networks, translating packet headers and formats between networks.")
                .frameSnapshot(frame.copy())
                .build());

        return processRouterHop(ctx, gw, ingressPort, frame, traversedPath, depth);
    }

    /**
     * Layer 2 Switch Processing: VLAN check, MAC learning, Forwarding, Flooding
     */
    private boolean processSwitchHop(SimContext ctx, Device sw, NetworkInterface ingressPort, EthernetFrame frame, List<String> traversedPath, int depth) {
        int vlanId = ingressPort.getVlanId() != null ? ingressPort.getVlanId() : 1;

        // Step 1: MAC Learning
        MacAddressTable macTable = switchingEngine.getMacTable(sw.getId());
        macTable.learn(frame.getSourceMac(), ingressPort.getName(), vlanId);

        ctx.addEvent(SimulationEvent.builder()
                .type("MAC_LEARNED")
                .deviceId(sw.getId())
                .device(sw.getName())
                .interfaceName(ingressPort.getName())
                .action("LEARN")
                .description("Switch " + sw.getName() + " learned MAC " + frame.getSourceMac() + " on " + ingressPort.getName() + " (VLAN " + vlanId + ")")
                .explanation("The switch inspects the source MAC of every incoming frame and records or updates its MAC forwarding table with the ingress port.")
                .frameSnapshot(frame.copy())
                .details(Map.of("mac", frame.getSourceMac(), "port", ingressPort.getName(), "vlan", vlanId))
                .build());

        List<NetworkInterface> allSwitchPorts = ctx.getDeviceInterfaces(sw.getId());
        List<String> availablePortNames = allSwitchPorts.stream()
                .filter(p -> p.getVlanId() == null || p.getVlanId().equals(vlanId) || "TRUNK".equalsIgnoreCase(p.getPortMode()))
                .map(NetworkInterface::getName)
                .toList();

        // Step 2: Unicast Forwarding or Flooding Decision
        List<String> egressPortNames = switchingEngine.processFrame(sw.getId(), frame, ingressPort.getName(), availablePortNames, vlanId);

        if (egressPortNames.isEmpty()) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("FRAME_FILTERED")
                    .deviceId(sw.getId())
                    .device(sw.getName())
                    .interfaceName(ingressPort.getName())
                    .action("DROP")
                    .dropReason("SAME_SEGMENT_FILTER")
                    .description("Frame filtered on " + sw.getName() + ": Target is on the same port segment.")
                    .explanation("The destination MAC is already associated with the incoming port, so the frame is not forwarded elsewhere.")
                    .build());
            return false;
        }

        boolean isFlood = egressPortNames.size() > 1 || EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(frame.getDestinationMac());

        ctx.addEvent(SimulationEvent.builder()
                .type(isFlood ? "SWITCH_FLOOD" : "SWITCH_FORWARD")
                .deviceId(sw.getId())
                .device(sw.getName())
                .interfaceName(ingressPort.getName())
                .action(isFlood ? "FLOOD" : "FORWARD")
                .description(isFlood
                        ? "Switch " + sw.getName() + " flooding frame out ports " + egressPortNames + " (excluding " + ingressPort.getName() + ")"
                        : "Switch " + sw.getName() + " forwarded unicast frame out port " + egressPortNames.get(0) + " to destination " + frame.getDestinationMac())
                .explanation(isFlood
                        ? "Destination MAC is either broadcast or unknown in the MAC table for VLAN " + vlanId + ". The switch floods the frame to all active ports in the same VLAN except the ingress port."
                        : "Destination MAC matched an active entry in the switch forwarding table. Frame forwarded directly out the mapped egress port.")
                .frameSnapshot(frame.copy())
                .details(Map.of("egressPorts", egressPortNames, "vlan", vlanId, "dstMac", frame.getDestinationMac()))
                .build());

        // Propagate out egress ports
        boolean anyDelivered = false;
        for (String egressPortName : egressPortNames) {
            NetworkInterface outPort = allSwitchPorts.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(egressPortName))
                    .findFirst()
                    .orElse(null);

            if (outPort != null && ctx.isInterfaceOperationallyUp(outPort)) {
                stateManager.recordPacketSent(outPort.getId(), frame.getFrameSize());
                NetworkInterface peerIntf = ctx.findPeerInterface(outPort.getId());
                if (peerIntf != null) {
                    boolean res = processHop(ctx, peerIntf, frame, traversedPath, depth + 1);
                    if (res) anyDelivered = true;
                }
            }
        }
        return anyDelivered;
    }

    /**
     * Layer 3 Router Processing: MAC check, ACL evaluation, TTL decrement, LPM Route lookup, ARP resolution, Re-encapsulation
     */
    private boolean processRouterHop(SimContext ctx, Device router, NetworkInterface ingressIntf, EthernetFrame frame, List<String> traversedPath, int depth) {
        // Step 1: Verify L2 Destination MAC
        String myMac = ingressIntf.getMacAddress();
        if (!myMac.equalsIgnoreCase(frame.getDestinationMac()) && !EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(frame.getDestinationMac())) {
            return false;
        }

        if (!(frame.getPayload() instanceof IpPacket ipPacket)) {
            return false;
        }

        // Step 2: Access Control List (ACL) Check
        AclEngine.AclEvaluationResult aclRes = aclEngine.evaluatePacket(router.getId(), ipPacket.getSourceIp(), ipPacket.getDestinationIp(), ipPacket.getProtocolName(), 80);
        if (!aclRes.isPermitted()) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ACL_DENIED")
                    .deviceId(router.getId())
                    .device(router.getName())
                    .action("DROP")
                    .dropReason("ACL_DENIED")
                    .description("Packet dropped by Access Control List on " + router.getName())
                    .explanation(aclRes.getExplanation())
                    .frameSnapshot(frame.copy())
                    .build());
            return false;
        }

        ctx.addEvent(SimulationEvent.builder()
                .type("ROUTER_RECEIVE")
                .deviceId(router.getId())
                .device(router.getName())
                .interfaceName(ingressIntf.getName())
                .action("RECEIVE")
                .description("Router " + router.getName() + " received IPv4 packet on " + ingressIntf.getName() + " [" + ipPacket.getSourceIp() + " -> " + ipPacket.getDestinationIp() + "]")
                .explanation("Router decapsulated the Layer 2 Ethernet frame, extracted the IPv4 header, and prepared for Layer 3 route lookup.")
                .frameSnapshot(frame.copy())
                .build());

        // Step 3: TTL Decrement & Check
        ipPacket.decrementTtl();
        ctx.addEvent(SimulationEvent.builder()
                .type("TTL_DECREMENT")
                .deviceId(router.getId())
                .device(router.getName())
                .action("INSPECT")
                .description("TTL decremented to " + ipPacket.getTtl() + " at " + router.getName())
                .explanation("Every Layer 3 hop decrements the IPv4 Time-To-Live (TTL) field to prevent indefinite packet loops in the network.")
                .details(Map.of("newTtl", ipPacket.getTtl()))
                .build());

        if (ipPacket.getTtl() <= 0) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ICMP_TIME_EXCEEDED")
                    .deviceId(router.getId())
                    .device(router.getName())
                    .interfaceName(ingressIntf.getName())
                    .action("DROP")
                    .dropReason("TTL_EXPIRED")
                    .description("Packet dropped: TTL expired in transit. Router generated ICMP Time Exceeded (Type 11, Code 0) back to " + ipPacket.getSourceIp())
                    .explanation("When a router receives a packet with TTL=1 and decrements it to 0, it drops the packet and responds with an ICMP Time Exceeded message, which traceroute uses to identify hops.")
                    .build());
            return false;
        }

        // Step 4: Check if packet is destined for the router itself
        List<NetworkInterface> routerInterfaces = ctx.getDeviceInterfaces(router.getId());
        for (NetworkInterface rIntf : routerInterfaces) {
            if (ipPacket.getDestinationIp().equalsIgnoreCase(rIntf.getIpAddress())) {
                ctx.addEvent(SimulationEvent.builder()
                        .type("DELIVERED")
                        .deviceId(router.getId())
                        .device(router.getName())
                        .interfaceName(rIntf.getName())
                        .action("DELIVER")
                        .description("Packet reached destination router interface: " + router.getName() + " (" + ipPacket.getDestinationIp() + ")")
                        .explanation("The destination IP address matches one of the router's own interfaces.")
                        .build());

                if (ipPacket.getPayload() instanceof IcmpPacket icmp && icmp.getType() == IcmpPacket.TYPE_ECHO_REQUEST) {
                    ctx.addEvent(SimulationEvent.builder()
                            .type("ICMP_ECHO_REPLY")
                            .deviceId(router.getId())
                            .device(router.getName())
                            .action("GENERATE")
                            .description("Router " + router.getName() + " replied with ICMP Echo Reply (Type 0, Code 0)")
                            .build());
                }
                return true;
            }
        }

        // Step 5: Longest-Prefix Match (LPM) Route Lookup
        RoutingTable routingTable = routingTableService.buildRoutingTable(router.getId());
        Optional<RouteEntry> routeOpt = routingEngine.lookupBestRoute(routingTable, ipPacket.getDestinationIp());

        if (routeOpt.isEmpty()) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ICMP_DEST_UNREACHABLE")
                    .deviceId(router.getId())
                    .device(router.getName())
                    .action("DROP")
                    .dropReason("NO_ROUTE")
                    .description("Packet dropped: No route to host/network " + ipPacket.getDestinationIp() + " in " + router.getName() + " routing table. Generated ICMP Destination Network Unreachable (Type 3, Code 0)")
                    .explanation("The router compared the destination IP against all connected, static, and default routes but found no matching prefix.")
                    .build());
            return false;
        }

        RouteEntry bestRoute = routeOpt.get();
        ctx.addEvent(SimulationEvent.builder()
                .type("ROUTE_LOOKUP")
                .deviceId(router.getId())
                .device(router.getName())
                .action("LOOKUP")
                .description("Route Match: " + bestRoute.getDestinationNetwork() + "/" + bestRoute.getPrefixLength() + " via " + bestRoute.getOutgoingInterface() + " (Type: " + bestRoute.getRouteType() + ")")
                .explanation("Longest-Prefix Matching selected the most specific routing table entry for destination " + ipPacket.getDestinationIp() + ".")
                .details(Map.of("destNet", bestRoute.getDestinationNetwork(), "prefix", bestRoute.getPrefixLength(), "outIntf", bestRoute.getOutgoingInterface()))
                .build());

        // Step 6: Identify Outgoing Interface
        NetworkInterface outIntf = routerInterfaces.stream()
                .filter(i -> i.getName().equalsIgnoreCase(bestRoute.getOutgoingInterface()))
                .findFirst()
                .orElse(null);

        if (outIntf == null || !ctx.isInterfaceOperationallyUp(outIntf)) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(router.getId())
                    .device(router.getName())
                    .action("DROP")
                    .dropReason("INTERFACE_DOWN")
                    .description("Packet dropped: Outgoing interface " + bestRoute.getOutgoingInterface() + " is operationally DOWN")
                    .explanation("Cannot forward traffic out a disabled or disconnected interface.")
                    .build());
            return false;
        }

        // Step 7: Determine Next-Hop IP and Resolve Next-Hop MAC
        String nextHopIp = (bestRoute.getNextHop() != null && !bestRoute.getNextHop().trim().isEmpty())
                ? bestRoute.getNextHop().trim()
                : ipPacket.getDestinationIp();

        Optional<String> nextHopMacOpt = arpEngine.lookupMac(router.getId(), nextHopIp);
        String nextHopMac;

        if (nextHopMacOpt.isPresent()) {
            nextHopMac = nextHopMacOpt.get();
        } else {
            nextHopMac = performArpResolution(ctx, router, outIntf, nextHopIp);
            if (nextHopMac == null) {
                ctx.addEvent(SimulationEvent.builder()
                        .type("ERROR")
                        .deviceId(router.getId())
                        .device(router.getName())
                        .interfaceName(outIntf.getName())
                        .action("DROP")
                        .dropReason("NEXT_HOP_ARP_FAILED")
                        .description("Packet dropped: ARP resolution failed for next hop " + nextHopIp)
                        .explanation("The router could not find the Layer 2 hardware address of the next hop device to encapsulate the frame.")
                        .build());
                return false;
            }
        }

        // Step 8: Ethernet Re-encapsulation
        frame.setSourceMac(outIntf.getMacAddress());
        frame.setDestinationMac(nextHopMac);
        frame.setVlanId(outIntf.getVlanId() != null ? outIntf.getVlanId() : 1);

        ctx.addEvent(SimulationEvent.builder()
                .type("FRAME_REENCAPSULATION")
                .deviceId(router.getId())
                .device(router.getName())
                .interfaceName(outIntf.getName())
                .action("REENCAPSULATE")
                .description("Router re-encapsulated frame: [SrcMAC: " + outIntf.getMacAddress() + " -> DstMAC: " + nextHopMac + "], preserving original IP [" + ipPacket.getSourceIp() + " -> " + ipPacket.getDestinationIp() + "]")
                .explanation("At Layer 3 forwarding, the router strips the old Layer 2 header and creates a new Ethernet frame header with its own outgoing MAC and the next hop's MAC address.")
                .frameSnapshot(frame.copy())
                .details(Map.of("newSrcMac", outIntf.getMacAddress(), "newDstMac", nextHopMac, "nextHopIp", nextHopIp))
                .build());

        stateManager.recordPacketSent(outIntf.getId(), frame.getFrameSize());

        NetworkInterface peerIntf = ctx.findPeerInterface(outIntf.getId());
        if (peerIntf == null) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ERROR")
                    .deviceId(router.getId())
                    .device(router.getName())
                    .action("DROP")
                    .dropReason("DISCONNECTED_CABLE")
                    .description("Frame dropped: Cable disconnected from router interface " + outIntf.getName())
                    .build());
            return false;
        }

        return processHop(ctx, peerIntf, frame, traversedPath, depth + 1);
    }

    /**
     * Host (PC / Server) Processing: Final delivery and Layer 4 protocol handling
     */
    private boolean processHostHop(SimContext ctx, Device host, NetworkInterface ingressIntf, EthernetFrame frame) {
        if (!(frame.getPayload() instanceof IpPacket ipPacket)) {
            return false;
        }

        // Check L2 MAC matching
        String myMac = ingressIntf.getMacAddress();
        if (!myMac.equalsIgnoreCase(frame.getDestinationMac()) && !EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(frame.getDestinationMac())) {
            return false;
        }

        // Check L3 IP matching
        if (!ipPacket.getDestinationIp().equalsIgnoreCase(ingressIntf.getIpAddress()) && !"255.255.255.255".equals(ipPacket.getDestinationIp())) {
            return false;
        }

        ctx.addEvent(SimulationEvent.builder()
                .type("DELIVERED")
                .deviceId(host.getId())
                .device(host.getName())
                .interfaceName(ingressIntf.getName())
                .action("DELIVER")
                .description("Packet successfully delivered to destination host: " + host.getName() + " (" + ipPacket.getDestinationIp() + ")")
                .explanation("Layer 2 MAC and Layer 3 IP verified. Frame decapsulated and handed to upper-layer protocol stack.")
                .frameSnapshot(frame.copy())
                .details(Map.of("device", host.getName(), "ip", ipPacket.getDestinationIp(), "mac", ingressIntf.getMacAddress(), "protocol", ipPacket.getProtocolName()))
                .build());

        // Process Upper Protocols
        if (ipPacket.getPayload() instanceof IcmpPacket icmp && icmp.getType() == IcmpPacket.TYPE_ECHO_REQUEST) {
            ctx.addEvent(SimulationEvent.builder()
                    .type("ICMP_ECHO_REPLY")
                    .deviceId(host.getId())
                    .device(host.getName())
                    .action("GENERATE")
                    .description("Destination host " + host.getName() + " responded with ICMP Echo Reply (Type 0, Code 0) 32 bytes")
                    .explanation("The host responded to the ICMP Echo Request, confirming round-trip reachability.")
                    .build());
        } else if (ipPacket.getPayload() instanceof TcpSegment tcp) {
            if (tcp.isSyn()) {
                ctx.addEvent(SimulationEvent.builder()
                        .type("TCP_SYN_ACK")
                        .deviceId(host.getId())
                        .device(host.getName())
                        .action("GENERATE")
                        .description("TCP 3-Way Handshake: " + host.getName() + " port " + tcp.getDestinationPort() + " responded with [SYN, ACK] seq=" + (tcp.getSequenceNumber() + 1))
                        .explanation("The listening server accepted the incoming TCP connection attempt and responded with SYN-ACK.")
                        .build());
            } else if (tcp.getPayload() instanceof HttpPayload http && http.isRequest()) {
                ctx.addEvent(SimulationEvent.builder()
                        .type("HTTP_RESPONSE")
                        .deviceId(host.getId())
                        .device(host.getName())
                        .action("GENERATE")
                        .description("HTTP Web Server on " + host.getName() + " served request: HTTP/1.1 200 OK (Content-Type: text/html)")
                        .explanation("The web server received the HTTP GET request, processed the URI, and responded with status 200 OK.")
                        .details(Map.of("status", 200, "statusText", "OK"))
                        .build());
            }
        }

        return true;
    }

    private int determineIpProtocol(String protocolType) {
        if ("ICMP".equalsIgnoreCase(protocolType) || "PING".equalsIgnoreCase(protocolType) || "TRACEROUTE".equalsIgnoreCase(protocolType)) {
            return IpPacket.PROTOCOL_ICMP;
        } else if ("TCP".equalsIgnoreCase(protocolType) || "HTTP".equalsIgnoreCase(protocolType)) {
            return IpPacket.PROTOCOL_TCP;
        } else if ("UDP".equalsIgnoreCase(protocolType) || "DNS".equalsIgnoreCase(protocolType) || "DHCP".equalsIgnoreCase(protocolType)) {
            return IpPacket.PROTOCOL_UDP;
        }
        return IpPacket.PROTOCOL_RAW;
    }

    private Object createUpperPayload(String protocolType, String sourceIp, String destinationIp) {
        if ("ICMP".equalsIgnoreCase(protocolType) || "PING".equalsIgnoreCase(protocolType) || "TRACEROUTE".equalsIgnoreCase(protocolType)) {
            return IcmpPacket.builder()
                    .type(IcmpPacket.TYPE_ECHO_REQUEST)
                    .code(0)
                    .identifier(1234)
                    .sequenceNumber(1)
                    .data("NetHub Ping Data Bytes=32")
                    .build();
        } else if ("HTTP".equalsIgnoreCase(protocolType)) {
            return TcpSegment.builder()
                    .sourcePort(52431)
                    .destinationPort(80)
                    .sequenceNumber(1001L)
                    .acknowledgmentNumber(1L)
                    .psh(true)
                    .ack(true)
                    .payload(HttpPayload.builder()
                            .request(true)
                            .method("GET")
                            .uri("/index.html")
                            .build())
                    .build();
        } else if ("TCP".equalsIgnoreCase(protocolType)) {
            return TcpSegment.builder()
                    .sourcePort(52430)
                    .destinationPort(80)
                    .sequenceNumber(1000L)
                    .syn(true)
                    .build();
        } else if ("DNS".equalsIgnoreCase(protocolType)) {
            return UdpDatagram.builder()
                    .sourcePort(53123)
                    .destinationPort(53)
                    .payload(DnsMessage.builder()
                            .queryDomain("server1.local")
                            .queryType("A")
                            .build())
                    .build();
        } else if ("DHCP".equalsIgnoreCase(protocolType)) {
            return UdpDatagram.builder()
                    .sourcePort(68)
                    .destinationPort(67)
                    .payload(DhcpMessage.builder()
                            .messageType(DhcpMessage.TYPE_DISCOVER)
                            .build())
                    .build();
        }
        return "Raw Network Payload";
    }
}
