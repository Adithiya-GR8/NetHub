package com.nethub.engine.simulation;

import com.nethub.engine.ArpEngine;
import com.nethub.engine.RoutingEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.DeviceStatus;
import com.nethub.entity.enums.DeviceType;
import com.nethub.model.*;
import com.nethub.service.DeviceStateManager;
import com.nethub.service.RoutingTableService;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DiscreteNetworkSimulator {

    private final ArpEngine arpEngine;
    private final SwitchingEngine switchingEngine;
    private final RoutingEngine routingEngine;
    private final RoutingTableService routingTableService;
    private final DeviceStateManager stateManager;

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
            // Loop protection
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

        // Route by Device Type
        switch (currentDevice.getType()) {
            case SWITCH:
                return processSwitchHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case ROUTER:
                return processRouterHop(ctx, currentDevice, ingressIntf, frame, traversedPath, depth);

            case PC:
            case SERVER:
                return processHostHop(ctx, currentDevice, ingressIntf, frame);

            default:
                return false;
        }
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
     * Layer 3 Router Processing: MAC check, TTL decrement, LPM Route lookup, ARP resolution, Re-encapsulation
     */
    private boolean processRouterHop(SimContext ctx, Device router, NetworkInterface ingressIntf, EthernetFrame frame, List<String> traversedPath, int depth) {
        // Step 1: Verify L2 Destination MAC
        String myMac = ingressIntf.getMacAddress();
        if (!myMac.equalsIgnoreCase(frame.getDestinationMac()) && !EthernetFrame.BROADCAST_MAC.equalsIgnoreCase(frame.getDestinationMac())) {
            // Not destined for this router interface
            return false;
        }

        if (!(frame.getPayload() instanceof IpPacket ipPacket)) {
            // Non-IP packet dropped by router
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

        // Step 2: TTL Decrement & Check
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

        // Step 3: Check if packet is destined for the router itself
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

        // Step 4: Longest-Prefix Match (LPM) Route Lookup
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

        // Step 5: Identify Outgoing Interface
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

        // Step 6: Determine Next-Hop IP and Resolve Next-Hop MAC
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

        // Step 7: Ethernet Re-encapsulation
        // Crucial realism: IP Source and Destination remain intact; Ethernet Source and Destination are replaced!
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
            // Not destined for this host MAC
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

