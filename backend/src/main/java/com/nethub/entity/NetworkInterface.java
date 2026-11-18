package com.nethub.entity;

import com.nethub.entity.enums.InterfaceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "network_interfaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInterface {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false)
    private Long deviceId;
    
    @Column(nullable = false)
    private String name; // e.g. eth0, port1
    
    @Column(name = "mac_address", nullable = false)
    private String macAddress;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "subnet_mask")
    private String subnetMask;
    
    private String gateway;
    
    @Column(name = "dns_server")
    private String dnsServer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InterfaceStatus status = InterfaceStatus.UP; // Operational status (UP if connected & admin UP)

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false)
    @Builder.Default
    private InterfaceStatus adminStatus = InterfaceStatus.UP; // Administrative status (user enabled/disabled)

    @Column(name = "mtu")
    @Builder.Default
    private Integer mtu = 1500;

    @Column(name = "speed")
    @Builder.Default
    private String speed = "1Gbps";

    @Column(name = "duplex")
    @Builder.Default
    private String duplex = "FULL";

    @Column(name = "vlan_id")
    @Builder.Default
    private Integer vlanId = 1;

    @Column(name = "port_mode")
    @Builder.Default
    private String portMode = "ACCESS"; // ACCESS, TRUNK

    @Column(name = "allowed_vlans")
    @Builder.Default
    private String allowedVlans = "1";

    // Backward-compatible constructor for existing tests and services
    public NetworkInterface(Long id, Long deviceId, String name, String macAddress, String ipAddress, String subnetMask, String gateway, String dnsServer, InterfaceStatus status) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.gateway = gateway;
        this.dnsServer = dnsServer;
        this.status = status != null ? status : InterfaceStatus.UP;
        this.adminStatus = this.status;
        this.mtu = 1500;
        this.speed = "1Gbps";
        this.duplex = "FULL";
        this.vlanId = 1;
        this.portMode = "ACCESS";
        this.allowedVlans = "1";
    }
}
