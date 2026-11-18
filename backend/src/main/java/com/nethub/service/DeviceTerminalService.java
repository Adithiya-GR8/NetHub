package com.nethub.service;

import com.nethub.dto.CommandResponse;
import com.nethub.engine.ArpEngine;
import com.nethub.engine.SwitchingEngine;
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
                    output = handleRouterCommand(device, cmd, parts, trimmed);
                    break;
                case SWITCH:
                    output = handleSwitchCommand(device, cmd, parts, trimmed);
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
            case "arp":
                return executeArp(dev, parts);
            case "route":
                return executeRoutePrint(dev, parts);
            case "tracert":
            case "traceroute":
                return executeTracert(dev, parts);
            case "help":
                return "Supported Host Commands:\n  ipconfig [/all]\n  ping <ip_address>\n  arp -a\n  route print\n  tracert <ip_address>\n  clear / cls";
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
        } else if (lowerFull.startsWith("show interfaces") || lowerFull.equals("sh int")) {
            return executeRouterShowInterfaces(dev);
        } else if (cmd.equals("ping")) {
            return executePing(dev, parts);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Router Commands:\n  show ip interface brief (sh ip int br)\n  show ip route (sh ip ro)\n  show arp\n  show interfaces\n  ping <ip_address>";
        } else {
            return "% Invalid input detected at '^' marker.\nType 'help' or '?' for available commands.";
        }
    }

    private String handleSwitchCommand(Device dev, String cmd, String[] parts, String fullCmd) {
        String lowerFull = fullCmd.toLowerCase().replaceAll("\\s+", " ");
        if (lowerFull.startsWith("show mac address-table") || lowerFull.startsWith("show mac-address-table") || lowerFull.equals("sh mac")) {
            return executeSwitchShowMacTable(dev);
        } else if (lowerFull.startsWith("show interfaces") || lowerFull.equals("sh int")) {
            return executeSwitchShowInterfaces(dev);
        } else if (lowerFull.startsWith("show vlan") || lowerFull.equals("sh vlan")) {
            return executeSwitchShowVlan(dev);
        } else if (lowerFull.startsWith("show spanning-tree") || lowerFull.equals("sh stp")) {
            return executeSwitchShowSpanningTree(dev);
        } else if (cmd.equals("help") || cmd.equals("?")) {
            return "Supported Switch Commands:\n  show mac address-table\n  show interfaces\n  show vlan\n  show spanning-tree";
        } else {
            return "% Invalid input detected at '^' marker.\nType 'help' for available commands.";
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
}

