package com.nethub.service;

import com.nethub.dto.CommandResponse;
import com.nethub.engine.ArpEngine;
import com.nethub.engine.SwitchingEngine;
import com.nethub.engine.protocols.ApplicationProtocolEngine;
import com.nethub.engine.routing.OspfEngine;
import com.nethub.engine.routing.RipEngine;
import com.nethub.engine.routing.BgpEngine;
import com.nethub.engine.security.AclEngine;
import com.nethub.engine.security.NatEngine;
import com.nethub.entity.Device;
import com.nethub.entity.NetworkInterface;
import com.nethub.entity.enums.DeviceType;
import com.nethub.entity.enums.InterfaceStatus;
import com.nethub.model.ArpEntry;
import com.nethub.model.MacTableEntry;
import com.nethub.model.RouteEntry;
import com.nethub.model.RoutingTable;
import com.nethub.model.SimulationEvent;
import com.nethub.model.SimulationResult;
import com.nethub.model.state.DeviceInspectionDto;
import com.nethub.model.state.InterfaceOperationalDto;
import com.nethub.repository.DeviceRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeviceTerminalService {

    private final DeviceRepository deviceRepository;
    private final NetworkInterfaceRepository interfaceRepository;
    private final DeviceStateManager stateManager;
    private final NetworkSimulationService simulationService;
    private final RoutingTableService routingTableService;
    private final ArpEngine arpEngine;
    private final SwitchingEngine switchingEngine;
    private final ApplicationProtocolEngine protocolEngine;
    private final OspfEngine ospfEngine;
    private final RipEngine ripEngine;
    private final BgpEngine bgpEngine;
    private final AclEngine aclEngine;
    private final NatEngine natEngine;

    public CommandResponse executeCommand(Long deviceId, String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return CommandResponse.builder()
                    .command("")
                    .output("")
                    .exitCode(0)
                    .build();
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device not found with id: " + deviceId));

        String trimmed = rawCommand.trim();
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase();

        String output;
        int exitCode = 0;

        try {
            switch (device.getType()) {
                case PC:
                case SERVER:
                    output = handleHostCommand(device, cmd, parts, trimmed);
                    break;
                case ROUTER:
                case GATEWAY:
                    output = handleRouterCommand(device, cmd, parts, trimmed);
                    break;
                case SWITCH:
                case BRIDGE:
                    output = handleSwitchCommand(device, cmd, parts, trimmed);
                    break;
                case ACCESS_POINT:
                    output = handleApCommand(device, cmd, parts, trimmed);
                    break;
                case HUB:
                case REPEATER:
                case MODEM:
                    output = handlePhysicalLayerCommand(device, cmd, parts, trimmed);
                    break;
                default:
                    output = "Unsupported device type: " + device.getType();
                    exitCode = 1;
            }
        } catch (Exception e) {
            output = "Error executing command: " + e.getMessage();
            exitCode = 1;
        }

        return CommandResponse.builder()
                .command(rawCommand)
                .output(output)
                .exitCode(exitCode)
                .deviceName(device.getName())
                .deviceType(device.getType().name())
                .build();
    }

    private String handleHostCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        switch (cmd) {
            case "ipconfig":
                return executeIpconfig(dev, parts);
            case "ifconfig":
                return executeIfconfig(dev);
            case "ping":
                return executePing(dev, parts);
            case "pathping":
                return executePathping(dev, parts);
            case "arp":
                return executeArp(dev, parts);
            case "route":
                return executeRoutePrint(dev, parts);
            case "tracert":
            case "traceroute":
                return executeTracert(dev, parts);
            case "netstat":
                return executeNetstat(dev, parts);
            case "nbtstat":
                return executeNbtstat(dev, parts);
            case "nslookup":
                return executeNslookup(dev, parts);
            case "nmap":
                return executeNmap(dev, parts);
            case "help":
                return "Supported Host Commands:\n  ipconfig [/all]\n  ifconfig\n  ping <ip_address>\n  pathping <ip_address>\n  arp -a\n  route print\n  tracert <ip_address>\n  netstat [-a] [-n]\n  nbtstat [-a IP]\n  nslookup <domain_name>\n  nmap <ip_address>\n  clear / cls";
            case "clear":
            case "cls":
                return "\u001b[2J\u001b[H";
            default:
                return "'" + cmd + "' is not recognized as an internal or external command, operable program or batch file.";
        }
    }

    private String handleRouterCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        String lowerFull = fullCmd.toLowerCase().replaceAll("\\s+", " ");
        if (lowerFull.startsWith("show ip interface brief") || lowerFull.equals("sh ip int br")) {
            return executeRouterInterfaceBrief(dev);
        } else if (lowerFull.startsWith("show ip route") || lowerFull.equals("sh ip ro")) {
            return executeRouterShowIpRoute(dev);
        } else if (lowerFull.startsWith("show arp") || lowerFull.equals("sh arp")) {
            return executeRouterShowArp(dev);
        } else if (lowerFull.startsWith("show ip ospf neighbor") || lowerFull.equals("sh ip ospf nei")) {
            return executeRouterShowOspfNeighbor(dev);
        } else if (lowerFull.startsWith("show ip ospf database") || lowerFull.equals("sh ip ospf db") || lowerFull.startsWith("show ip ospf")) {
            return executeRouterShowOspfDatabase(dev);
        } else if (lowerFull.startsWith("show ip rip") || lowerFull.equals("sh ip rip")) {
            return executeRouterShowRip(dev);
        } else if (lowerFull.startsWith("show ip bgp") || lowerFull.equals("sh ip bgp")) {
            return executeRouterShowBgp(dev);
        } else if (lowerFull.startsWith("show ip nat translations") || lowerFull.startsWith("show ip nat") || lowerFull.equals("sh ip nat trans")) {
            return executeRouterShowNat(dev);
        } else if (lowerFull.startsWith("show access-lists") || lowerFull.startsWith("show ip access-lists") || lowerFull.equals("sh acl")) {
            return executeRouterShowAcl(dev);
        } else if (lowerFull.startsWith("show interfaces") || lowerFull.equals("sh int")) {
            return executeRouterShowInterfaces(dev);
        } else if (cmd.equals("ping")) {
            return executePing(dev, parts);
        } else if (cmd.equals("tracert") || cmd.equals("traceroute")) {
            return executeTracert(dev, parts);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Router Commands:\n  show ip interface brief (sh ip int br)\n  show ip route (sh ip ro)\n  show arp\n  show ip ospf neighbor\n  show ip ospf database\n  show ip rip\n  show ip bgp\n  show ip nat translations\n  show access-lists\n  show interfaces\n  ping <ip_address>\n  traceroute <ip_address>";
        } else {
            return "% Invalid input detected at '^' marker.\nType 'help' or '?' for available commands.";
        }
    }

    private String handleSwitchCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        String lowerFull = fullCmd.toLowerCase().replaceAll("\\s+", " ");
        if (lowerFull.startsWith("show mac address-table") || lowerFull.startsWith("show mac-address-table") || lowerFull.equals("sh mac")) {
            return executeSwitchShowMacTable(dev);
        } else if (lowerFull.startsWith("show interfaces status") || lowerFull.startsWith("show interfaces") || lowerFull.equals("sh int")) {
            return executeSwitchShowInterfaces(dev);
        } else if (lowerFull.startsWith("show vlan") || lowerFull.equals("sh vlan")) {
            return executeSwitchShowVlan(dev);
        } else if (lowerFull.startsWith("show spanning-tree") || lowerFull.equals("sh stp")) {
            return executeSwitchShowSpanningTree(dev);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Switch Commands:\n  show mac address-table\n  show interfaces [status]\n  show vlan\n  show spanning-tree";
        } else {
            return "% Invalid input detected at '^' marker.\nType 'help' for available commands.";
        }
    }

    private String handleApCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        String lowerFull = fullCmd.toLowerCase().replaceAll("\\s+", " ");
        if (lowerFull.contains("wireless") || lowerFull.contains("ssid") || lowerFull.contains("clients")) {
            return executeApShowWireless(dev);
        } else if (lowerFull.contains("interfaces") || lowerFull.contains("int")) {
            return executeRouterShowInterfaces(dev);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Access Point Commands:\n  show wireless\n  show clients\n  show ssid\n  show interfaces";
        } else {
            return "% Invalid command. Type 'help' for available AP commands.";
        }
    }

    private String handlePhysicalLayerCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        String lowerFull = fullCmd.toLowerCase().replaceAll("\\s+", " ");
        if (lowerFull.contains("stat") || lowerFull.contains("port") || lowerFull.contains("status")) {
            return executePhysicalShowStats(dev);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Commands for " + dev.getType() + ":\n  show stats\n  show ports\n  show status";
        } else {
            return "% Invalid command. Type 'help' for available commands.";
        }
    }

    // --- PC/Server Command Handlers ---

    private String executeIpconfig(Device dev, String[] parts) {
        boolean all = parts.length > 1 && parts[1].equalsIgnoreCase("/all");
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Windows IP Configuration\n\n");
        if (all) {
            sb.append("   Host Name . . . . . . . . . . . . : ").append(dev.getHostname()).append("\n");
            sb.append("   Primary Dns Suffix  . . . . . . . : local\n");
            sb.append("   Node Type . . . . . . . . . . . . : Hybrid\n");
            sb.append("   IP Routing Enabled. . . . . . . . : No\n\n");
        }

        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            sb.append("Ethernet adapter ").append(intf.getName()).append(":\n\n");
            sb.append("   Connection-specific DNS Suffix  . : local\n");
            if (all) {
                sb.append("   Description . . . . . . . . . . . : Intel(R) Gigabit Network Connection\n");
                sb.append("   Physical Address. . . . . . . . . : ").append(intf.getMacAddress()).append("\n");
                sb.append("   DHCP Enabled. . . . . . . . . . . : No\n");
            }
            sb.append("   Link-local IPv6 Address . . . . . : fe80::").append(intf.getMacAddress().replace(":", "").substring(0, 8)).append("%4\n");
            sb.append("   IPv4 Address. . . . . . . . . . . : ").append(intf.getIpAddress() != null ? intf.getIpAddress() : "0.0.0.0").append("\n");
            sb.append("   Subnet Mask . . . . . . . . . . . : ").append(intf.getSubnetMask() != null ? intf.getSubnetMask() : "0.0.0.0").append("\n");
            sb.append("   Default Gateway . . . . . . . . . : ").append(intf.getGateway() != null ? intf.getGateway() : "").append("\n");
            if (all && intf.getDnsServer() != null) {
                sb.append("   DNS Servers . . . . . . . . . . . : ").append(intf.getDnsServer()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String executeIfconfig(Device dev) {
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());
        StringBuilder sb = new StringBuilder();
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            sb.append(intf.getName()).append(": flags=4163<").append(intf.getOperationalStatus()).append(",BROADCAST,RUNNING,MULTICAST>  mtu ").append(intf.getMtu()).append("\n");
            if (intf.getIpAddress() != null) {
                sb.append("        inet ").append(intf.getIpAddress()).append("  netmask ").append(intf.getSubnetMask());
                if (intf.getSubnetMask() != null) {
                    sb.append("  broadcast ").append(IpUtils.getBroadcastIp(intf.getIpAddress(), intf.getSubnetMask()));
                }
                sb.append("\n");
            }
            sb.append("        ether ").append(intf.getMacAddress()).append("  txqueuelen 1000  (Ethernet)\n");
            sb.append("        RX packets ").append(intf.getStats().getPacketsReceived()).append("  bytes ").append(intf.getStats().getBytesReceived()).append(" (").append(intf.getStats().getBytesReceived() / 1024).append(" KB)\n");
            sb.append("        TX packets ").append(intf.getStats().getPacketsSent()).append("  bytes ").append(intf.getStats().getBytesSent()).append(" (").append(intf.getStats().getBytesSent() / 1024).append(" KB)\n\n");
        }
        return sb.toString();
    }

    private String executePing(Device dev, String[] parts) {
        if (parts.length < 2) {
            return "Usage: ping <destination_ip>";
        }
        String targetIp = parts[1].trim();
        if (!IpUtils.isValidIPv4(targetIp)) {
            return "Ping request could not find host " + targetIp + ". Please check the name and try again.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nPinging ").append(targetIp).append(" with 32 bytes of data:\n");

        int sent = 4;
        int received = 0;

        for (int i = 0; i < sent; i++) {
            SimulationResult result = simulationService.simulateTransmission(dev.getProjectId(), dev.getId(), targetIp, "ICMP", 64);
            if (result.isSuccess()) {
                received++;
                sb.append("Reply from ").append(targetIp).append(": bytes=32 time<1ms TTL=64\n");
            } else {
                if ("NO_DEFAULT_GATEWAY".equals(result.getErrorCode()) || "NO_ROUTE".equals(result.getErrorCode())) {
                    sb.append("Reply from ").append(dev.getName()).append(": Destination host unreachable.\n");
                } else if ("TTL_EXPIRED".equals(result.getErrorCode())) {
                    sb.append("Reply from intermediate hop: TTL expired in transit.\n");
                } else {
                    sb.append("Request timed out.\n");
                }
            }
        }

        int lost = sent - received;
        int pct = (lost * 100) / sent;
        sb.append("\nPing statistics for ").append(targetIp).append(":\n");
        sb.append("    Packets: Sent = ").append(sent).append(", Received = ").append(received).append(", Lost = ").append(lost).append(" (").append(pct).append("% loss)\n");

        return sb.toString();
    }

    private String executeArp(Device dev, String[] parts) {
        List<ArpEntry> entries = arpEngine.getArpTable(dev.getId()).getAllEntries();
        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(dev.getId());
        String myIp = interfaces.isEmpty() || interfaces.get(0).getIpAddress() == null ? "0.0.0.0" : interfaces.get(0).getIpAddress();

        StringBuilder sb = new StringBuilder();
        sb.append("\nInterface: ").append(myIp).append(" --- 0x2\n");
        sb.append(String.format("  %-22s%-22s%-10s\n", "Internet Address", "Physical Address", "Type"));

        if (entries.isEmpty()) {
            sb.append("  No ARP Entries Found\n");
        } else {
            for (ArpEntry entry : entries) {
                sb.append(String.format("  %-22s%-22s%-10s\n",
                        entry.getIpAddress(),
                        entry.getMacAddress(),
                        entry.getState().toLowerCase()));
            }
        }
        return sb.toString();
    }

    private String executeRoutePrint(Device dev, String[] parts) {
        RoutingTable rt = routingTableService.buildRoutingTable(dev.getId());
        List<RouteEntry> routes = rt != null ? rt.getAllRoutes() : Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        sb.append("===========================================================================\n");
        sb.append("Interface List\n");
        List<NetworkInterface> intfs = interfaceRepository.findByDeviceId(dev.getId());
        for (NetworkInterface ni : intfs) {
            sb.append(String.format("  0x%-5d...%s ...... %s\n", ni.getId(), ni.getMacAddress(), ni.getName()));
        }
        sb.append("===========================================================================\n");
        sb.append("IPv4 Route Table\n");
        sb.append("===========================================================================\n");
        sb.append(String.format("%-25s%-18s%-18s%-15s%-8s\n", "Network Destination", "Netmask", "Gateway", "Interface", "Metric"));

        for (RouteEntry r : routes) {
            String gateway = (r.getNextHop() != null) ? r.getNextHop() : "On-link";
            sb.append(String.format("%-25s%-18s%-18s%-15s%-8d\n",
                    r.getDestinationNetwork(),
                    r.getSubnetMask(),
                    gateway,
                    r.getOutgoingInterface(),
                    r.getMetric()));
        }
        sb.append("===========================================================================\n");
        return sb.toString();
    }

    private String executeTracert(Device dev, String[] parts) {
        if (parts.length < 2) {
            return "Usage: tracert <destination_ip>";
        }
        String targetIp = parts[1].trim();
        if (!IpUtils.isValidIPv4(targetIp)) {
            return "Unable to resolve target system name " + targetIp + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nTracing route to ").append(targetIp).append(" over a maximum of 30 hops:\n\n");

        for (int ttl = 1; ttl <= 30; ttl++) {
            SimulationResult result = simulationService.simulateTransmission(dev.getProjectId(), dev.getId(), targetIp, "TRACEROUTE", ttl);
            String hopInfo = null;

            // Check events for TTL expiration hop or final delivery
            for (SimulationEvent ev : result.getEvents()) {
                if ("TTL_DECREMENT".equals(ev.getType()) && ev.getDetails() != null && ((Integer) ev.getDetails().get("newTtl")) == 0) {
                    hopInfo = ev.getDevice();
                } else if ("DELIVERED".equals(ev.getType())) {
                    hopInfo = ev.getDevice() + " (" + targetIp + ")";
                }
            }

            if (hopInfo != null) {
                sb.append(String.format("  %2d    <1 ms    <1 ms    <1 ms  %s\n", ttl, hopInfo));
            } else {
                sb.append(String.format("  %2d     *        *        *     Request timed out.\n", ttl));
            }

            if (result.isSuccess()) {
                break;
            }
        }
        sb.append("\nTrace complete.\n");
        return sb.toString();
    }

    // --- Router Command Handlers ---

    private String executeRouterInterfaceBrief(Device dev) {
        List<NetworkInterface> interfaces = interfaceRepository.findByDeviceId(dev.getId());
        List<com.nethub.entity.Connection> connections = Collections.emptyList();
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s%-18s%-6s%-8s%-15s%-10s\n", "Interface", "IP-Address", "OK?", "Method", "Status", "Protocol"));
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            String ip = (intf.getIpAddress() != null && !intf.getIpAddress().isEmpty()) ? intf.getIpAddress() : "unassigned";
            String admin = (intf.getAdminStatus() == InterfaceStatus.UP) ? "up" : "administratively down";
            String proto = (intf.getOperationalStatus() == InterfaceStatus.UP) ? "up" : "down";
            sb.append(String.format("%-18s%-18s%-6s%-8s%-15s%-10s\n", intf.getName(), ip, "YES", "manual", admin, proto));
        }
        return sb.toString();
    }

    private String executeRouterShowIpRoute(Device dev) {
        RoutingTable rt = routingTableService.buildRoutingTable(dev.getId());
        List<RouteEntry> routes = rt != null ? rt.getAllRoutes() : Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        sb.append("Codes: C - connected, S - static, R - RIP, O - OSPF, * - candidate default\n\n");
        sb.append("Gateway of last resort is not set\n\n");

        for (RouteEntry r : routes) {
            String code = r.getRouteType().name().startsWith("CONN") ? "C" : "S";
            if (r.getNextHop() == null) {
                sb.append(String.format("%-4s %s/%d is directly connected, %s\n", code, r.getDestinationNetwork(), r.getPrefixLength(), r.getOutgoingInterface()));
            } else {
                sb.append(String.format("%-4s %s/%d [%d/%d] via %s, %s\n", code, r.getDestinationNetwork(), r.getPrefixLength(), 1, r.getMetric(), r.getNextHop(), r.getOutgoingInterface()));
            }
        }
        return sb.toString();
    }

    private String executeRouterShowArp(Device dev) {
        List<ArpEntry> entries = arpEngine.getArpTable(dev.getId()).getAllEntries();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s%-18s%-12s%-20s%-8s%-12s\n", "Protocol", "Address", "Age (min)", "Hardware Addr", "Type", "Interface"));
        for (ArpEntry e : entries) {
            sb.append(String.format("%-10s%-18s%-12s%-20s%-8s%-12s\n",
                    "Internet", e.getIpAddress(), "0", e.getMacAddress(), "ARPA", e.getInterfaceName()));
        }
        return sb.toString();
    }

    private String executeRouterShowInterfaces(Device dev) {
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());
        StringBuilder sb = new StringBuilder();
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            sb.append(intf.getName()).append(" is ").append(intf.getAdminStatus() == InterfaceStatus.UP ? "up" : "administratively down");
            sb.append(", line protocol is ").append(intf.getOperationalStatus() == InterfaceStatus.UP ? "up\n" : "down\n");
            sb.append("  Hardware is GigabitEthernet, address is ").append(intf.getMacAddress()).append("\n");
            if (intf.getIpAddress() != null) {
                sb.append("  Internet address is ").append(intf.getIpAddress()).append("/").append(IpUtils.maskToCidr(intf.getSubnetMask())).append("\n");
            }
            sb.append("  MTU ").append(intf.getMtu()).append(" bytes, BW ").append(intf.getSpeed()).append(", DLY 10 usec\n");
            sb.append("     ").append(intf.getStats().getPacketsReceived()).append(" packets input, ").append(intf.getStats().getBytesReceived()).append(" bytes\n");
            sb.append("     ").append(intf.getStats().getPacketsSent()).append(" packets output, ").append(intf.getStats().getBytesSent()).append(" bytes\n\n");
        }
        return sb.toString();
    }

    // --- Switch Command Handlers ---

    private String executeSwitchShowMacTable(Device dev) {
        List<MacTableEntry> entries = switchingEngine.getMacTable(dev.getId()).getAllEntries();
        StringBuilder sb = new StringBuilder();
        sb.append("          Mac Address Table\n");
        sb.append("-------------------------------------------\n");
        sb.append(String.format("%-8s%-20s%-12s%-8s\n", "Vlan", "Mac Address", "Type", "Ports"));
        sb.append("----    -----------         --------    -----\n");
        for (MacTableEntry e : entries) {
            sb.append(String.format("%-8d%-20s%-12s%-8s\n",
                    e.getVlanId(), e.getMacAddress(), e.getStatus(), e.getPort()));
        }
        sb.append("Total Mac Addresses for this criterion: ").append(entries.size()).append("\n");
        return sb.toString();
    }

    private String executeSwitchShowInterfaces(Device dev) {
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s%-10s%-12s%-10s%-10s%-10s\n", "Port", "Status", "Vlan", "Duplex", "Speed", "Type"));
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            String status = (intf.getOperationalStatus() == InterfaceStatus.UP) ? "connected" : "notconnect";
            sb.append(String.format("%-10s%-10s%-12d%-10s%-10s%-10s\n",
                    intf.getName(), status, intf.getVlanId(), intf.getDuplex(), intf.getSpeed(), intf.getPortMode()));
        }
        return sb.toString();
    }

    private String executeSwitchShowVlan(Device dev) {
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());
        Map<Integer, List<String>> vlanPortMap = new HashMap<>();
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            vlanPortMap.computeIfAbsent(intf.getVlanId(), k -> new ArrayList<>()).add(intf.getName());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s%-20s%-10s%-30s\n", "VLAN", "Name", "Status", "Ports"));
        sb.append("----  --------------------  --------- -------------------------------\n");
        for (Map.Entry<Integer, List<String>> entry : vlanPortMap.entrySet()) {
            String name = (entry.getKey() == 1) ? "default" : "VLAN" + String.format("%04d", entry.getKey());
            sb.append(String.format("%-6d%-20s%-10s%-30s\n",
                    entry.getKey(), name, "active", String.join(", ", entry.getValue())));
        }
        return sb.toString();
    }

    private String executeSwitchShowSpanningTree(Device dev) {
        DeviceInspectionDto inspection = stateManager.getDeviceInspection(dev.getId());
        String bridgeMac = inspection.getInterfaces().isEmpty() ? "00:00:00:00:00:00" : inspection.getInterfaces().get(0).getMacAddress();

        StringBuilder sb = new StringBuilder();
        sb.append("Spanning-tree enabled protocol ieee\n");
        sb.append("Root ID    Priority    32768\n");
        sb.append("           Address     ").append(bridgeMac).append("\n");
        sb.append("           This bridge is the root\n");
        sb.append("           Hello Time  2 sec  Max Age 20 sec  Forward Delay 15 sec\n\n");
        sb.append(String.format("%-12s%-8s%-6s%-8s%-10s%-6s\n", "Port", "Role", "Sts", "Cost", "Prio.Nbr", "Type"));
        sb.append("----------- ---- --- ------- ---------- -----\n");
        int portNbr = 1;
        for (InterfaceOperationalDto intf : inspection.getInterfaces()) {
            String role = "Desg";
            String sts = (intf.getOperationalStatus() == InterfaceStatus.UP) ? "FWD" : "BLK";
            sb.append(String.format("%-12s%-8s%-6s%-8d%-10s%-6s\n",
                    intf.getName(), role, sts, 19, "128." + portNbr, "P2p"));
            portNbr++;
        }
        return sb.toString();
    }

    private String executePathping(Device dev, String[] parts) {
        if (parts.length < 2) {
            return "Usage: pathping <destination_ip>";
        }
        String targetIp = parts[1].trim();
        if (!IpUtils.isValidIPv4(targetIp)) {
            return "Unable to resolve target system name " + targetIp + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nTracing route to ").append(targetIp).append(" over a maximum of 30 hops:\n\n");
        List<String> hops = new ArrayList<>();
        hops.add(dev.getName());
        for (int ttl = 1; ttl <= 30; ttl++) {
            SimulationResult result = simulationService.simulateTransmission(dev.getProjectId(), dev.getId(), targetIp, "TRACEROUTE", ttl);
            String hopName = null;
            for (SimulationEvent ev : result.getEvents()) {
                if ("TTL_DECREMENT".equals(ev.getType()) && ev.getDetails() != null && ((Integer) ev.getDetails().get("newTtl")) == 0) {
                    hopName = ev.getDevice();
                } else if ("DELIVERED".equals(ev.getType())) {
                    hopName = ev.getDevice() + " [" + targetIp + "]";
                }
            }
            if (hopName != null) {
                sb.append(String.format("  %2d     %s\n", ttl, hopName));
                hops.add(hopName);
            }
            if (result.isSuccess()) break;
        }
        sb.append("\nComputing statistics for 250 milliseconds...\n");
        sb.append(String.format("            Source to Here   This Node/Link\n"));
        sb.append(String.format("Hop  RTT    Lost/Sent = Pct  Lost/Sent = Pct  Address\n"));
        for (int i = 0; i < hops.size(); i++) {
            sb.append(String.format("  %d   %dms     0/ 100 =  0%%     0/ 100 =  0%%   %s\n", i, (i * 2) + 1, hops.get(i)));
        }
        sb.append("\nTrace complete.\n");
        return sb.toString();
    }

    private String executeNetstat(Device dev, String[] parts) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nActive Connections\n\n");
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "Proto", "Local Address", "Foreign Address", "State"));
        
        List<NetworkInterface> intfs = interfaceRepository.findByDeviceId(dev.getId());
        String ip = (intfs.isEmpty() || intfs.get(0).getIpAddress() == null) ? "127.0.0.1" : intfs.get(0).getIpAddress();

        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "TCP", "0.0.0.0:80", "0.0.0.0:0", "LISTENING"));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "TCP", "0.0.0.0:443", "0.0.0.0:0", "LISTENING"));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "TCP", "0.0.0.0:22", "0.0.0.0:0", "LISTENING"));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "TCP", ip + ":52140", "192.168.1.1:80", "ESTABLISHED"));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "TCP", ip + ":52141", "192.168.2.10:443", "TIME_WAIT"));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "UDP", "0.0.0.0:53", "*:*", ""));
        sb.append(String.format("  %-7s%-22s%-22s%-12s\n", "UDP", "0.0.0.0:67", "*:*", ""));
        return sb.toString();
    }

    private String executeNbtstat(Device dev, String[] parts) {
        List<NetworkInterface> intfs = interfaceRepository.findByDeviceId(dev.getId());
        String mac = intfs.isEmpty() ? "00-00-00-00-00-00" : intfs.get(0).getMacAddress().replace(":", "-");
        StringBuilder sb = new StringBuilder();
        sb.append("\nNetBIOS Remote Machine Name Table\n\n");
        sb.append(String.format("       Name               Type         Status\n"));
        sb.append("    ---------------------------------------------\n");
        sb.append(String.format("    %-18s <00>  UNIQUE      Registered\n", dev.getHostname() != null ? dev.getHostname().toUpperCase() : dev.getName().toUpperCase()));
        sb.append(String.format("    %-18s <00>  GROUP       Registered\n", "WORKGROUP"));
        sb.append(String.format("    %-18s <20>  UNIQUE      Registered\n", dev.getHostname() != null ? dev.getHostname().toUpperCase() : dev.getName().toUpperCase()));
        sb.append("\n    MAC Address = ").append(mac).append("\n");
        sb.append("    Node Type   = Hybrid\n");
        return sb.toString();
    }

    private String executeNslookup(Device dev, String[] parts) {
        if (parts.length < 2) {
            return "Default Server:  dns.nethub.local\nAddress:  192.168.1.53\n\n> ";
        }
        String domain = parts[1].trim();
        var res = protocolEngine.resolveDns(domain);
        StringBuilder sb = new StringBuilder();
        sb.append("Server:  dns.nethub.local\n");
        sb.append("Address:  192.168.1.53\n\n");
        if ("NOERROR".equals(res.getResponseCode()) && res.getResolvedIp() != null) {
            sb.append("Non-authoritative answer:\n");
            sb.append("Name:    ").append(domain).append("\n");
            sb.append("Address: ").append(res.getResolvedIp()).append("\n");
        } else {
            sb.append("*** dns.nethub.local can't find ").append(domain).append(": Non-existent domain\n");
        }
        return sb.toString();
    }

    private String executeNmap(Device dev, String[] parts) {
        if (parts.length < 2) {
            return "Usage: nmap <target_ip>";
        }
        String targetIp = parts[1].trim();
        StringBuilder sb = new StringBuilder();
        sb.append("\nStarting Nmap 7.94 ( https://nmap.org ) at 2026-11-18 12:00 UTC\n");
        sb.append("Nmap scan report for ").append(targetIp).append("\n");
        sb.append("Host is up (0.00041s latency).\n");
        sb.append("Not shown: 994 closed tcp ports (reset)\n");
        sb.append(String.format("%-10s%-10s%-15s\n", "PORT", "STATE", "SERVICE"));
        sb.append(String.format("%-10s%-10s%-15s\n", "21/tcp", "open", "ftp"));
        sb.append(String.format("%-10s%-10s%-15s\n", "22/tcp", "open", "ssh"));
        sb.append(String.format("%-10s%-10s%-15s\n", "53/tcp", "open", "domain"));
        sb.append(String.format("%-10s%-10s%-15s\n", "80/tcp", "open", "http"));
        sb.append(String.format("%-10s%-10s%-15s\n", "443/tcp", "open", "https"));
        sb.append(String.format("%-10s%-10s%-15s\n", "8080/tcp", "open", "http-proxy"));
        sb.append("\nNmap done: 1 IP address (1 host up) scanned in 0.24 seconds\n");
        return sb.toString();
    }

    private String executeRouterShowOspfNeighbor(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s%-5s%-12s%-12s%-16s%-12s\n", "Neighbor ID", "Pri", "State", "Dead Time", "Address", "Interface"));
        sb.append(String.format("%-15s%-5s%-12s%-12s%-16s%-12s\n", "10.0.0.2", "1", "FULL/BDR", "00:00:36", "192.168.1.2", "eth0"));
        sb.append(String.format("%-15s%-5s%-12s%-12s%-16s%-12s\n", "10.0.0.3", "1", "FULL/DR", "00:00:34", "192.168.2.1", "eth1"));
        return sb.toString();
    }

    private String executeRouterShowOspfDatabase(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n            OSPF Router with ID (10.0.0.1) (Process ID 1)\n\n");
        sb.append("                Router Link States (Area 0)\n\n");
        sb.append(String.format("%-16s%-16s%-8s%-12s%-10s%-8s\n", "Link ID", "ADV Router", "Age", "Seq#", "Checksum", "Link count"));
        sb.append(String.format("%-16s%-16s%-8s%-12s%-10s%-8s\n", "10.0.0.1", "10.0.0.1", "412", "0x80000003", "0x00A124", "2"));
        sb.append(String.format("%-16s%-16s%-8s%-12s%-10s%-8s\n", "10.0.0.2", "10.0.0.2", "380", "0x80000002", "0x008432", "2"));
        sb.append(String.format("%-16s%-16s%-8s%-12s%-10s%-8s\n", "10.0.0.3", "10.0.0.3", "356", "0x80000004", "0x0078FE", "3"));
        return sb.toString();
    }

    private String executeRouterShowRip(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Routing Protocol is \"rip\"\n");
        sb.append("  Sending updates every 30 seconds, next due in 12 seconds\n");
        sb.append("  Invalid after 180 seconds, hold down 180, flushed after 240\n");
        sb.append("  Outgoing update filter list for all interfaces is not set\n");
        sb.append("  Incoming update filter list for all interfaces is not set\n");
        sb.append("  Redistributing: connected\n");
        sb.append("  Default version control: send version 2, receive version 2\n");
        sb.append("  Routing for Networks:\n");
        sb.append("    192.168.1.0/24\n");
        sb.append("    192.168.2.0/24\n");
        sb.append("  Routing Information Sources:\n");
        sb.append("    Gateway         Distance      Last Update\n");
        sb.append("    192.168.1.2          120         00:00:18\n");
        sb.append("  Distance: (default is 120)\n");
        return sb.toString();
    }

    private String executeRouterShowBgp(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("BGP table version is 3, local router ID is 10.0.0.1\n");
        sb.append("Status codes: s suppressed, d damped, h history, * valid, > best, i - internal\n\n");
        sb.append(String.format("   %-18s%-16s%-10s%-10s%-10s%-15s\n", "Network", "Next Hop", "Metric", "LocPrf", "Weight", "Path"));
        sb.append(String.format("*> %-18s%-16s%-10s%-10s%-10s%-15s\n", "192.168.1.0/24", "0.0.0.0", "0", "100", "32768", "i"));
        sb.append(String.format("*> %-18s%-16s%-10s%-10s%-10s%-15s\n", "10.10.0.0/16", "192.168.1.2", "0", "100", "0", "65001 65002 i"));
        sb.append(String.format("*> %-18s%-16s%-10s%-10s%-10s%-15s\n", "172.16.0.0/12", "192.168.2.1", "0", "100", "0", "65100 i"));
        return sb.toString();
    }

    private String executeRouterShowNat(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s%-24s%-24s%-24s%-24s\n", "Pro", "Inside global", "Inside local", "Outside local", "Outside global"));
        sb.append(String.format("%-6s%-24s%-24s%-24s%-24s\n", "tcp", "203.0.113.1:10001", "192.168.1.10:49152", "8.8.8.8:80", "8.8.8.8:80"));
        sb.append(String.format("%-6s%-24s%-24s%-24s%-24s\n", "udp", "203.0.113.1:10002", "192.168.1.11:53000", "8.8.8.8:53", "8.8.8.8:53"));
        sb.append(String.format("%-6s%-24s%-24s%-24s%-24s\n", "tcp", "203.0.113.1:10003", "192.168.1.12:8080", "1.1.1.1:443", "1.1.1.1:443"));
        return sb.toString();
    }

    private String executeRouterShowAcl(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Standard IP access list 10\n");
        sb.append("    10 permit 192.168.1.0, wildcard bits 0.0.0.255 (28 matches)\n");
        sb.append("    20 deny   any (4 matches)\n\n");
        sb.append("Extended IP access list 101\n");
        sb.append("    10 permit tcp 192.168.1.0 0.0.0.255 any eq www (45 matches)\n");
        sb.append("    20 permit tcp 192.168.1.0 0.0.0.255 any eq 443 (102 matches)\n");
        sb.append("    30 permit icmp any any echo (16 matches)\n");
        sb.append("    40 deny   ip any any (7 matches)\n");
        return sb.toString();
    }

    private String executeApShowWireless(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Access Point Wireless Operational Status:\n");
        sb.append("  SSID:              ").append(dev.getSsid() != null ? dev.getSsid() : "NetHub-WiFi").append("\n");
        sb.append("  Channel:           ").append(dev.getWirelessChannel() > 0 ? dev.getWirelessChannel() : 6).append(" (2.437 GHz)\n");
        sb.append("  Security:          ").append(dev.getWirelessSecurity() != null ? dev.getWirelessSecurity() : "WPA2-PSK (AES)").append("\n");
        sb.append("  Radio State:       UP / TRANSMITTING\n");
        sb.append("  Beacon Interval:   100 ms\n");
        sb.append("  Associated Clients: 2 active\n");
        sb.append(String.format("    %-18s%-16s%-10s%-8s\n", "Client MAC", "IP Address", "RSSI", "Tx Rate"));
        sb.append(String.format("    %-18s%-16s%-10s%-8s\n", "02:00:00:11:22:33", "192.168.1.15", "-52 dBm", "144 Mbps"));
        sb.append(String.format("    %-18s%-16s%-10s%-8s\n", "02:00:00:44:55:66", "192.168.1.18", "-68 dBm", "72 Mbps"));
        return sb.toString();
    }

    private String executePhysicalShowStats(Device dev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Physical Layer Device Status: ").append(dev.getName()).append(" [").append(dev.getType()).append("]\n");
        sb.append("  Operational Mode: Bit Regeneration & Port Flooding\n");
        sb.append("  Bandwidth:        100 Mbps (Ethernet Base-T)\n");
        sb.append("  Collision Domain: Single Shared Collision Domain\n");
        sb.append("  Broadcast Domain: Shared\n");
        sb.append("  Total Signal Repeats: 284\n");
        sb.append("  Signal Attenuation Restored: 100%\n");
        sb.append("  Port Status:\n");
        List<NetworkInterface> intfs = interfaceRepository.findByDeviceId(dev.getId());
        for (NetworkInterface ni : intfs) {
            sb.append("    Port ").append(ni.getName()).append(": Link UP, Half-Duplex, 0 Collisions\n");
        }
        return sb.toString();
    }
}

